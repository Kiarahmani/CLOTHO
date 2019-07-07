package ar.expression.vals;

import ar.expression.Expression;
import ar.ddl.Column;
import ar.ddl.Table;

// these nodes represent columns; e.g. in conditionals (as opposed to fieldAccess which represent values of columns in a given specific row)
public class ProjValExp extends ValExp {
	public Column column;
	public Table table;

	public ProjValExp(Column column, Table table) {
		this.table = table;
		this.column = column;
	}

	public Column getColumn() {
		return this.column;
	}

	public String toString() {
		return this.table.getName() + "." + this.column.getName();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
