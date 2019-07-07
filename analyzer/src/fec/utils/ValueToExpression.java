package fec.utils;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import fec.UnitData;
import ar.Type;
import ar.expression.BinOpExp;
import ar.expression.Expression;
import ar.expression.UnOpExp;
import ar.expression.UnOpExp.UnOp;
import ar.expression.BinOpExp.BinOp;
import ar.expression.vals.ConstValExp;
import ar.expression.vals.NullExp;
import ar.expression.vars.ProjVarExp;
import ar.expression.vars.RowVarExp;
import ar.expression.vars.UnknownExp;
import cons.ConstantArgs;
import soot.Transformer;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAddExpr;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GEqExpr;
import soot.grimp.internal.GGeExpr;
import soot.grimp.internal.GGtExpr;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GLeExpr;
import soot.grimp.internal.GLtExpr;
import soot.grimp.internal.GMulExpr;
import soot.grimp.internal.GNeExpr;
import soot.grimp.internal.GSubExpr;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.infoflow.FakeJimpleLocal;

public class ValueToExpression {
	private static final Logger LOG = LogManager.getLogger(Transformer.class);
	static UnitData data;

	public ValueToExpression(UnitData data) {
		this.data = data;
	}

	// Open Ended --- I'll add more handler upon occurence
	/* loopNo signals that constants should be abstracted for locals */
	/* shouldAbsConsts */
	public Expression valueToExpression(boolean shouldAbsConsts, int loopNo, Type tp, Unit callerU, Value v)
			throws UnknownUnitException, ColumnDoesNotExist {
		switch (v.getClass().getSimpleName()) {
		case "GSubExpr":
			GSubExpr gse = (GSubExpr) v;
			return new BinOpExp(BinOp.MINUS, valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gse.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gse.getOp2()));
		case "GAddExpr":
			GAddExpr gae = (GAddExpr) v;
			return new BinOpExp(BinOp.PLUS, valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gae.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gae.getOp2()));
		case "GMulExpr":
			GMulExpr gme = (GMulExpr) v;
			return new BinOpExp(BinOp.MULT, valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gme.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, tp, callerU, gme.getOp2()));
		case "GLeExpr":
			GLeExpr gle = (GLeExpr) v;
			return new BinOpExp(BinOp.LT, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, gle.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, gle.getOp2()));
		case "GLtExpr":
			GLtExpr glt = (GLtExpr) v;
			return new BinOpExp(BinOp.LEQ, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, glt.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, glt.getOp2()));

		case "GNeExpr":
			GNeExpr ne = (GNeExpr) v;
			return new UnOpExp(UnOp.NOT,
					new BinOpExp(BinOp.EQ, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ne.getOp1()),
							valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ne.getOp2())));

		case "GGeExpr":
			GGeExpr ge = (GGeExpr) v;
			return new BinOpExp(BinOp.GEQ, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ge.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ge.getOp2()));

		case "GEqExpr":
			GEqExpr ee = (GEqExpr) v;
			return new BinOpExp(BinOp.EQ, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ee.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, ee.getOp2()));

		case "GGtExpr":
			GGtExpr gt = (GGtExpr) v;
			return new BinOpExp(BinOp.GT, valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, gt.getOp1()),
					valueToExpression(shouldAbsConsts, loopNo, Type.REAL, callerU, gt.getOp2()));
		case "JimpleLocal":
			if (data.getExp(v) != null) {
				return data.getExp(v);
			} else {
				// after this call, if we reach constants, must abstract them if we are in loops
				// and the value in hand was local to the loop
				List<Value> locals = data.getLoopLocals(data.getLoopNo(callerU));
				return valueToExpression((locals != null && locals.contains(v)), loopNo, tp, data.getDefinedAt(v),
						((GAssignStmt) data.getDefinedAt(v)).getRightOp());
			}
		case "IntConstant":
			IntConstant ic = (IntConstant) v;
			// if the value is going to be used outside of loops
			if (loopNo == -1 || !shouldAbsConsts)
				return new ConstValExp(ic.value);
			else
				break;

		case "LongConstant":
			LongConstant lc = (LongConstant) v;
			return new ConstValExp(lc.value);
		case "StringConstant":
			StringConstant sc = (StringConstant) v;
			return new ConstValExp(sc.value);
		case "GInterfaceInvokeExpr":
			GInterfaceInvokeExpr iie = (GInterfaceInvokeExpr) v;
			String mName = iie.getMethod().getName();
			Expression result;
			if (mName.equals("getInt") || mName.equals("getString") || mName.equals("getLong")) {
				try {
					RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
					result = projectRow(rSet, iie.getArgs());
					data.addExp(new FakeJimpleLocal(rSet.getName() + "_proj", null, null), result);
				} catch (NullPointerException e) {
					// if (ConstantArgs.DEBUG_MODE) {
					// System.out.println("---- " + e + " in ValueToExpression.java: ");
					System.out.println(v);
					e.printStackTrace();
					// }
					break;
				}
				return result;
			} else if (mName.equals("next")) {
				NullExp rSet = new NullExp(iie.getBase());
				data.addExp(new FakeJimpleLocal(rSet.getName() + "_NotNull", null, null), rSet);
				return rSet;
			}

		default:
			break;
		}

		String resName = "Abs-" + tp + "#" + (data.absIter++);
		LOG.warn(v.getClass().getSimpleName() + " - Unhandled case - will abstract to: " + resName + "\n");
		Expression defResult = new UnknownExp(resName, -1);
		data.addExp(new FakeJimpleLocal(resName, null, null), defResult);
		return defResult;
	}

	/*
	 * given a rowVarExpression returns a new expression projecting a column
	 */
	private ProjVarExp projectRow(RowVarExp rVar, List<Value> args) throws ColumnDoesNotExist {
		assert (args.size() == 1) : "Case not handled : UnitHandler.java.projectRow";
		Value v = args.get(0);
		if (v.getType().toString().equals("java.lang.String"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(v.toString().replaceAll("\"", "")), rVar);
		else if (v.getType().toString().equals("int"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(Integer.parseInt(v.toString())), rVar);

		throw new ColumnDoesNotExist("");
	}

}
