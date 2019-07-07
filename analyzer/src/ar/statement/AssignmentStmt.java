package ar.statement;

import ar.expression.Expression;
import ar.expression.vars.VarExp;

public class AssignmentStmt extends Statement {
	public AssignmentStmt(Expression pathCond, VarExp lhs, Expression rhs) {
		super(pathCond);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public VarExp getLhs() {
		return lhs;
	}

	public Expression getRhs() {
		return rhs;
	}

	VarExp lhs;
	Expression rhs;

}
