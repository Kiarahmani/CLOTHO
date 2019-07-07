package com.github.kiarahmani.replayer;

public class App {
	public static void main(String[] args) {
		int id = Integer.valueOf(args[0]);
		String benchmark = args[1];
		int testNumber = Integer.valueOf(args[2]);
		Transaction txn = new Transaction(id, benchmark, testNumber);
		txn.run();

	}
}
