package exceptions;

public class ColumnDoesNotExist extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7558835075625909910L;

	public ColumnDoesNotExist(String n) {
		super("Requested Column: "+n+" does not exist");

	}

}
