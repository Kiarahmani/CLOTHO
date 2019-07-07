package ar.expression.vars;

import ar.expression.Expression;
import ar.ddl.Table;

public class RowSetVarExp extends VarExp {

	private Table table;
	private Expression whereClause;

	public RowSetVarExp(String name, Table table, Expression wh) {
		super(name);
		this.table = table;
		this.whereClause = wh;
	}

	public Expression getWhClause() {
		return this.whereClause;
	}
	
	public String getName() {
		return super.getName();
	}



	public Table getTable() {
		return this.table;
	}

	public String toString() {
		return "ROW SET: " + this.table.getName() + " WHERE: " + whereClause;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		this.whereClause = this.whereClause.getUpdateExp(newExp, index);
		return this;
	}

}
