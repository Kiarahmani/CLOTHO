package ar;

import java.util.ArrayList;
import java.util.List;

// This class captures the intermediate representation of db-backed programs which are extracted from Gimple representations
public class Application {
	private ArrayList<Transaction> txns;

	public Application() {
		txns = new ArrayList<Transaction>();
	}

	public ArrayList<Transaction> getTxns() {
		return this.txns;
	}

	public void addTxn(Transaction txn) {
		this.txns.add(txn);
	}

	public void printApp() {
		System.out.println("\n\n------------------------------------------------------"
				+ "\n 			Compiled Application\n" + "------------------------------------------------------");
		for (Transaction t : txns)
			t.printTxn();
	}

	public Transaction getTxnByName(String txnName) {
		return this.txns.stream().filter(t -> t.getName().equals(txnName)).findAny().get();
	}

	public String[] getAllStmtTypes() {
		List<String> result = new ArrayList<String>();
		int size = 0;
		for (Transaction t : this.txns)
			for (String s : t.getStmtNames()) {
				result.add(s);
				size++;
			}
		return result.toArray(new String[size]);
	}

	public List<String> getAllUpdateStmtTypes() {
		List<String> result = new ArrayList<String>();
		for (Transaction t : this.txns)
			for (String s : t.getUpdateStmtNames()) {
				result.add(s);
			}
		return result;
	}

	public String[] getAllTxnNames() {
		List<String> result = new ArrayList<String>();
		int size = 0;
		for (Transaction t : this.txns) {
			result.add(t.getName());
			size++;
		}
		return result.toArray(new String[size]);
	}

}
