package ar.expression;

/* an expression is either made of */
public abstract class Expression {

	// used to find abstract expressions which must be replaced with concrete ones
	public abstract Expression getUpdateExp(Expression newExp, int index);

}
