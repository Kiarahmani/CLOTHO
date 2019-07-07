package ar.statement;

import ar.expression.Expression;

public class Statement {
	private Expression pathCond;

	public Statement(Expression pathCond) {
		this.setPathCond(pathCond);
	}
	
	
	public void updatePathCond(Expression newPathCond) {
		this.setPathCond(newPathCond);
	}

	public Expression getPathCond() {
		return pathCond;
	}

	public void setPathCond(Expression pathCond) {
		this.pathCond = pathCond;
	}
}
