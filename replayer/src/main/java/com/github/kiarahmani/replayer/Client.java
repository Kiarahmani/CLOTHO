package com.github.kiarahmani.replayer;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.github.adejanovski.cassandra.jdbc.CassandraConnection;

public class Client {
	private Connection connect = null;
	private Statement stmt = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_UNCOMMITTED;
	private int id;
	Properties p;

	public Client(int id) {
		this.id = id;
		p = new Properties();
		p.setProperty("id", String.valueOf(this.id));
		Object o;
		try {
			o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect("", p);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void close() {
		try {
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 1
	// ************************************************************************************
	public void amalgamate(Long custId0, Long custId1) throws SQLException {
		// 1 - SELECT - ACCOUNTS - 1
		// Get Account Information
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt0.setLong(1, custId0);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custId0 + "'";
			System.out.println(msg);
			return;
		}
		// 2 - SELECT - ACCOUNTS - 2 
		PreparedStatement stmt1 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ? ");
		stmt1.setLong(1, custId1);
		ResultSet r1 = stmt1.executeQuery();
		if (r1.next() == false) {
			String msg = "Invalid account '" + custId0 + "'";
			System.out.println(msg);
			return;
		}
		// 3 - SELECT - SAVINGS - 1 
		// Get Balance Information
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?  ");
		balStmt0.setLong(1, custId0);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId0);
			System.out.println(msg);
			return;
		}
		int old_sv_cust0 = balRes0.getInt("bal");

		System.out.println("balance at custid: " + custId0 + ": " + old_sv_cust0);

		// 4 - SELECT - CHECKING - 1
		// Get Balance Information
		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?   ");
		balStmt1.setLong(1, custId1);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId0);
			System.out.println(msg);
			return;
		}
		long old_ck_cust1 = balRes1.getLong("bal");

		// sum of the checking and saving accounts read above
		long total = old_sv_cust0 + old_ck_cust1;

		// 5 - UPDATE - CHECKING - 1
		// Update Balance Information
		PreparedStatement updateStmt0 = connect
				.prepareStatement("UPDATE " + "CHECKING" + " SET bal = 0 " + "WHERE custid = ?");
		updateStmt0.setLong(1, custId0);
		updateStmt0.executeUpdate();

