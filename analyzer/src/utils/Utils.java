package utils;

import exceptions.SqlTypeNotFoundException;
import ar.Type;

public class Utils {
	// converts SQL schema definitions' types to ir.Types
	public static Type convertType(String t) throws SqlTypeNotFoundException {
		if (t.contains("VARCHAR"))
			return Type.STRING;
		else {
			switch (t) {
			case "INT":
				return Type.INT;
			case "INTEGER":
				return Type.INT;
			case "SMALLINT":
				return Type.INT;
			case "BIGINT":
				return Type.INT;
			case "NUMERIC":
				return Type.INT;
			case "REAL":
				return Type.REAL;
			case "FLOAT":
				return Type.REAL;
			case "DOUBLE":
				return Type.REAL;
			case "DOUBLE PRECISION":
				return Type.REAL;
			case "DATE":
				return Type.INT;
			case "TIME":
				return Type.INT;
			case "CHARACTER":
				return Type.STRING;
			case "CHAR":
				return Type.STRING;
			default:
				throw new SqlTypeNotFoundException("SQL Type: " + t + " is unknown");
			}
		}

	}

	
}
