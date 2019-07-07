package fec.utils;

import java.util.List;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import fec.UnitData;
import ar.Type;
import ar.statement.Query;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInvokeStmt;
import soot.jimple.InvokeExpr;

public class QueryPatcher {
	// will replace the queries with holes with patched ones
	// might need to recursive track variable outside of the scope of this iteration
	public void patchQuery(Unit u, ValueToExpression veTranslator, UnitData data) {
		Query q = data.getQueryFromUnit(u);
		Value value = ((GAssignStmt) u).getLeftOp();
		List<Unit> executeUnits = data.getInvokeListFromVal(value);
		if (executeUnits != null)
			for (Unit eu : executeUnits) {
				try {
					// if it is an invoke, e.g. setInt
					InvokeExpr ieu = ((GInvokeStmt) eu).getInvokeExpr();
					String mName = ieu.getMethod().getName();
					if (ieu.getArgCount() != 0) {
						try {
							switch (mName) {
							case "setInt":
								q.patch(Integer.parseInt(ieu.getArg(0).toString()), veTranslator
										.valueToExpression(false, data.getLoopNo(u), Type.INT, u, ieu.getArg(1)));
								break;
							case "setString":
								q.patch(Integer.parseInt(ieu.getArg(0).toString()), veTranslator
										.valueToExpression(false, data.getLoopNo(u), Type.STRING, u, ieu.getArg(1)));
								break;
							case "setLong":
								q.patch(Integer.parseInt(ieu.getArg(0).toString()), veTranslator
										.valueToExpression(false, data.getLoopNo(u), Type.INT, u, ieu.getArg(1)));
								break;
							case "setBool":
								q.patch(Integer.parseInt(ieu.getArg(0).toString()), veTranslator
										.valueToExpression(false, data.getLoopNo(u), Type.BOOLEAN, u, ieu.getArg(1)));
								break;
							case "setDouble":
								q.patch(Integer.parseInt(ieu.getArg(0).toString()), veTranslator
										.valueToExpression(false, data.getLoopNo(u), Type.REAL, u, ieu.getArg(1)));
								break;
							default:
								break;
							}

						} catch (NumberFormatException | UnknownUnitException | ColumnDoesNotExist e) {
							System.out.println("Qery:"+q);
							e.printStackTrace();
						}

					}
				} catch (ClassCastException e) {

				}
			}
	}
}
