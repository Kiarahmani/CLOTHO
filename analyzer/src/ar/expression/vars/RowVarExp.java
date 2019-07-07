package ar.expression.vars;

import ar.expression.Expression;
import ar.ddl.Table;

public class RowVarExp extends VarExp {

	private Table table;
	private RowSetVarExp belongsTo;

	public RowVarExp(String name, Table table, RowSetVarExp belongsTo) {
		super(name);
		this.table = table;
		this.belongsTo = belongsTo;
	}

	public RowSetVarExp getSetVar() {
		return this.belongsTo;
	}

	public Table getTable() {
		return this.table;
	}

	public String toString() {
		return "ROW:" + this.table.getName() + "-" + this.getName();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