		// 6 - SELECT - SAVINGS - 2
		PreparedStatement updateStmt10 = connect
				.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?    ");
		updateStmt10.setLong(1, custId1);
		ResultSet balRes10 = updateStmt10.executeQuery();
		if (balRes10.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId1);
			System.out.println(msg);
			return;
		}
		long old_bal = balRes10.getLong("bal");
		System.out.println("balance at custid: " + custId1 + ": " + old_bal);

		// 7 - UPDATE - SAVINGS - 1
		PreparedStatement updateStmt11 = connect
				.prepareStatement("UPDATE " + "SAVINGS" + "   SET bal =  ? " + " WHERE custid = ?");
		updateStmt11.setLong(1, old_bal - total);
		updateStmt11.setLong(2, custId1);
		updateStmt11.executeUpdate();

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void balance(String custName) throws SQLException {
		// 1 - SELECT - ACCOUNTS - 1
		PreparedStatement stmt0 = connect
				.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ? ALLOW FILTERING");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		long custId = r0.getLong("custid");

		// Then get their account balances
		// 2 - SELECT - SAVINGS - 1
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setLong(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId);
			System.out.println(msg);
			return;
		}
		long savings = balRes0.getLong("bal");
		// 3 - SELECT - CHECKING - 1
		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setLong(1, custId);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		long checking = balRes1.getLong("bal");

		// show the results
		System.out.println(savings + checking);

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void depositChecking(String custName, Long amount) throws SQLException {
		// First convert the custName to the custId
		// 1 - SELECT - ACCOUNTS - 1
		PreparedStatement stmt0 = connect
				.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ? ALLOW FILTERING");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		long custId = r0.getLong("custid");

		// Then update their checking balance
		// 2 - SELECT - CHECKING - 1
		PreparedStatement stmt10 = connect.prepareStatement("SELECT * FROM " + "CHECKING" + " WHERE custid = ?");
		stmt10.setLong(1, custId);
		ResultSet r10 = stmt10.executeQuery();
		if (r10.next() == false) {
			String msg = "Invalid checking '" + custId + "'";
			System.out.println(msg);
			return;
		}
		int old_bal = r10.getInt("bal");
		// 3 - UPDATE - CHECKING - 1 
		PreparedStatement stmt11 = connect.prepareStatement("UPDATE CHECKING SET bal = ? " + " WHERE custid = ?");
		stmt11.setLong(1, old_bal - amount);
		stmt11.setLong(2, custId);
		stmt11.executeUpdate();

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void sendPayment(Long sendAcct, Long destAcct, Long amount) throws SQLException {
		// 1 - SELECT - ACCOUNTS - 1
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt0.setLong(1, sendAcct);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + sendAcct + "'";
			System.out.println(msg);
			return;
		}

		// 2 - SELECT - ACCOUNTS - 2
		PreparedStatement stmt1 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ? ");
		stmt1.setLong(1, destAcct);
		ResultSet r1 = stmt1.executeQuery();
		if (r1.next() == false) {
			String msg = "Invalid account '" + destAcct + "'";
			System.out.println(msg);
			return;
		}

		// Get the sender's account balance
		// 3 - SELECT - CHECKING - 1
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt0.setLong(1, sendAcct);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", sendAcct);
			System.out.println(msg);
			return;
		}
		long old_bal_send = balRes0.getLong("bal");

		// Make sure that they have enough money
		if (old_bal_send > amount) {
			// Update the sender's account balance
			// 4 - UPDATE - CHECKING - 1
			PreparedStatement deptStmt = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			deptStmt.setLong(1, old_bal_send - amount);
			deptStmt.setLong(2, sendAcct);
			deptStmt.executeUpdate();

			// Get the receivers's account balance
			// 5 - SELECT - CHECKING - 2
			PreparedStatement crdtStmt1 = connect
					.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?  ");
			crdtStmt1.setLong(1, destAcct);
			ResultSet balRes1 = crdtStmt1.executeQuery();
			if (balRes1.next() == false) {
				String msg = String.format("No %s for customer #%d", "CHECKING", destAcct);
				System.out.println(msg);
				return;
			}
			long old_bal_dest = balRes1.getLong("bal");

			// Update the receivers's account balance
			// 6 - UPDATE - CHECKING 2
			PreparedStatement crdtStmt2 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ? ");
			crdtStmt2.setLong(1, old_bal_dest + amount);
			crdtStmt2.setLong(2, destAcct);
			crdtStmt2.executeUpdate();

		}
		else {
			System.out.println("ERROR: NSF "+ old_bal_send);
		}

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void transactSavings(String custName, Long amount) throws SQLException {
		// First convert the custName to the custId
		// 1 - SELECT - ACCOUNTS - 1
		PreparedStatement stmt0 = connect
				.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ? ALLOW FILTERING");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		long custId = r0.getLong("custid");

		System.out.println("~~~>"+custId);
		// Get the account balance
		// 2 - SELECT - SAVINGS - 1
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setLong(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		// Update the balance
		long bal = balRes0.getLong("bal") - amount;
		if (bal > 0) {
			// 3 - UPDATE - SAVINGS - 1
			PreparedStatement deptStmt = connect.prepareStatement("UPDATE SAVINGS SET bal = ? WHERE custid = ?");
			deptStmt.setLong(1, bal);
			deptStmt.setLong(2, custId);
			deptStmt.executeUpdate();
		}
		else
		{
			System.out.println("custid: "+custId);
			System.out.println("balRes0.getLong(): "+balRes0.getLong("bal"));
			System.out.println("bal is: "+bal);
			System.out.println("amoun is: "+amount);
		}
	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void writeCheck(String custName, Long amount) throws SQLException {
		// First convert the custName to the custId
		// 1 - SELECT - ACCOUNTS - 1
		PreparedStatement stmt0 = connect
				.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ? ALLOW FILTERING");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		long custId = r0.getLong("custid");

		// Then get their account balances
		// 2 - SELECET - SAVINGS - 1
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setLong(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId);
			System.out.println(msg);
			return;
		}
		long savings = balRes0.getLong("bal");
		// 3 - SELECT - CHECKING - 1
		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setLong(1, custId);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		long checking = balRes1.getLong("bal");

		if (savings + checking > amount) {
			// Update the receivers's account balance
			// 4 - UPDATE - CHECKING - 1
			PreparedStatement crdtStmt1 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			crdtStmt1.setLong(1, checking - amount);
			crdtStmt1.setLong(2, custId);
			crdtStmt1.executeUpdate();
		} else {
			// Update the receivers's account balance
			// 5 - UPDATE - CHECKING - 1
			PreparedStatement crdtStmt2 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			crdtStmt2.setLong(1, checking - (amount - 1));
			crdtStmt2.setLong(2, custId);
			crdtStmt2.executeUpdate();
		}

	}

}
