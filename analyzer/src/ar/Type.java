package ar;

import exceptions.SqlTypeNotFoundException;
import soot.Value;

public enum Type {
	INT, REAL, STRING, BOOLEAN;

	public String toZ3String() {
		switch (this) {
		case INT:
			return "Int";
		case REAL:
			return "Real";
		case STRING:
			return "String";
		case BOOLEAN:
			return "Bool";
		default:
			return null;
		}
	}

	public Type fromJavaTypes(Value v) throws SqlTypeNotFoundException {
		switch (v.getType().toString()) {
		case "int":
			return INT;
		case "java.lang.String":
			return Type.STRING;
		case "boolean":
			return BOOLEAN;
		case "double":
			return REAL;
		case "float":
			return REAL;
		case "long":
			return INT;
		case "int[]":
			return Type.STRING;
		default:
			throw new SqlTypeNotFoundException(
					"Type.java.fromJavaTypes : --- unhandled java type (" + v.toString() + ")");
		}
	}
}
