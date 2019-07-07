package ar.expression.vals;

import ar.Type;
import ar.expression.Expression;

public class ParamValExp extends ValExp {
	Type type;
	String name;
	String originalName;

	public ParamValExp(String name, Type type, String originalName) {
		this.type = type;
		this.name = name;
		this.originalName = originalName;
	}

	public Type getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}

	public String toString() {
		return this.name + "_param";
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}
}
