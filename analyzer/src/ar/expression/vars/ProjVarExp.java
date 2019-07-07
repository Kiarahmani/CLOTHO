package ar.expression.vars;

import ar.expression.Expression;
import ar.expression.vars.RowVarExp;
import ar.ddl.Column;
import ar.ddl.Table;

// these nodes represent columns; e.g. in conditionals (as opposed to fieldAccess which represent values of columns in a given specific row)
public class ProjVarExp extends VarExp {
	Column column;
	RowVarExp rVar;

	public ProjVarExp(String name, Column column, RowVarExp rVar) {
		super(name);
		this.column = column;
		this.rVar = rVar;
	}

	public RowVarExp getRVar() {
		return this.rVar;
	}

	public String toString() {
		return "(" + rVar.toString() + ")." + this.column.getName();
	}

	public Column getColumn() {
		return this.column;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
