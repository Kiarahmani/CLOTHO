import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class MyDriver implements Driver {

	public Connection connect(String url, Properties info) throws SQLException {
		if (url.contains("cassandra"))
			return null;
		MyConnection myc = null;
		int insID = Integer.valueOf(info.getProperty("id"));
		try {
			myc = new MyConnection(insID);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return myc;
	}

	public boolean acceptsURL(String url) throws SQLException {
		// TODO Auto-generated method stub
		return true;
	}

	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public int getMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean jdbcCompliant() {
		// TODO Auto-generated method stub
		return false;
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

}
