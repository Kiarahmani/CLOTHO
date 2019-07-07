package ar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ar.expression.Expression;
import ar.expression.vals.ParamValExp;
import ar.expression.vars.VarExp;
import ar.statement.AssignmentStmt;
import ar.statement.InvokeStmt;
import ar.statement.SqlStmtType;
import ar.statement.Statement;
import soot.Value;

public class Transaction {
	private String name;
	private ArrayList<Statement> stmts;
	private Map<String, ParamValExp> params;
	private Map<Value, Expression> exps;

	public String getName() {
		return this.name;
	}

	public Transaction(String name) {
		this.name = name;
		this.stmts = new ArrayList<Statement>();
		this.params = new HashMap<String, ParamValExp>();
		this.exps = new LinkedHashMap<Value, Expression>();
	}

	public void addParam(String l, ParamValExp p) {
		this.params.put(l, p);
	}

	public void addStmt(Statement stmt) {
		this.stmts.add(stmt);
	}

	public Statement getStmtByType(String type) {
		List<InvokeStmt> invokations = new ArrayList<InvokeStmt>();
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				invokations.add(is);
			} catch (Exception e) {
			}

		List<InvokeStmt> x = invokations.stream().filter(invk -> invk.getType().toString().equals(type))
				.collect(Collectors.toList());
		if (x.size() > 0)
			return x.get(0);
		else
			return null;

	}

	public void setTypes() {
		int selectCount = 0;
		int insertCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		int seq = 0;
		for (Statement s : stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				if (is.getQuery().getText().toLowerCase().contains("select"))
					is.setType(new SqlStmtType(name, "select", ++selectCount, false, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("insert"))
					is.setType(new SqlStmtType(name, "insert", ++insertCount, true, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("update"))
					is.setType(new SqlStmtType(name, "update", ++updateCount, true, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("delete"))
					is.setType(new SqlStmtType(name, "delete", ++deleteCount, true, ++seq));

			} catch (Exception e) {
			}

	}

	public Map<String, ParamValExp> getParams() {
		return this.params;
	}

	// return mapping from program order to the stmt name
	public Map<Integer, String> getStmtNamesMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		int iter = -1;
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				result.put(is.getType().getSeq(), is.getType().toString());
			} catch (Exception e) {
			}
		return result;
	}

	public String[] getStmtNames() {
		List<String> invokations = new ArrayList<String>();
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				invokations.add(is.getType().toString());
			} catch (Exception e) {

			}
		String[] result = invokations.toArray(new String[invokations.size()]);
		return result;
	}

	public List<String> getUpdateStmtNames() {
		List<String> result = new ArrayList<String>();
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				if (is.getType().isUpdate)
					result.add(is.getType().toString());
			} catch (Exception e) {
			}
		return result;
	}

	public List<VarExp> getAllLhsVars() {
		List<VarExp> result = new ArrayList<VarExp>();
		for (Statement s : this.stmts)
			try {
				AssignmentStmt as = (AssignmentStmt) s;
				result.add(as.getLhs());
			} catch (Exception e) {
			}
		return result;

	}

	public Map<Value, Expression> getAllExps() {
		return this.exps;
	}

	public void setExps(Map<Value, Expression> exps) {
		this.exps = exps;
	}

	public ArrayList<Statement> getStmts() {
		return this.stmts;
	}

	public void printTxn() {
		String paramList = " (";
		int iter = 0;
		for (String s : params.keySet()) {
			paramList += (s + ":" + params.get(s).getType());
			if (iter++ < params.size() - 1)
				paramList += ",";
		}

		paramList += ")";
		System.out.println("\nTXN_" + name + paramList);
		for (Statement stmt : stmts)
			try {
				System.out.println(" ++ " + ((InvokeStmt) stmt).toString());
			} catch (ClassCastException e) {
				System.out.println(" ++ UNEXPECTED -> cast to (InvokeStmt) failed ...");
			}
	}

}
