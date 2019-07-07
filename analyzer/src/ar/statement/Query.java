package ar.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.ColumnDoesNotExist;
import exceptions.WhereClauseNotKnownException;
import fec.UnitData;
import ar.expression.BinOpExp;
import ar.expression.BinOpExp.BinOp;
import ar.expression.UnOpExp.UnOp;
import ar.expression.vals.ConstValExp;
import ar.expression.vals.ProjValExp;
import ar.expression.Expression;
import ar.expression.UnOpExp;
import ar.expression.vars.RowSetVarExp;
import ar.expression.vars.UnknownExp;
import ar.ddl.Column;
import ar.ddl.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.*;
import net.sf.jsqlparser.expression.operators.relational.*;

public class Query {
	public enum Kind {
		SELECT, INSERT, DELETE, UPDATE
	};

	// reference to the sVar which holds the select query (if applicable)
	private Expression sVar;
	UnitData data;
	ArrayList<Table> tables;
	String text;
	Kind kind;
	Table table;
	List<Column> s_columns;

	public List<Column> getS_columns() {
		return s_columns;
	}

	private List<Expression> i_values;
	private Map<Column, Expression> u_updates;
	Expression whereClause;
	Statement statements;

	public Query(String query, UnitData data, ArrayList<Table> tables) {
		this.data = data;
		this.tables = tables;
		this.text = query.substring(1, query.length() - 1);
		//
		// analyze the text and generate appropriate objects
		try {
			statements = CCJSqlParserUtil.parse(text);
		} catch (JSQLParserException e) {
			e.printStackTrace();
		}
		this.kind = extractKind();
		try {
			this.table = extractTable();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		this.s_columns = extractSCols();
		this.setI_values(extractIVals());
		this.setU_updates(extractUfuncs());
		try {
			this.whereClause = extractWC();
		} catch (WhereClauseNotKnownException e) {
			e.printStackTrace();
		}
	}

	public void patch(int index, Expression newExp) {

		// must replace the i'th (oroginal placement of course) occurence of unknownExp
		// with the given exp

		this.whereClause = this.whereClause.getUpdateExp(newExp, index);
		if (this.getsVar() != null)
			this.setsVar(this.getsVar().getUpdateExp(newExp, index));

		switch (this.kind) {
		case SELECT:
			break;
		case UPDATE:
			for (Column c : this.getU_updates().keySet())
				this.getU_updates().put(c, this.getU_updates().get(c).getUpdateExp(newExp, index));
			break;
		case DELETE:
			break;
		case INSERT:
			List<Expression> newIValues = new ArrayList<Expression>();
			for (Expression e : this.getI_values())
				newIValues.add(e.getUpdateExp(newExp, index));
			this.setI_values(newIValues);
			break;
		default:
			break;
		}
	}

	// private Exp

	private Map<Column, Expression> extractUfuncs() {
		Map<Column, Expression> result = new HashMap<Column, Expression>();
		switch (this.kind) {
		case UPDATE:
			Update updateStatement = (Update) statements;
			int iter = 0;
			for (net.sf.jsqlparser.schema.Column c : updateStatement.getColumns()) {
				Column myCol = null;
				try {
					myCol = table.getColumn(c.toString());
				} catch (ColumnDoesNotExist e) {
					e.printStackTrace();
				}
				net.sf.jsqlparser.expression.Expression exp = updateStatement.getExpressions().get(iter++);

				if (exp.toString().equals("?")) {
					JdbcParameter jp = (JdbcParameter) exp;
					result.put(myCol, new UnknownExp("?", jp.getIndex()));
				} else
					switch (myCol.getType()) {
					case INT:
						result.put(myCol, new ConstValExp(Integer.parseInt(exp.toString())));
						break;
					case STRING:
						result.put(myCol, new ConstValExp(exp.toString()));
						break;
					case REAL:
						result.put(myCol, new ConstValExp(Double.parseDouble(exp.toString())));
						break;
					case BOOLEAN:
						result.put(myCol, new ConstValExp(Boolean.parseBoolean(exp.toString())));
						break;
					default:
						break;
					}
			}
			return result;
		default:
			return null;
		}
	}

	private List<Expression> extractIVals() {
		List<Expression> result = new ArrayList<Expression>();
		switch (this.kind) {
		case INSERT:
			Insert insertStatement = (Insert) statements;
			ExpressionList itemsList = (ExpressionList) insertStatement.getItemsList();
			int iter = 0;
			// because I can't currently extract literals/types from SQL (due to some
			// horrible JSQLParser lib error) I'm gonna use information from the known table
			// for now
			for (net.sf.jsqlparser.expression.Expression i : itemsList.getExpressions()) {
				if (i.toString().equals("?")) {
					JdbcParameter jp = (JdbcParameter) i;
					result.add(new UnknownExp("?", jp.getIndex()));
					iter++;
				} else
					switch (this.table.getColumns().get(iter++).getType()) {
					case INT:
						result.add(new ConstValExp(Integer.parseInt(i.toString())));
						break;
					case STRING:
						result.add(new ConstValExp(i.toString()));
						break;
					case REAL:
						result.add(new ConstValExp(Double.parseDouble(i.toString())));
						break;
					case BOOLEAN:
						result.add(new ConstValExp(Boolean.parseBoolean(i.toString())));
						break;
					default:
						break;
					}
			}
			return result;
		default:
			return null;
		}
	}

	private List<Column> extractSCols() {
		List<Column> result = new ArrayList<Column>();
		switch (this.kind) {
		case SELECT:
			Select select = (Select) statements;
			PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
			// TODO: this is stupid (should be fine for now since tables are not big)
			for (SelectItem si : plainSelect.getSelectItems())
				if (si.toString().equals("*"))
					return table.getColumns();
				else {
					for (Column c : table.getColumns())
						if (si.toString().equals(c.getName()))
							result.add(c);
				}
			return result;
		default:
			return null;
		}
	}

	private Expression extractWC() throws WhereClauseNotKnownException {
		switch (this.kind) {
		case SELECT:
			Select select = (Select) statements;
			PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
			return recExtractWC(plainSelect.getWhere());
		case UPDATE:
			Update updateStatement = (Update) statements;
			return recExtractWC(updateStatement.getWhere());
		case DELETE:
			Delete deleteStatement = (Delete) statements;
			return recExtractWC(deleteStatement.getWhere());
		case INSERT:
			return new UnknownExp("?", -1);
		default:
			break;
		}
		return null;
	}

	private Expression recExtractWC(net.sf.jsqlparser.expression.Expression clause)
			throws WhereClauseNotKnownException {
		switch (clause.getClass().getSimpleName()) {
		case "EqualsTo":
			EqualsTo eq = (EqualsTo) clause;
			return new BinOpExp(BinOp.EQ, recExtractWC(eq.getLeftExpression()), recExtractWC(eq.getRightExpression()));
		case "AndExpression":
			AndExpression ae = (AndExpression) clause;
			return new BinOpExp(BinOp.AND, recExtractWC(ae.getLeftExpression()), recExtractWC(ae.getRightExpression()));
		case "MinorThan":
			MinorThan mt = (MinorThan) clause;
			return new BinOpExp(BinOp.LT, recExtractWC(mt.getLeftExpression()), recExtractWC(mt.getRightExpression()));
		case "GreaterThan":
			GreaterThan gt = (GreaterThan) clause;
			return new BinOpExp(BinOp.GT, recExtractWC(gt.getLeftExpression()), recExtractWC(gt.getRightExpression()));
		case "GreaterThanEquals":
			GreaterThanEquals gte = (GreaterThanEquals) clause;
			return new BinOpExp(BinOp.GEQ, recExtractWC(gte.getLeftExpression()),
					recExtractWC(gte.getRightExpression()));
		case "MinorThanEquals":
			MinorThanEquals mte = (MinorThanEquals) clause;
			return new BinOpExp(BinOp.LEQ, recExtractWC(mte.getLeftExpression()),
					recExtractWC(mte.getRightExpression()));
		case "NotEqualsTo":
			NotEqualsTo neq = (NotEqualsTo) clause;
			return new UnOpExp(UnOp.NOT, new BinOpExp(BinOp.EQ, recExtractWC(neq.getLeftExpression()),
					recExtractWC(neq.getRightExpression())));
		case "Column":
			Column c = null;
			try {
				c = table.getColumn(clause.toString());
				return new ProjValExp(c, this.table);
			} catch (ColumnDoesNotExist e) {
				e.printStackTrace();
				return new ConstValExp(clause.toString());
			}

		case "JdbcParameter":
			JdbcParameter jp = (JdbcParameter) clause;
			return new UnknownExp("?", jp.getIndex());
		case "LongValue":
			long lv = ((LongValue) clause).getValue();
			return new ConstValExp(lv);
		default:
			throw new WhereClauseNotKnownException(
					"Query.java.recExtractWC: clase node not handled yet: " + clause.getClass().getSimpleName());
		}

	}

	private Table extractTable() throws Exception {
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tableList = tablesNamesFinder.getTableList(statements);
		for (Table t : tables)
			if (t.getName().toLowerCase().equals(tableList.get(0).toLowerCase()))
				return t;
		throw new Exception("Query was not created -> table does not exist: " + this.text);
	}

	private Kind extractKind() {
		switch (statements.getClass().getSimpleName()) {
		case "Select":
			return Kind.SELECT;
		case "Update":
			return Kind.UPDATE;
		case "Insert":
			return Kind.INSERT;
		case "Delete":
			return Kind.DELETE;
		default:
			return null;
		}
	}

	public Kind getKind() {
		return this.kind;
	}

	public Table getTable() {
		return this.table;
	}

	public Expression getWhClause() {
		return this.whereClause;
	}

	public String toString() {
		String k = this.kind.toString();
		String t = this.table.getName();
		String wc = this.whereClause.toString();
		switch (this.kind) {
		case SELECT:
			String c = this.s_columns.toString();
			return k + "[" + t + ":" + c + "] " + " <<" + wc + ">>";
		case INSERT:
			String v = this.getI_values().toString();
			return k + "[" + t + "] " + v;
		case DELETE:
			return k + "[" + t + "] " + "<<" + wc + ">>";
		case UPDATE:
			String u = this.getU_updates().toString();
			return k + "[" + t + "] " + u + " <<" + wc + ">>";

		default:
			return "UNKNOWN QUERY";
		}
	}

	// GETTER & SETTERS
	public String getText() {
		return text;
	}

	public void addStmt(RowSetVarExp newExp) {
		this.setsVar(newExp);
	}

	public Expression getsVar() {
		return sVar;
	}

	public void setsVar(Expression sVar) {
		this.sVar = sVar;
	}

	public List<Expression> getI_values() {
		return i_values;
	}

	public void setI_values(List<Expression> i_values) {
		this.i_values = i_values;
	}

	public Map<Column, Expression> getU_updates() {
		return u_updates;
	}

	public void setU_updates(Map<Column, Expression> u_updates) {
		this.u_updates = u_updates;
	}

}
