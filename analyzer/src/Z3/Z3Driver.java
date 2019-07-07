// this function will communicate with Z3 according to the counter-example
// generation settings and will produce and return a concrete anomaly which will
// be used for user information or execution path generations
package Z3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.microsoft.z3.*;
import anomaly.Anomaly;
import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ar.Application;
import ar.Transaction;
import ar.expression.Expression;
import ar.expression.vals.*;
import ar.expression.vars.*;
import ar.ddl.Column;
import ar.ddl.Table;
import ar.statement.InvokeStmt;
import ar.statement.Statement;
import cons.ConstantArgs;
import soot.Value;
import utils.Tuple;
import Z3.initialConstraints.InitialAssertions;

public class Z3Driver {
	int _MAX_VERSIONS = 2;
	int _MAX_STRINGS = 1;
	Application app;
	ArrayList<Table> tables;
	Context ctx;
	Solver slv;
	Model model;
	DeclaredObjects objs;
	StaticAssertions staticAssrtions;
	InitialAssertions initialAssrtions;
	DynamicAssertsions dynamicAssertions;
	Rules ruleGenerator;
	// a log file containing all assertions and defs for debugging
	File file;
	FileWriter writer;
	PrintWriter printer;
	Expr vo1, vo2, vt1, vt2;
	private boolean findCore;
	int globalIter = 1;

