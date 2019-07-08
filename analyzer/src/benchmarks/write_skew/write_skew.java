package benchmarks.write_skew;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class write_skew {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public write_skew(int id) {
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

	public void one_increment(int key1 , int key2 , int amount2) throws SQLException {
		// read account 1
		PreparedStatement stmt = connect.prepareStatement("SELECT value " + "FROM " + "ACCOUNTS" + " WHERE id = ?");
		stmt.setInt(1, key1);
		ResultSet rs = stmt.executeQuery();
		rs.next();
		int read_val = rs.getInt("VALUE");
		System.out.println(read_val);
		// update account 2
		if (read_val + amount2 < 1000 ) {
			PreparedStatement stmt1 = connect.prepareStatement("UPDATE ACCOUNTS SET value = ?" + " WHERE id = ?");
			stmt1.setInt(1, amount2);
			stmt1.setInt(2, key2);
			stmt1.executeUpdate();
		}
	}
}
