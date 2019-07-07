package Z3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.Sort;

import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ar.Application;
import ar.Transaction;
import ar.expression.Expression;
import ar.expression.vars.RowSetVarExp;
import ar.ddl.Column;
import ar.ddl.Table;
import ar.statement.InvokeStmt;
import ar.statement.Query;
import ar.statement.Statement;
import cons.ConstantArgs;
import ar.statement.Query.Kind;

public class Rules {

	Context ctx;
	Application app;
	DeclaredObjects objs;
	Expr o1, o2, o3;
	Z3Util z3Util;
	ArrayList<Table> tables;

	public Rules(Context ctx, DeclaredObjects objs, Application app, ArrayList<Table> tables) {
		this.app = app;
		this.ctx = ctx;
		this.objs = objs;
		this.z3Util = new Z3Util(ctx, objs);
		this.tables = tables;
		o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// RW THEN
	//
	////////////////////////
	public List<BoolExpr> return_conditions_rw_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2,
			Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints shared beween cases:
				BoolExpr otypeCond1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otypeCond2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements
				if (q1.getTable().equals(q2.getTable()) && includedTables.contains(q1.getTable())) {
					String tableName = q1.getTable().getName();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					Table table = tables.stream().filter(t -> t.getName().equals(tableName)).findAny().get();
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
							o2.getPathCond());

					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ConstantArgs._current_version_enforcement
							? ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar)
							: ctx.mkTrue();
					//
					if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.UPDATE) {
						if (!Collections.disjoint(q1.getS_columns(), q2.getU_updates().keySet())) {
							BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
									q1.getWhClause());
							BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
									q2.getWhClause());
							BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar,
									vo2);
							BoolExpr rwOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + tableName), rowVar,
									vo1, vo2);
							// relate the updated velues to the projected values at the next version
							// ZZZ
							BoolExpr versionCond2 = ConstantArgs._current_version_enforcement
									? ctx.mkAnd(getVersionCondsRW(txn2, vt2, vo2, vo1, q2, rowVar))
									: ctx.mkTrue();
							Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
									versionCond2, pathCond1, pathCond2, aliveCond, rwOnTableCond);
							BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null,
									null);
							// ----
							/*
							 * Expr ivcO = ctx.mkFreshConst("o", objs.getSort("O")); FuncDecl wr_func =
							 * objs.getfuncs("WR_O_" + tableName); FuncDecl init_ver_func =
							 * objs.getfuncs(tableName + "_INITIAL_V"); FuncDecl ver_func =
							 * objs.getfuncs(tableName + "_VERSION"); Expr ivcBody = ctx.mkNot((BoolExpr)
							 * ctx.mkApp(wr_func, rowVar, ivcO, vo1)); Quantifier ivcLhs = ctx.mkForall(new
							 * Expr[] { rowVar, ivcO }, ivcBody, 1, null, null, null, null); BoolExpr ivcRhs
							 * = (BoolExpr) ctx.mkEq(ctx.mkApp(init_ver_func, rowVar), ctx.mkApp(ver_func,
							 * rowVar, vo1)); BoolExpr initialVersionCondition =
							 * (ConstantArgs._INSTANTIATE_NON_CYCLE_OPS &&
							 * ConstantArgs._current_version_enforcement) ? ctx.mkImplies(ivcLhs, ivcRhs) :
							 * ctx.mkTrue();
							 */
							result.add(ctx.mkAnd(rowExistsCond));
							//
						}
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.INSERT) {
						String lhsVarName = ((RowSetVarExp) q1.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr aliveCond = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn1.getName() + "_" + lhsVarName + "_isNull"), vo1);
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo2);
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
											q2.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, pathCond1,
								pathCond2, allInsertedRowCond, aliveCond, notNullCond, rwAliveOnTableCond);

						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);

						//
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.DELETE) {
						String lhsVarName = ((RowSetVarExp) q1.getsVar()).getName();
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn1.getName() + "_" + lhsVarName + "_isNull"), vo1);
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);

						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond, notNullCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.DELETE) {

						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr aliveCond2 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr aliveCond1 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						Expr body = ctx.mkAnd(otypeCond1, otypeCond2, whereClause1, whereClause2, pathCond1, pathCond2,
								aliveCond1, aliveCond2, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						if (ConstantArgs._DEP_ONLY_ON_READ_WRITES) //XXX
							result.add(ctx.mkEq(ctx.mkInt(1), ctx.mkInt(2)));
						else
							result.add(rowExistsCond);

					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.INSERT) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr aliveCond1 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo2);
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
											q2.getI_values().get(iter++)));
						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, pathCond1,
								pathCond2, aliveCond1, allInsertedRowCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						if (ConstantArgs._DEP_ONLY_ON_READ_WRITES)
							result.add(ctx.mkEq(ctx.mkInt(1), ctx.mkInt(2)));
						else
							result.add(rowExistsCond);
					} else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.INSERT) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr aliveCond1 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo2);
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
											q2.getI_values().get(iter++)));
						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, rowConflictCond, otypeCond1, otypeCond2, whereClause1,
								pathCond1, pathCond2, aliveCond1, allInsertedRowCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						if (ConstantArgs._DEP_ONLY_ON_READ_WRITES)
							result.add(ctx.mkEq(ctx.mkInt(1), ctx.mkInt(2)));
						else
							result.add(rowExistsCond);
					}
				}
			}
		return result;
	}

	private BoolExpr[] getVersionCondsRW(Transaction txn, Expr t, Expr o, Expr oldO, Query q, Expr rowVar)
			throws UnexoectedOrUnhandledConditionalExpression {
		Map<Column, Expression> updateFuncs = q.getU_updates();
		String tableName = q.getTable().getName();
		BoolExpr[] versionConds = new BoolExpr[updateFuncs.size() * 2];
		int iter96 = 0;
		for (Column c : updateFuncs.keySet()) {
			FuncDecl projFunc = objs.getfuncs(tableName + "_PROJ_" + c);
			FuncDecl verFunc = objs.getfuncs(tableName + "_VERSION");
			Expr lhsVal = ctx.mkApp(projFunc, rowVar, (ctx.mkApp(verFunc, rowVar, o)));
			switch (c.type) {
			case STRING:
				SeqExpr lhsValolderVersion = (SeqExpr) ctx.mkApp(projFunc, rowVar,
						(BitVecExpr) ctx.mkApp(verFunc, rowVar, oldO));
				versionConds[iter96++] = (ctx.mkNot(ctx.mkEq(lhsVal, lhsValolderVersion)));
				break;
			case INT:
				ArithExpr lhsValolderVersion1 = (ArithExpr) ctx.mkApp(projFunc, rowVar,
						(BitVecExpr) ctx.mkApp(verFunc, rowVar, oldO));
				versionConds[iter96++] = (ctx.mkNot(ctx.mkEq(lhsVal, lhsValolderVersion1)));
				break;
			case REAL:
				ArithExpr lhsValolderVersion2 = (ArithExpr) ctx.mkApp(projFunc, rowVar,
						(BitVecExpr) ctx.mkApp(verFunc, rowVar, oldO));
				versionConds[iter96++] = (ctx.mkNot(ctx.mkEq(lhsVal, lhsValolderVersion2)));
				break;
			default:
				System.out.println("----- case not handled yet: " + c.type);
			}

			Expression rhsVal = updateFuncs.get(c);
			versionConds[iter96++] = (ctx.mkEq(z3Util.irCondToZ3Expr(txn.getName(), t, rowVar, oldO, rhsVal), lhsVal));

		}

		return versionConds;
	}

	public BoolExpr[] getVersionCondsWR(Transaction txn, Expr t, Expr o, Query q, Expr rowVar)
			throws UnexoectedOrUnhandledConditionalExpression {
		Map<Column, Expression> updateFuncs = q.getU_updates();
		String tableName = q.getTable().getName();
		BoolExpr[] versionConds = new BoolExpr[updateFuncs.size() + 1];
		int iter96 = 0;
		FuncDecl verFunc = objs.getfuncs(tableName + "_VERSION");
		for (Column c : updateFuncs.keySet()) {
			FuncDecl projFunc = objs.getfuncs(tableName + "_PROJ_" + c);
			Expr lhsVal = ctx.mkApp(projFunc, rowVar, (ctx.mkApp(verFunc, rowVar, o)));
			Expression rhsVal = updateFuncs.get(c);
			try {
				versionConds[iter96++] = (ctx.mkEq(z3Util.irCondToZ3Expr(txn.getName(), t, rowVar, o, rhsVal), lhsVal));
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("rhsVal:" + rhsVal);
				System.out.println("tr(rhsVal):" + z3Util.irCondToZ3Expr(txn.getName(), t, rowVar, o, rhsVal));
				System.out.println("lhsVal:" + lhsVal);
			}
		}
		// last condition enforcing the version to be >0
		versionConds[iter96++] = ctx.mkBVSGT((BitVecExpr) ctx.mkApp(verFunc, rowVar, o),
				ctx.mkBV(0, ConstantArgs._MAX_BV_));
		return versionConds;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// WR THEN
	//
	////////////////////////
	public List<BoolExpr> return_conditions_wr_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2,
			Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		// these have to be removed
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints sharet beween cases:
				BoolExpr otypeCond1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otypeCond2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements which is
				// included at the current iteration
				if (q1.getTable().equals(q2.getTable()) && includedTables.contains(q1.getTable())) {
					String tableName = q1.getTable().getName();
					Table table = tables.stream().filter(t -> t.getName().equals(tableName)).findAny().get();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
							o2.getPathCond());
					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar);
					//
					if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.SELECT) {
						String lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr wrOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O_" + tableName), rowVar, vo1,
								vo2);
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);

						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName + "_isNull"), vo2);
						// ZZZ
						BoolExpr versionCond1 = ConstantArgs._current_version_enforcement
								? ctx.mkAnd(getVersionCondsWR(txn1, vt1, vo1, q1, rowVar))
								: ctx.mkTrue();

						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								versionCond1, pathCond1, pathCond2, aliveCond, notNullCond, wrOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.SELECT) {
						String lhsVarName = "";
						try {
							lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						} catch (Exception e) {
							System.out.println("q2:" + q2);
							System.out.println("q2.getsVar():" + q2.getsVar());
							System.out.println();
							System.out.println();
							System.out.println(e);
						}
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr notNullCond = ctx.mkNot((BoolExpr) ctx
								.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName + "_isNull"), vo2));
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns()) {

							Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo1);
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
											q1.getI_values().get(iter++)));

						}

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause2, pathCond1,
								pathCond2, allInsertedRowCond, aliveCond, notNullCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.SELECT) {
						String lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2));

						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName + "_isNull"), vo2);

						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);

						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond1, aliveCond2, notNullCond, wrAliveOnTableCond);

						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
					//
					else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2));

						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond1, aliveCond2, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo1);
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
											q1.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause2, pathCond1,
								pathCond2, allInsertedRowCond, aliveCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.DELETE) {
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						Expr version = ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), rowVar, vo1);
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar, version),
									z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
											q1.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause2, pathCond1,
								pathCond2, allInsertedRowCond, aliveCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
				}
			}
		return result;
	}

	public List<BoolExpr> return_conditions_ww_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2,
			Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints shared beween cases:
				BoolExpr otypeCond1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otypeCond2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements
				if (q1.getTable().equals(q2.getTable()) && includedTables.contains(q1.getTable())) {
					String tableName = q1.getTable().getName();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
							o2.getPathCond());
					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar);
					//
					if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, vo1,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar, vo2,
								q2.getWhClause());
						BoolExpr wwOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O_" + tableName), rowVar, vo1,
								vo2);
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond1, aliveCond2, wwOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
				}
			}
		return result;
	}
}
