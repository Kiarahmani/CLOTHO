package com.github.kiarahmani.replayer;

public class App {
	public static void main(String[] args) {
		int id = Integer.valueOf(args[0]);
		Transaction txn = new Transaction(id);
		txn.run();

	}
}
