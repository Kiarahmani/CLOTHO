package ar.expression.vars;

import ar.expression.Expression;

public class UnknownExp extends VarExp {

	int index;

	public UnknownExp(String name, int index) {
		super(name);
		this.index = index;
	}

	public String toString() {
		return super.getName();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		if (this.index == index)
			
			return newExp;
		else
			return this;
	}

}
