package anomaly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ar.Application;
import ar.Transaction;
import ar.ddl.Column;
import ar.ddl.Table;
import ar.statement.InvokeStmt;
import ar.statement.SqlStmtType;
import ar.statement.Statement;
import utils.Tuple;
import cons.ConstantArgs;
import Z3.DeclaredObjects;

public class scheduleGen {
	String text = "";
	Model model;
	Application app;
	List<Tuple<String, Tuple<String, String>>> cqlData = new ArrayList<>();
	ArrayList<Table> tables;
	DeclaredObjects objs;
	private Map<String, FuncDecl> allNextVars;
	Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	Context ctx;
	List<Expr> ts, os;

	public scheduleGen(Context ctx, Model model, DeclaredObjects objs, ArrayList<Table> tables,
			Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow, List<Expr> ts, List<Expr> os,
			Application app) {
		this.app = app;
		this.conflictingRow = conflictingRow;
		this.model = model;
		this.objs = objs;
		this.tables = tables;
		this.ctx = ctx;
		this.allNextVars = objs.getAllNextVars();
		this.ts = ts;
		this.os = os;
	}

	//
	/////////////////////
	public void createInstance(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		new PrintWriter(writer);
		PrintWriter printer = new PrintWriter(writer);
		String delim = "";
		printer.append("[");
		for (Expr model_txn : ts) {

			printer.append(delim);
			printer.append("\n{");
			String txn_id = String.valueOf(Integer.valueOf(model_txn.toString().replace("T!val!", "")) + 1);

			printer.append("\"" + txn_id + "\"");
			printer.append(":{\"methodName\":");
			String txn_type = (model.eval(objs.getfuncs("ttype").apply(model_txn), true).toString());
			printer.append("\"" + txn_type + "\"");
			printer.append(",\"args\":");
			printer.append("[");
			String inner_delim = "";
			Transaction app_txn = app.getTxnByName(txn_type);
			// iterate over args
			for (String app_param : app_txn.getParams().keySet()) {
				printer.append(inner_delim);
				String modelVal = (model.eval(
						ctx.mkApp(objs.getfuncs(app_txn.getName().toString() + "_PARAM_" + app_param), model_txn),
						true)).toString();
				printer.append(modelVal);
				inner_delim = ",";
			}
			printer.append("]");
			printer.append("}}");
			delim = ",";
		}
		printer.append("\n]");
		printer.flush();
	}

	//
	/////////////////////
	public void createSchedule(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		Map<Integer, String> result = new HashMap<>();
		Map<Integer, String> model_par = new HashMap<>();
		for (Expr o : os) {
			// put all operations in the model in the final result
			result.put(Integer.valueOf(model.eval(objs.getfuncs("otime").apply(o), true).toString()),
					model.eval(objs.getfuncs("otype").apply(o), true).toString());
			model_par.put(Integer.valueOf(model.eval(objs.getfuncs("otime").apply(o), true).toString()),
					model.eval(objs.getfuncs("parent").apply(o), true).toString());
		}

		// iterate over all transactions
		for (Transaction txn1 : app.getTxns()) {
			// iterate over all operations in each transaction
			for (Statement o1 : txn1.getStmts()) {
				SqlStmtType app_o = ((InvokeStmt) o1).getType();
				// search the model in the model and find matches
				for (Integer time : result.keySet()) {
					String model_o = result.get(time);
					if (model_o.contains(app_o.toString())) {
						// the corresponding op from the app is found
						// extract info
						String par_id = String
								.valueOf(Integer.valueOf(model_par.get(time).replaceAll("T!val!", "")) + 1);
						String op_kind = app_o.kind;
						String op_table = ((InvokeStmt) o1).getQuery().getTable().getName();
						String op_number = String.valueOf(app_o.getNumber());
						result.put(time, par_id + "," + op_kind + "," + op_table + "," + op_number);
					}
				}
			}
		}

		for (Integer time : result.keySet())
			printer.append(result.get(time) + "\n");
		// EOF marker
		printer.append("-99,\"\",\"\",-99");
		printer.flush();

	}

	// w
	/////////////////////
	public void createData(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		Expr[] allOs = model.getSortUniverse(objs.getSort("O"));
		/////////// ADD INSTANTIATED ROWS

		// System.out.println("\n\n\n\n\n\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		for (Table t : tables) {
			Expr[] allRs = model.getSortUniverse(objs.getSort(t.getName()));
			String delim = "";
			String columnNames = "(";
			for (Column column : t.getColumns()) {
				columnNames += (delim + column.name);
				delim = ",";
			}
			for (Expr r : allRs) {
				// System.out.println("-------" + r.toString().replaceAll("!val!", "#"));
				for (int i = 0; i < (Math.pow(2, ConstantArgs._MAX_BV_)); i++) {
					delim = "";
					String columnVals = "(";
					for (Column column : t.getColumns()) {
						// columnNames += (delim + column.name);
						FuncDecl projFunc = objs.getfuncs(t.getName() + "_PROJ_" + column.name);
						String val = model.eval(projFunc.apply(r, ctx.mkBV(i, ConstantArgs._MAX_BV_)), true).toString();
						columnVals += (delim + val);
						delim = ",";
					}
					if (i == 0) {
						printer.append("\nINSERT INTO testks." + t.getName().toUpperCase() + columnNames + ")"
								+ " VALUES" + columnVals + ")" + ";");
					}
					// System.out.println(header + columnVals + ")");
				}

			}
		}

