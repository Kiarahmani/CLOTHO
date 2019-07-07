package sync;

import java.io.Serializable;

public class OpType implements Serializable {
	/**
	 * 
	 */
	private int txnInsID;
	private String query;
	private int txnOrder;
	private String kind;
	private int order;
	private String table;

	public OpType(int txnInsID, int txnOrder, String query, String kind, String table, int order) {
		this.txnInsID = txnInsID;
		this.txnOrder = txnOrder;
		this.query = query;
		this.kind = kind;
		this.table = table;
		this.order = order;

	}

	public String getKind() {
		return this.kind;
	}

	public int getOrder() {
		return this.order;
	}

	public String getTable() {
		return this.table;
	}

	public int getTxnInsID() {
		return this.txnInsID;
	}

	public String getQuery() {
		return this.query;
	}

	public int getTxnOrder() {
		return this.txnOrder;
	}

	public boolean isEqual(OpType other) {
		return (other.getTxnInsID() == this.txnInsID && this.kind.equalsIgnoreCase(other.getKind())
				&& this.table.equalsIgnoreCase(other.getTable()) && this.order == other.getOrder());
	}

	@Override
	public boolean equals(Object obj) {

		OpType op = (OpType) obj;

		return (op.getTxnInsID() == this.txnInsID && this.kind.equalsIgnoreCase(op.getKind())
				&& this.table.equalsIgnoreCase(op.getTable()) && this.order == op.getOrder());
	}

	public String toString() {
		return "[TID:" + txnInsID + "--Seq:" + txnOrder + "--kind:" + kind + "#" + order + "--Tb:" + table + "]";

	}

	@Override
	public int hashCode() {
		return table.hashCode() + kind.hashCode() + txnInsID * 100;
	}
}
