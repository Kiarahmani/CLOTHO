import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import sync.OpType;
import sync.RemoteService;

public class MyConnection implements Connection {
	private Connection orgConnection;
	RemoteService stub;
	private int insID;
	private int seq;
	private Map<String, OpType> lastUpdate;
	private Map<String, OpType> lastSelect;
	private Map<String, OpType> lastDelete;
	private Map<String, OpType> lastInsert;

	public void incSeq() {
		this.seq++;
	}

	public int getSeq() {
		return this.seq;
	}

	public MyConnection(int insID) throws Exception {
		Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
		System.out.println(">> original JDBC driver loaded4");		
		orgConnection = DriverManager.getConnection("jdbc:cassandra://localhost:19041/testks");

		System.out.println("orgConnection: " + orgConnection);
		
		
		
		
		System.out.println(">> connection established: localhost:1904" + insID);
		Registry registry = LocateRegistry.getRegistry(null);
		System.out.println(">> modified driver registered");
		stub = (RemoteService) registry.lookup("RemoteService");
		System.out.println(">> RMI service connected");
		stub.printTestMsg(insID);
		this.insID = insID;
		this.seq = 0;
		lastUpdate = new HashMap<>();
		lastSelect = new HashMap<>();
		lastDelete = new HashMap<>();
		lastInsert = new HashMap<>();
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public Statement createStatement() throws SQLException {
		return null;
		// OpType ot = new OpType(this.insID, "", -10000);
		// return new MyStatement(orgConnection.createStatement(), stub, ot, this);
	}

	// XXX
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		String arr[] = (sql.replace(" ALLOW FILTERING", "")).split(" ");
		String kind = arr[0];
		OpType ot = null;
		int order = 1;
		String table = "";
		// parse the query
		if (kind.equalsIgnoreCase("delete"))
			table = arr[2];
		else if (kind.equalsIgnoreCase("update"))
			table = arr[1];
		else if (kind.equalsIgnoreCase("insert"))
			table = arr[2];
		else
			for (int i = 1; i < arr.length; i++) {
				if (arr[i].equalsIgnoreCase("from")) {
					table = arr[i + 1];
					break;
				}
			}

		// detect loops
		if (kind.equalsIgnoreCase("update")) {
			if (lastUpdate.get(table) != null)
				if (lastUpdate.get(table).getQuery().equalsIgnoreCase(sql))
					order = lastUpdate.get(table).getOrder();
				else
					order = lastUpdate.get(table).getOrder() + 1;
			ot = new OpType(this.insID, -10000, sql, kind, table, order);
			this.lastUpdate.put(table, ot);
		}
		if (kind.equalsIgnoreCase("select")) {
			if (lastSelect.get(table) != null)
				if (lastSelect.get(table).getQuery().equalsIgnoreCase(sql))
					order = lastSelect.get(table).getOrder();
				else
					order = lastSelect.get(table).getOrder() + 1;
			ot = new OpType(this.insID, -10000, sql, kind, table, order);
			this.lastSelect.put(table, ot);
		}
		if (kind.equalsIgnoreCase("delete")) {
			if (lastDelete.get(table) != null)
				if (lastDelete.get(table).getQuery().equalsIgnoreCase(sql))
					order = lastDelete.get(table).getOrder();
				else
					order = lastDelete.get(table).getOrder() + 1;
			ot = new OpType(this.insID, -10000, sql, kind, table, order);
			this.lastDelete.put(table, ot);
		}
		if (kind.equalsIgnoreCase("insert")) {
			if (lastInsert.get(table) != null)
				if (lastInsert.get(table).getQuery().equalsIgnoreCase(sql))
					order = lastInsert.get(table).getOrder();
				else
					order = lastInsert.get(table).getOrder() + 1;
			ot = new OpType(this.insID, -10000, sql, kind, table, order);
			this.lastInsert.put(table, ot);
		}

		return new MyPreparedStatement(orgConnection.prepareStatement(sql), stub, ot, this);
	}
	// XXX

	public CallableStatement prepareCall(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String nativeSQL(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		orgConnection.setAutoCommit(autoCommit);

	}

	public boolean getAutoCommit() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void commit() throws SQLException {
		orgConnection.commit();

	}

	public void rollback() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void close() throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isReadOnly() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setCatalog(String catalog) throws SQLException {
		// TODO Auto-generated method stub

	}

	public String getCatalog() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		orgConnection.setTransactionIsolation(level);
		// TODO Auto-generated method stub

	}

	public int getTransactionIsolation() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setHoldability(int holdability) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public Savepoint setSavepoint() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		// TODO Auto-generated method stub

	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Clob createClob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Blob createBlob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public NClob createNClob() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public SQLXML createSQLXML() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isValid(int timeout) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		// TODO Auto-generated method stub

	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		// TODO Auto-generated method stub

	}

	public String getClientInfo(String name) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Properties getClientInfo() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setSchema(String schema) throws SQLException {
		// TODO Auto-generated method stub

	}

	public String getSchema() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void abort(Executor executor) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getNetworkTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

}