		// System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		for (Expr o : allOs)
			for (String nextVar : allNextVars.keySet()) {
				FuncDecl varFunc = allNextVars.get(nextVar);
				FuncDecl parent = objs.getfuncs("parent");
				Expr t = model.eval(parent.apply(o), true);
				Expr row = model.eval(varFunc.apply(t), true);
				Table table = tables.stream().filter(tab -> row.toString().contains(tab.getName())).findAny().get();
				FuncDecl verFunc = objs.getfuncs(table.getName() + "_VERSION");
				model.eval(verFunc.apply(row, o), true);

				t.toString().replaceAll("!val!", "");
				o.toString().replaceAll("!val!", "");
				table.getName();

				// System.out.println(header + columnNames + "" + columnVals + "");
				// System.out.println();
			}
		// System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n\n\n\n\n\n\n");
		/////////// ADD CONFLICTING ROWS
		for (Tuple<Expr, Integer> versionedRow : conflictingRow.values()) {
			Table table = tables.stream().filter(t -> versionedRow.x.toString().contains(t.getName())).findAny().get();
			String delim = "";
			String columnNames = "(";
			String columnVals = "(";
			for (Column column : table.getColumns()) {
				columnNames += (delim + column.name);
				FuncDecl projFunc = objs.getfuncs(table.getName() + "_PROJ_" + column.name);
				String val = model
						.eval(projFunc.apply(versionedRow.x, ctx.mkBV(versionedRow.y, ConstantArgs._MAX_BV_)), true)
						.toString();
				columnVals += (delim + val);
				delim = ",";
			}
			this.cqlData.add(new Tuple<String, Tuple<String, String>>(table.getName(),
					new Tuple<String, String>((columnNames + ")"), columnVals + ")")));
			break; // because we only need the very first version to be instantiated
		}
		// write to file
		for (Tuple<String, Tuple<String, String>> dataElement : cqlData) {
			String tableName = dataElement.x;
			String Cnames = dataElement.y.x;
			String Vals = dataElement.y.y;
			printer.append("\nINSERT INTO testks." + tableName.toUpperCase() + Cnames + " VALUES" + Vals + ";");
			printer.flush();
		}

	}

/////////////////////
	public void createDump(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		Expr[] allOs = model.getSortUniverse(objs.getSort("O"));

		for (Table t : tables) {
			Expr[] allRs = model.getSortUniverse(objs.getSort(t.getName()));
			String delim = "";
			for (@SuppressWarnings("unused")
			Column column : t.getColumns()) {
				delim = ",";
			}
			for (Expr r : allRs) {
				printer.append("\n-------" + r.toString().replaceAll("!val!", "#"));
				for (int i = 0; i < (Math.pow(2, ConstantArgs._MAX_BV_)); i++) {
					delim = "";
					String columnVals = "(";
					String header = "V" + i + ": ";
					for (Column column : t.getColumns()) {
						// columnNames += (delim + column.name);
						FuncDecl projFunc = objs.getfuncs(t.getName() + "_PROJ_" + column.name);
						String val = model.eval(projFunc.apply(r, ctx.mkBV(i, ConstantArgs._MAX_BV_)), true).toString();
						columnVals += (delim + val);
						delim = ",";
					}
					printer.append("\n" + header + columnVals + ")");
				}

			}
		}

		for (Expr o : allOs)
			for (String nextVar : allNextVars.keySet()) {
				FuncDecl varFunc = allNextVars.get(nextVar);
				FuncDecl parent = objs.getfuncs("parent");
				Expr t = model.eval(parent.apply(o), true);
				Expr row = model.eval(varFunc.apply(t), true);
				Table table = tables.stream().filter(tab -> row.toString().contains(tab.getName())).findAny().get();
				FuncDecl verFunc = objs.getfuncs(table.getName() + "_VERSION");
				Expr version = model.eval(verFunc.apply(row, o), true);

				String columnNames = "";
				String columnVals = "";
				String header = t.toString().replaceAll("!val!", "") + "-" + o.toString().replaceAll("!val!", "") + ": "
						+ table.getName() + ": V" + version + " ";

				printer.append("\n" + header + columnNames + "" + columnVals + "");
			}
		printer.flush();

	}

}