	// constructor
	public Z3Driver(Application app, ArrayList<Table> tables, boolean findCore) {
		this.findCore = findCore;
		this.app = app;
		this.tables = tables;
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		cfg.put("unsat_core", "true");
		ctx = new Context(cfg);
		slv = ctx.mkSolver();
		if (!findCore)
			this.file = new File("z3-encoding.smt2");
		else
			this.file = new File("z3-encoding-core.smt2");
		model = null;
		try {
			writer = new FileWriter(file, false);
			printer = new PrintWriter(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.objs = new DeclaredObjects(printer);
		ctxInitializeSorts();
		this.staticAssrtions = new StaticAssertions(ctx, objs);
		//this.initialAssrtions = new InitialAssertions(ctx, objs);
		this.dynamicAssertions = new DynamicAssertsions(ctx, objs, this.app);
		this.ruleGenerator = new Rules(ctx, objs, this.app, this.tables);

		// to be used in the rules
		vo1 = ctx.mkFreshConst("o", objs.getSort("O"));
		vo2 = ctx.mkFreshConst("o", objs.getSort("O"));
		vt1 = ctx.mkFreshConst("t", objs.getSort("T"));
		vt2 = ctx.mkFreshConst("t", objs.getSort("T"));

	}

	private void HeaderZ3(String s) {
		if (ConstantArgs._LOG_ASSERTIONS) {
			int line_length = 110;
			int white_space_length = (line_length - s.length()) / 2;
			String line = ";" + String.format("%0" + line_length + "d", 0).replace("0", "-");
			String white_space = String.format("%0" + white_space_length + "d", 0).replace("0", " ");
			LogZ3("\n" + line);
			LogZ3(";" + white_space + s);
			LogZ3(line);
			printer.flush();
		}
	}

	private void SubHeaderZ3(String s) {
		if (ConstantArgs._LOG_ASSERTIONS) {
			LogZ3("\n;" + s.toUpperCase());
			printer.flush();
		}
	}

	private void LogZ3(String s) {
		if (ConstantArgs._LOG_ASSERTIONS) {
			printer.append(s + "\n");
			printer.flush();
		}
	}

	/*
	 * 
	 * adds the constant assertions and defs to the context
	 */
	private void ctxInitializeSorts() {
		HeaderZ3("SORTS & DATATYPES");
		objs.addSort("T", ctx.mkUninterpretedSort("T"));
		objs.addSort("O", ctx.mkUninterpretedSort("O"));
		objs.addSort("Bool", ctx.mkBoolSort());
		objs.addSort("Int", ctx.mkIntSort());
		objs.addSort("BitVec", ctx.mkBitVecSort(ConstantArgs._MAX_BV_));
		objs.addSort("LoopBitVec", ctx.mkBitVecSort(ConstantArgs._MAX_BV_));
		objs.addSort("RowBitVec", ctx.mkBitVecSort(ConstantArgs._MAX_BV_));
		objs.addSort("String", ctx.mkStringSort());
		objs.addSort("Real", ctx.mkRealSort());

		// create table sorts and constraints
		for (Table t : tables) {
			objs.addSort(t.getName(), ctx.mkUninterpretedSort(t.getName()));
		}
	}

	@SuppressWarnings("unused")
	private void ctxInitialize(Anomaly unVersionedAnml) {

		LogZ3(";data types");
		objs.addDataType("TType", mkDataType("TType", app.getAllTxnNames()));
		objs.addDataType("OType", mkDataType("OType", app.getAllStmtTypes()));

		// =====================================================================================================================================================
		HeaderZ3("STATIC FUNCTIONS & PROPS");
		SubHeaderZ3(";declarations");

		objs.addFunc("abs_integer", ctx.mkFuncDecl("abs_integer", objs.getSort("Int"), objs.getSort("Int")));
		objs.addFunc("abs_boolean", ctx.mkFuncDecl("abs_boolean", objs.getSort("Int"), objs.getSort("Bool")));
		objs.addFunc("abs_real", ctx.mkFuncDecl("abs_real", objs.getSort("Int"), objs.getSort("Real")));
		objs.addFunc("abs_string", ctx.mkFuncDecl("abs_string", objs.getSort("Int"), objs.getSort("String")));
		objs.addFunc("otime", ctx.mkFuncDecl("otime", objs.getSort("O"), objs.getSort("Int")));
		objs.addFunc("opart", ctx.mkFuncDecl("opart", objs.getSort("O"), objs.getSort("Int")));
		objs.addFunc("ttype", ctx.mkFuncDecl("ttype", objs.getSort("T"), objs.getDataTypes("TType")));
		objs.addFunc("otype", ctx.mkFuncDecl("otype", objs.getSort("O"), objs.getDataTypes("OType")));
		objs.addFunc("is_update", ctx.mkFuncDecl("is_update", objs.getSort("O"), objs.getSort("Bool")));
		objs.addFunc("parent", ctx.mkFuncDecl("parent", objs.getSort("O"), objs.getSort("T")));
		objs.addFunc("sibling",
				ctx.mkFuncDecl("sibling", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("WR_O",
				ctx.mkFuncDecl("WR_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("RW_O",
				ctx.mkFuncDecl("RW_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("WW_O",
				ctx.mkFuncDecl("WW_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("vis",
				ctx.mkFuncDecl("vis", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("ar",
				ctx.mkFuncDecl("ar", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		// Dependency Graph Relations
		objs.addFunc("D",
				ctx.mkFuncDecl("D", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("X",
				ctx.mkFuncDecl("X", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));

		HeaderZ3("properties");
		// assertions
		addAssertion("par_then_sib", staticAssrtions.mk_par_then_sib());
		addAssertion("sib_then_par", staticAssrtions.mk_sib_then_par());
		addAssertion("ar_on_writes", staticAssrtions.mk_ar_on_writes());
		addAssertion("vis_on_writes", staticAssrtions.mk_vis_on_writes());
		addAssertion("vis_then_ar", staticAssrtions.mk_vis_then_ar());
		addAssertion("types_then_eq", staticAssrtions.mk_types_then_eq());
		addAssertion("no_loops_o", staticAssrtions.mk_no_loops_o());
		addAssertion("trans_ar", staticAssrtions.mk_trans_ar());
		addAssertion("total_ar", staticAssrtions.mk_total_ar());
		addAssertion("wr_then_vis", staticAssrtions.mk_wr_then_vis());
		addAssertion("ww_then_ar", staticAssrtions.mk_ww_then_ar());
		addAssertion("rw_then_not_vis", staticAssrtions.mk_rw_then_not_vis());
		addAssertion("irreflx_ar", staticAssrtions.mk_irreflx_ar());
		addAssertion("otime_props", staticAssrtions.mk_otime_props());
		addAssertion("opart_props", staticAssrtions.mk_opart_props());
		SubHeaderZ3("anomaly shaping");
		// change the shape of the anomaly according to the user-given configuration
		if (ConstantArgs._NO_WW)
			addAssertion("no_ww", staticAssrtions.mk_no_ww());
		if (ConstantArgs._NO_WR)
			addAssertion("no_wr", staticAssrtions.mk_no_wr());
		if (ConstantArgs._NO_RW)
			addAssertion("no_rw", staticAssrtions.mk_no_rw());
		if (ConstantArgs._MAX_TXN_INSTANCES != -1) {
			if (ConstantArgs._MAX_TXN_INSTANCES == -2)
				addAssertion("limit_txn_instances",
						dynamicAssertions.mk_limit_txn_instances(ConstantArgs._Current_Cycle_Length - 1));
			else
				addAssertion("limit_txn_instances",
						dynamicAssertions.mk_limit_txn_instances(ConstantArgs._MAX_TXN_INSTANCES));
		}

		// =====================================================================================================================================================
		HeaderZ3("DYNAMIC FUNCTIONS & PROPS");
		addAssertion("oType_to_is_update", dynamicAssertions.mk_oType_to_is_update(app.getAllUpdateStmtTypes()));
		addAssertion("is_update_to_oType", dynamicAssertions.mk_is_update_to_oType(app.getAllUpdateStmtTypes()));

		// relating operation otypes to parent ttypes
		for (Transaction txn : app.getTxns()) {
			String name = txn.getName();
			for (String stmtName : txn.getStmtNames())
				addAssertion("op_types_" + name + "_" + stmtName,
						dynamicAssertions.op_types_to_parent_type(name, stmtName));
		}
		// make sure the otime assignment follows the program order
		for (Transaction txn : app.getTxns()) {
			Map<Integer, String> map = txn.getStmtNamesMap();
			for (int j = 1; j < map.size(); j++)
				for (int i = j; i <= map.size(); i++) {
					if (map.get(i + 1) != null) {
						addAssertion("otime_follows_po_" + i + "_" + j + map.get(i),
								dynamicAssertions.otime_follows_po(map.get(j), map.get(i + 1)));
					}
				}
		}

		// =====================================================================================================================================================
		HeaderZ3("CONFLICTING ROWS");
		for (Transaction txn1 : app.getTxns()) {
			Sort oSort = objs.getSort("O");
			for (Transaction txn2 : app.getTxns()) {
				for (Statement o1 : txn1.getStmts())
					for (Statement o2 : txn2.getStmts()) {
						InvokeStmt io1 = (InvokeStmt) o1;
						InvokeStmt io2 = (InvokeStmt) o2;
						String tableName = io1.getQuery().getTable().getName();
						objs.addFunc(io1.getType().toString() + "_" + io2.getType().toString() + "_conflict_rows",
								ctx.mkFuncDecl(
										io1.getType().toString() + "_" + io2.getType().toString() + "_conflict_rows",
										new Sort[] { oSort, oSort }, objs.getSort(tableName)));

					}
			}
		}

		HeaderZ3("TABLE FUNCTIONS & PROPS");
		// create table sorts and constraints
		for (Table t : tables) {
			SubHeaderZ3(t.getName());
			LogZ3(";");
			Sort tSort = objs.getSort(t.getName());
			Sort oSort = objs.getSort("O");
			objs.addFunc(t.getName() + "_VERSION",
					ctx.mkFuncDecl(t.getName() + "_VERSION", new Sort[] { tSort, oSort }, objs.getSort("BitVec")));
			// objs.addFunc(t.getName() + "_INITIAL_V",
			// ctx.mkFuncDecl(t.getName() + "_INITIAL_V", new Sort[] { tSort },
			// objs.getSort("BitVec")));
			for (Column c : t.getColumns())
				objs.addFunc(t.getName() + "_PROJ_" + c.getName(), ctx.mkFuncDecl(t.getName() + "_PROJ_" + c.getName(),
						new Sort[] { tSort, objs.getSort("BitVec") }, objs.getSort(c.getType().toZ3String())));

			addAssertion("pk_" + t.getName(), dynamicAssertions.mk_pk_tables(t));
			addAssertion("pk_" + t.getName() + "_identical", dynamicAssertions.mk_pk_tables_identical(t));
			LogZ3(";functions");
			// dependecy relations on operations
			objs.addFunc("IsAlive_" + t.getName(),
					ctx.mkFuncDecl("IsAlive_" + t.getName(), new Sort[] { tSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("RW_O_" + t.getName(),
					ctx.mkFuncDecl("RW_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WR_O_" + t.getName(),
					ctx.mkFuncDecl("WR_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WW_O_" + t.getName(),
					ctx.mkFuncDecl("WW_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("RW_Alive_" + t.getName(), ctx.mkFuncDecl("RW_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WR_Alive_" + t.getName(), ctx.mkFuncDecl("WR_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WW_Alive_" + t.getName(), ctx.mkFuncDecl("WW_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));

			// addAssertion(t.getName() + "_no_incom_edge_then_init_ver",
			// dynamicAssertions._mk_no_incom_edge_then_init_ver(t.getName()));
			// addAssertion(t.getName() + "_no_incom_edge_then_NOT_init_ver",
			// dynamicAssertions._mk_no_incom_edge_then_NOT_init_ver(t.getName()));
			addAssertion(t.getName() + "_RW_TABLE_then_RW", dynamicAssertions.mk_rw_then_deps(t.getName()));
			addAssertion(t.getName() + "_WR_TABLE_then_WR", dynamicAssertions.mk_wr_then_deps(t.getName()));
			addAssertion(t.getName() + "_WW_TABLE_then_WW", dynamicAssertions.mk_ww_then_deps(t.getName()));
			addAssertion(t.getName() + "_LWW", dynamicAssertions.mk_lww(t.getName()));

		}
		// =====================================================================================================================================================

		HeaderZ3("Initial Assertions");
		// initial assertions
		//addAssertion("all_logs_greater_than_zero", initialAssrtions.mk_all_logs_greater_than_zero());

		// =====================================================================================================================================================

		for (Transaction txn : app.getTxns()) {
			HeaderZ3("TXN: " + txn.getName().toUpperCase());
			// declare functions for txn's input parameters
			SubHeaderZ3("parameters");
			for (ParamValExp p : txn.getParams().values()) {
				String label = txn.getName() + "_PARAM_" + p.getName();
				objs.addFunc(label, ctx.mkFuncDecl(label, new Sort[] { objs.getSort("T") },
						objs.getSort(p.getType().toZ3String())));
			}
			SubHeaderZ3("?");
			// define lhs assignees [[XXX not sure what this does -> might be a legacy
			// feature]]
			for (VarExp ve : txn.getAllLhsVars()) {
				String label = txn.getName() + "_" + ve.getName();
				for (AST f : dynamicAssertions.mk_declare_lhs(label, ve)) {
					objs.addFunc(label, (FuncDecl) f);
					// if there is more than one, the second function is isNull
					label += "_isNull";
				}
				// assertion on existence of a record when not Null
				label = txn.getName() + "_" + ve.getName();
				BoolExpr isNullProp = dynamicAssertions.mk_assert_is_null(label, ve);
				if (isNullProp != null)
					addAssertion(label + "_isNull_prop", isNullProp);
			}

			// reorder the definitions to make sure the dependencies are satisfied when
			// defining new vars
			LinkedHashMap<Value, Expression> sortedMap = new LinkedHashMap<Value, Expression>();
			Map<Value, Expression> orgMap = txn.getAllExps();
			LinkedBlockingQueue<Value> unAddedEntries = new LinkedBlockingQueue<>();

			for (Value v : orgMap.keySet())
				unAddedEntries.add(v);

			for (Value v : orgMap.keySet())
				if (orgMap.get(v).getClass().getSimpleName().equals("RowSetVarExp")) {
					sortedMap.put(v, orgMap.get(v));
					unAddedEntries.remove(v);
					for (Value v1 : orgMap.keySet())
						if (v1.toString().contains(v.toString() + "-")) {
							sortedMap.put(v1, orgMap.get(v1));
							unAddedEntries.remove(v);
						}
				}

			for (Value v : unAddedEntries)
				sortedMap.put(v, orgMap.get(v));

			if (!sortedMap.isEmpty())
				SubHeaderZ3("Expressions");
			// add expressions for each trn
			for (Value val : sortedMap.keySet()) {
				Expression exp = txn.getAllExps().get(val);
				String label = txn.getName() + "_" + val.toString();
				Sort tSort = objs.getSort("T");
				Sort oSort = objs.getSort("O");

				switch (exp.getClass().getSimpleName()) {
				case "RowSetVarExp":
					this.SubHeaderZ3(val.toString());
					// declare SVar
					RowSetVarExp rsv = (RowSetVarExp) exp;
					String table = rsv.getTable().getName();
					objs.addFunc(label,
							ctx.mkFuncDecl(label, new Sort[] { tSort, objs.getSort(table) }, objs.getSort("Bool")));
					objs.addFunc(label + "_isNull",
							ctx.mkFuncDecl(label + "_isNull", new Sort[] { oSort }, objs.getSort("Bool")));
					// add props for SVar
					BoolExpr prop = dynamicAssertions.mk_svar_props(txn.getName(), val.toString(), table,
							rsv.getWhClause());
					addAssertion(label + "_props", prop);

					break;
				case "RowVarExp":
					RowVarExp rv = (RowVarExp) exp;
					String tableName = rv.getTable().getName();
					RowSetVarExp setVar = rv.getSetVar();
					// declare rowVar
					objs.addFunc(label, ctx.mkFuncDecl(label, new Sort[] { tSort }, objs.getSort(tableName)));
					// add props for rowVar
					prop = dynamicAssertions.mk_row_var_props(txn.getName(), val.toString(), setVar);
					addAssertion(label + "_props", prop);

					break;
				case "RowVarLoopExp":
					// thse vars have an extra INT argument
					RowVarLoopExp vle = (RowVarLoopExp) exp;
					tableName = vle.getTable().getName();
					setVar = vle.getSetVar();
					objs.addFunc(label, ctx.mkFuncDecl(label, new Sort[] { tSort, objs.getSort("BitVec") },
							objs.getSort(tableName)));
					// add props for loopVar
					prop = dynamicAssertions.mk_row_var_loop_props(txn.getName(), val.toString(), setVar);
					addAssertion(label + "_props", prop);

				case "ParamValExp":
					break;
				default:
					break;
				}

			}
		}
	}

	private void addAssertion(String name, BoolExpr ass) {
		LogZ3(";" + name);
		objs.addAssertion(name, ass);
		slv.add(ass);
		// System.out.println("add#: " + this.globalIter++ + ": " + name);
	}

	/*
	 */
	private DatatypeSort mkDataType(String name, String[] consts) {
		Symbol[] head_tail = new Symbol[] {};
		Sort[] sorts = new Sort[] {};
		int[] sort_refs = new int[] {};
		Constructor[] constructors = new Constructor[consts.length];
		for (int i = 0; i < consts.length; i++)
			constructors[i] = ctx.mkConstructor(ctx.mkSymbol(consts[i]), ctx.mkSymbol("is_" + consts[i]), head_tail,
					sorts, sort_refs);
		try {
			DatatypeSort result = ctx.mkDatatypeSort(name, constructors);
			return result;
		} catch (com.microsoft.z3.Z3Exception e) {
			throw new com.microsoft.z3.Z3Exception("No Txn with SQL operations found");
		}
	}

	public void closeCtx() {
		this.ctx.close();
	}

	/*
	 * final call to Z3 when the context is completely done
	 */
	private Anomaly checkSAT() {
		if (slv.check() == Status.SATISFIABLE) {
			model = slv.getModel();
			return new Anomaly(model, ctx, objs, tables, app, findCore);
		} else {
			// System.err.println("Failed to generate a counter example +++ bound: " +
			// ConstantArgs._DEP_CYCLE_LENGTH);
			//System.out.println("--------");
			//System.out.println("--------\n-- UNSAT");
			//System.out.println("--------");
			//System.out.println("--------");
			for (Expr e : slv.getUnsatCore())
				System.out.println(e);
			return null;
		}
	}

	//
	//
	//
	// ---------------------------------------------------------------------------
	// functions adding assertions for every pair of operations that 'potentially'
	// create the edge
	private void RWthen(Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {

		Map<String, FuncDecl> Ts = objs.getAllTTypes();
		for (FuncDecl t1 : Ts.values())
			for (FuncDecl t2 : Ts.values()) {
				List<BoolExpr> conditions = ruleGenerator.return_conditions_rw_then(t1, t2, vo1, vo2, vt1, vt2,
						includedTables);
				conditions.add(ctx.mkFalse());
				// conditions.add(ctx.mkTrue());
				BoolExpr rhs = ctx.mkOr(conditions.toArray(new BoolExpr[conditions.size()]));
				BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo1), vt1);
				BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo2), vt2);
				BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt1),
						ctx.mkApp(objs.getConstructor("TType", t1.getName().toString())));
				BoolExpr lhs4 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt2),
						ctx.mkApp(objs.getConstructor("TType", t2.getName().toString())));
				BoolExpr lhs5 = ctx.mkNot(ctx.mkEq(vo1, vo2));
				BoolExpr lhs6 = ctx.mkNot(ctx.mkEq(vt1, vt2));
				BoolExpr lhs7 = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), vo1, vo2);

				BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3, lhs4, lhs5, lhs6, lhs7);
				BoolExpr body = ctx.mkImplies(lhs, rhs);
				Quantifier rw_then = ctx.mkForall(new Expr[] { vo1, vo2, vt1, vt2 }, body, 1, null, null, null, null);
				String rule_name = t1.getName().toString() + "-" + t2.getName().toString() + "-rw-then";
				addAssertion(rule_name, rw_then);
			}

	}

	private void WRthen(Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {
		Map<String, FuncDecl> Ts = objs.getAllTTypes();
		for (FuncDecl t1 : Ts.values())
			for (FuncDecl t2 : Ts.values()) {
				List<BoolExpr> conditions = ruleGenerator.return_conditions_wr_then(t1, t2, vo1, vo2, vt1, vt2,
						includedTables);
				conditions.add(ctx.mkFalse());
				BoolExpr rhs = ctx.mkOr(conditions.toArray(new BoolExpr[conditions.size()]));
				BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo1), vt1);
				BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo2), vt2);
				BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt1),
						ctx.mkApp(objs.getConstructor("TType", t1.getName().toString())));
				BoolExpr lhs4 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt2),
						ctx.mkApp(objs.getConstructor("TType", t2.getName().toString())));
				BoolExpr lhs5 = ctx.mkNot(ctx.mkEq(vo1, vo2));
				BoolExpr lhs6 = ctx.mkNot(ctx.mkEq(vt1, vt2));
				BoolExpr lhs7 = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), vo1, vo2);
				BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3, lhs4, lhs5, lhs6, lhs7);
				BoolExpr body = ctx.mkImplies(lhs, rhs);
				Quantifier rw_then = ctx.mkForall(new Expr[] { vo1, vo2, vt1, vt2 }, body, 1, null, null, null, null);
				String rule_name = t1.getName().toString() + "-" + t2.getName().toString() + "-wr-then";
				addAssertion(rule_name, rw_then);
			}
	}

	private void WWthen(Set<Table> includedTables) throws UnexoectedOrUnhandledConditionalExpression {
		Map<String, FuncDecl> Ts = objs.getAllTTypes();
		for (FuncDecl t1 : Ts.values())
			for (FuncDecl t2 : Ts.values()) {
				List<BoolExpr> conditions = ruleGenerator.return_conditions_ww_then(t1, t2, vo1, vo2, vt1, vt2,
						includedTables);
				conditions.add(ctx.mkFalse());
				BoolExpr rhs = ctx.mkOr(conditions.toArray(new BoolExpr[conditions.size()]));
				BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo1), vt1);
				BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo2), vt2);
				BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt1),
						ctx.mkApp(objs.getConstructor("TType", t1.getName().toString())));
				BoolExpr lhs4 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt2),
						ctx.mkApp(objs.getConstructor("TType", t2.getName().toString())));
				BoolExpr lhs5 = ctx.mkNot(ctx.mkEq(vo1, vo2));
				BoolExpr lhs6 = ctx.mkNot(ctx.mkEq(vt1, vt2));
				BoolExpr lhs7 = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), vo1, vo2);
				BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3, lhs4, lhs5, lhs6, lhs7);
				BoolExpr body = ctx.mkImplies(lhs, rhs);
				Quantifier ww_then = ctx.mkForall(new Expr[] { vo1, vo2, vt1, vt2 }, body, 1, null, null, null, null);
				String rule_name = t1.getName().toString() + "-" + t2.getName().toString() + "-ww-then";
				addAssertion(rule_name, ww_then);
			}

	}

	private void thenWR(Set<Table> includedTables) {
		// TODO Auto-generated method stub

	}

	private void thenWW(Set<Table> includedTables) {
		// TODO Auto-generated method stub

	}

	/*
	 * public function called from main
	 */
	public Anomaly analyze(int round, List<List<Tuple<String, Tuple<String, String>>>> seenStructures,
			List<Anomaly> seenAnmls, Set<Table> includedTables, Anomaly unVersionedAnml) {
		// this function is called twice at each iteration: once for unannotated
		// solution and once for the annotated (second call). In the second call certain
		// constraints (e.g. rule constraints) must be popped and be replaced with
		// annotated versions.
		switch (round) {
		case 1:
			ctxInitialize(unVersionedAnml);
			int iter530 = 0;
			for (Anomaly anml : seenAnmls)
				excludeAnomaly(anml, iter530++);
			for (List<Tuple<String, Tuple<String, String>>> strc : seenStructures) {
				excludeAnomalyFromStructure(strc, iter530++);

			}
			try {
				// rules
				slv.push();
				HeaderZ3(" ->WW ");
				thenWW(includedTables);
				HeaderZ3(" ->WR ");
				thenWR(includedTables);
				HeaderZ3(" WW-> ");
				WWthen(includedTables);
				HeaderZ3(" WR-> ");
				WRthen(includedTables);
				HeaderZ3(" RW-> ");
				RWthen(includedTables);
			} catch (UnexoectedOrUnhandledConditionalExpression e) {
				e.printStackTrace();
			}
			//
			HeaderZ3("CYCLE ASSERTIONS");
			// dependency assertions
			addAssertion("gen_dep", staticAssrtions.mk_gen_dep());
			// addAssertion("gen_dep_props", staticAssrtions.mk_gen_dep_props());
			addAssertion("gen_depx", staticAssrtions.mk_gen_depx());
			// addAssertion("gen_depx_props", staticAssrtions.mk_gen_depx_props());
			addAssertion("base_cycle_enforcement", dynamicAssertions.mk_cycle(findCore, null));
			HeaderZ3("EOF");
			break;

		case 2:
			HeaderZ3("VERSIONING PROPS");
			int iter = 0;
			// pop the old unversioned encodings
			slv.pop();
			// because the global variable _current_version_enforcement has changed, the
			// following rules will add versioned encodings
			try {
				HeaderZ3(" ->WW ");
				thenWW(includedTables);
				HeaderZ3(" ->WR ");
				thenWR(includedTables);
				HeaderZ3(" WW-> ");
				WWthen(includedTables);
				HeaderZ3(" WR-> ");
				WRthen(includedTables);
				HeaderZ3(" RW-> ");
				RWthen(includedTables);
			} catch (UnexoectedOrUnhandledConditionalExpression e) {
				e.printStackTrace();
			}

			for (BoolExpr ass : dynamicAssertions.mk_versioning_props(tables))
				addAssertion("versioning_props" + (iter++), ass);
			HeaderZ3("NEW CYCLE ASSERTIONS");
			// dependency assertions
			addAssertion("gen_dep", staticAssrtions.mk_gen_dep());
			// addAssertion("gen_dep_props", staticAssrtions.mk_gen_dep_props());
			addAssertion("gen_depx", staticAssrtions.mk_gen_depx());
			// addAssertion("gen_depx_props", staticAssrtions.mk_gen_depx_props());
			slv.push();
			addAssertion("exact_cycle_enforcement", dynamicAssertions.mk_cycle(findCore, unVersionedAnml));
			HeaderZ3("EOF");
			break;
		case 3:
			slv.pop();
			HeaderZ3("ROUND 3: newly pushed");
			List<Tuple<String, Tuple<String, String>>> structure3 = unVersionedAnml.getCycleStructure();
			addAssertion("loose_cycle_constraint", dynamicAssertions.mk_loose_cycle(findCore, structure3));
			excludeAnomaly(unVersionedAnml, seenAnmls.size() + 1);
			break;

		case 4:
			excludeAnomaly(unVersionedAnml, seenAnmls.size() + 1);
			break;

		default:
			break;
		}

		return checkSAT();
	}

	private void excludeAnomaly(Anomaly anml, int iter) {
		HeaderZ3("previous anomalies exclusion");
		List<Tuple<String, Tuple<String, String>>> structure = anml.getCycleStructure();
		addAssertion("exact_anomaly_exclusion_" + iter, dynamicAssertions.mk_previous_anomaly_exclusion(structure));
	}

	private void excludeAnomalyFromStructure(List<Tuple<String, Tuple<String, String>>> structure, int iter) {
		HeaderZ3("previous anomalies exclusion");
		addAssertion("exact_anomaly_exclusion_" + iter, dynamicAssertions.mk_previous_anomaly_exclusion(structure));
	}

}

//

//

//

//

//

//

//
