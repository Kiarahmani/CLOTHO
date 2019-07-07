package ar.expression.vals;

import ar.expression.Expression;
import ar.expression.vars.RowSetVarExp;
import soot.Value;

public class NullExp extends ValExp {
	// because at the generation time of this construct we don't have rSets yet, we
	// should first make them by the Soot Value they represent and then at the end
	// of the of the analysis patch them up
	public Value rSet;
	Expression RowSetVarExp;

	public NullExp(Value rSet) {
		this.rSet = rSet;
	}

	public String getName() {
		return this.rSet.toString();
	}

	public String toString() {
		return "NULL[" + this.RowSetVarExp + "]";
	}

	public void updateRowSetExp(Expression RowSetVarExp) {
		this.RowSetVarExp = RowSetVarExp;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
