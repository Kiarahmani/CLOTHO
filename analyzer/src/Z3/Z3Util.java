package Z3;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ar.expression.*;
import ar.expression.vars.*;
import cons.ConstantArgs;
import ar.expression.vals.*;
import ar.expression.Expression;
import ar.expression.UnOpExp.UnOp;

public class Z3Util {
	Context ctx;
	DeclaredObjects objs;
	private int loopCount = 0;
	private int absIntCount = 0;
	private int absRealCount = 0;
	private int absStringCount = 0;
	private int absBoolCount = 0;

	public Z3Util(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
	}

	public Expr irCondToZ3Expr(String txnName, Expr txn, Expr row, Expr o1, Expression cond)
			throws UnexoectedOrUnhandledConditionalExpression {

		switch (cond.getClass().getSimpleName()) {
		case "ConstValExp":
			ConstValExp cve = (ConstValExp) cond; {
			switch (cve.getType()) {
			case INT:
				return ctx.mkInt(cve.getIVal());
			case BOOLEAN:
				return ctx.mkBool(cve.isBVal());
			case REAL:
				return ctx.mkReal((int) cve.getDVal());
			case STRING:
				return ctx.MkString(cve.getSVal());
			}

		}
		case "FieldAccessValExp":
			break;
		case "NullExp":
			return ctx.mkApp(objs.getfuncs("abs_integer"), ctx.mkInt(absIntCount++));
		case "ParamValExp":
			ParamValExp pave = (ParamValExp) cond;
			return ctx.mkApp(objs.getfuncs(txnName + "_PARAM_" + pave.getName()), txn);
		case "ProjValExp":
			ProjValExp pve = (ProjValExp) cond;
			return ctx.mkApp(objs.getfuncs(pve.table.getName() + "_PROJ_" + pve.column.name), row,
					ctx.mkApp(objs.getfuncs(pve.table.getName() + "_VERSION"), row, o1));
		case "PrimitiveVarExp":
			break;
		case "ProjVarExp":
			ProjVarExp pv = (ProjVarExp) cond;
			FuncDecl projFunc = objs.getfuncs(pv.getRVar().getTable().getName() + "_PROJ_" + pv.getColumn().toString());
			Expr rowVar = irCondToZ3Expr(txnName, txn, row, o1, pv.getRVar());
			return ctx.mkApp(projFunc, irCondToZ3Expr(txnName, txn, row, o1, pv.getRVar()),
					ctx.mkApp(objs.getfuncs(pv.getRVar().getTable().getName() + "_VERSION"), rowVar, o1));

		case "RowSetVarExp":
			break;
		case "RowVarExp":
			RowVarExp ve = (RowVarExp) cond;
			FuncDecl rowFunc = objs.getfuncs(txnName + "_" + ve.getName());
			return ctx.mkApp(rowFunc, txn);
		case "RowVarLoopExp":
			RowVarLoopExp vle = (RowVarLoopExp) cond;
			FuncDecl loopVarFunc = objs.getfuncs(txnName + "_" + vle.getName());
			if (false) //XXX
				loopCount++;
			return ctx.mkApp(loopVarFunc, txn, ctx.mkBV(loopCount, ConstantArgs._MAX_BV_));
		case "UnknownExp":
			UnknownExp ue = (UnknownExp) cond;
			return ctx.mkApp(objs.getfuncs("abs_integer"), ctx.mkInt(absIntCount++));

		case "BinOpExp":
			BinOpExp boe = (BinOpExp) cond; {
			switch (boe.op) {
			case EQ:
				return ctx.mkEq(irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case PLUS:
				return ctx.mkAdd((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case MINUS:
				return ctx.mkSub((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case MULT:
				return ctx.mkMul((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case DIV:
				return ctx.mkDiv((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case AND:
				return ctx.mkAnd((BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case OR:
				return ctx.mkOr((BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case XOR:
				return ctx.mkXor((BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case GEQ:
				return ctx.mkGe((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case LEQ:
				return ctx.mkLe((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case LT:
				return ctx.mkLt((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			case GT:
				return ctx.mkGt((ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, o1, boe.e2));
			default:
				break;
			}
		}
			break;
		case "UnOpExp":
			UnOpExp uoe = (UnOpExp) cond;
			if (uoe.op == (UnOp.NOT))
				return ctx.mkNot((BoolExpr) irCondToZ3Expr(txnName, txn, row, o1, uoe.e));
			else
				break;
		}
		throw new UnexoectedOrUnhandledConditionalExpression(
				"--irCondToZ3Expr case not handled yet: " + cond.getClass().getName());
	}
}
