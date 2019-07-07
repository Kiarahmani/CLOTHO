package ar.expression;

public class BinOpExp extends Expression {
	public enum BinOp {
		PLUS, MINUS, MULT, DIV, AND, OR, XOR, GEQ, LEQ, LT, GT, EQ,
	};

	public BinOp op;
	public Expression e1, e2;

	public BinOpExp(BinOp op, Expression e1, Expression e2) {
		this.op = op;
		this.e1 = e1;
		this.e2 = e2;
	}

	public String toString() {
		return "(" + op + " " + e1.toString() + "," + e2.toString() + ")";
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return new BinOpExp(this.op, this.e1.getUpdateExp(newExp, index), this.e2.getUpdateExp(newExp, index));
	}

}
