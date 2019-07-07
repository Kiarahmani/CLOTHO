package ar.expression.vals;

import ar.Type;
import ar.expression.Expression;

public class ConstValExp extends ValExp {
	private Type type;
	private String SVal;
	private double DVal;
	private int IVal;
	private boolean BVal;

	public Type getType() {
		return this.type;
	}

	public String getSVal() {
		return SVal;
	}

	public double getDVal() {
		return DVal;
	}

	public int getIVal() {
		return IVal;
	}

	public boolean isBVal() {
		return BVal;
	}

	public ConstValExp(int i) {
		this.IVal = i;
		this.type = Type.INT;
	}

	public ConstValExp(boolean b) {
		this.BVal = b;
		this.type = Type.BOOLEAN;
	}

	public ConstValExp(double d) {
		this.DVal = d;
		this.type = Type.REAL;
	}

	public ConstValExp(long l) {
		this.IVal = (int) l;
		this.type = Type.INT;
	}

	public ConstValExp(float d) {
		this.DVal = d;
		this.type = Type.REAL;
	}

	public ConstValExp(String s) {
		this.SVal = s;
		this.type = Type.STRING;
	}

	public String toString() {
		switch (this.type) {
		case STRING:
			return SVal;
		case INT:
			return String.valueOf(IVal);
		case REAL:
			return String.valueOf(DVal);
		case BOOLEAN:
			return String.valueOf(BVal);

		}
		return "????";
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
