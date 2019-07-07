package benchmarks.test;

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

public class test {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public test(int id) {
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

	//
	// *****************
	public void transaction(int id, int amount, double param2, String param3, float param4) throws SQLException {
		PreparedStatement stmt = connect
				.prepareStatement("SELECT balance, name " + "  FROM " + "ACCOUNTS" + " WHERE ID = ?");
		stmt.setInt(1, id);

		ResultSet rs = stmt.executeQuery();
		rs.next();
		int old_bal = rs.getInt("balance");
		PreparedStatement stmt2 = connect.prepareStatement("UPDATE ACCOUNTS SET BALANCE = ?" + " WHERE ID = ?");
		stmt2.setInt(1, old_bal + amount);
		stmt2.setInt(2, id);
		stmt2.executeUpdate();

	}

}
