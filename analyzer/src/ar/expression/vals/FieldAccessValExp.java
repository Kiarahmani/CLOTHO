package ar.expression.vals;

import ar.expression.Expression;
import ar.expression.vars.RowVarExp;
import ar.ddl.Column;

public class FieldAccessValExp extends ValExp {
	RowVarExp projectee;
	Column col;

	public FieldAccessValExp(RowVarExp projectee, Column col) {
		this.projectee = projectee;
		this.col = col;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
