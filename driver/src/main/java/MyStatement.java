import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import sync.OpType;
import sync.RemoteService;

public class MyStatement implements Statement {

	private Statement orgStatement;
	private RemoteService stub;
	private OpType type;
	private MyConnection con;

	public MyStatement(Statement orgS, RemoteService stub, OpType ot, MyConnection con) {
		this.orgStatement = orgS;
		this.stub = stub;
		this.type = ot;
		this.con = con;
	}

	// looks at the parent connection's current seq number and returns a fresh type
	// to be used at the scheduler
	//private OpType updatedType() {
	//	return new OpType(this.type.getTxnInsID(), this.type.getKind(), con.getSeq());
	//}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public ResultSet executeQuery(String sql) throws SQLException {

	//	try {
	//		stub.execRequest(updatedType());
	//	} catch (RemoteException e) {
	//		e.printStackTrace();
	//	}
	//	con.incSeq();
		return orgStatement.executeQuery(sql);
	}

	public int executeUpdate(String sql) throws SQLException {
	//	try {
	//		stub.execRequest(updatedType());
	//	} catch (RemoteException e) {
	//		e.printStackTrace();
	//	}
	//	con.incSeq();
		return orgStatement.executeUpdate(sql);
	}

	public void close() throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getMaxFieldSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setMaxFieldSize(int max) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getMaxRows() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setMaxRows(int max) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getQueryTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void cancel() throws SQLException {
		// TODO Auto-generated method stub

	}

	public SQLWarning getWarnings() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public void clearWarnings() throws SQLException {
		// TODO Auto-generated method stub

	}

	public void setCursorName(String name) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean execute(String sql) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public ResultSet getResultSet() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public int getUpdateCount() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean getMoreResults() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setFetchDirection(int direction) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getFetchDirection() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setFetchSize(int rows) throws SQLException {
		// TODO Auto-generated method stub

	}

	public int getFetchSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getResultSetConcurrency() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getResultSetType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void addBatch(String sql) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void clearBatch() throws SQLException {
		// TODO Auto-generated method stub

	}

	public int[] executeBatch() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean getMoreResults(int current) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean execute(String sql, String[] columnNames) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void setPoolable(boolean poolable) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isPoolable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}