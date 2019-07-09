package ar.statement;

import ar.expression.Expression;

public class InvokeStmt extends Statement {
	public InvokeStmt(Expression pathCond, Query query) {
		super(pathCond);
		this.query = query;
	}

	private SqlStmtType type;
	Query query;

	public void setType(SqlStmtType type) {
		this.type = type;
	}

	public Query getQuery() {
		return this.query;
	}

	public String toString() {
		String firstPart = this.type + " (" + this.query + ")";
		String whiteSpace = String.format("%0" + Math.max((100 - firstPart.length()), 10) + "d", 0).replace("0", " ");
		return "" +firstPart  + "\n    [PATH CONDITION: " + super.getPathCond() + "]";
	}

	public Expression getPathCondition() {
		return super.getPathCond();
	}

	public SqlStmtType getType() {
		return type;
	}
}
