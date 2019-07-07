package anomaly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ar.Application;
import ar.Transaction;
import ar.ddl.Table;
import utils.Tuple;
import cons.ConstantArgs;
import Z3.DeclaredObjects;

public class Anomaly {
	private String rawData = "";
	private String name;
	private Model model;
	long stepOneTime, stepTwoTime;

	public long getStepOneTime() {
		return stepOneTime;
	}

	public long getStepTwoTime() {
		return stepTwoTime;
	}

	public Map<Expr, Expr> getTtypes() {
		return ttypes;
	}

	public List<Expr> getTs() {
		return Ts;
	}

	public ArrayList<Table> getTables() {
		return tables;
	}

	private Context ctx;
	DeclaredObjects objs;
	public Map<Expr, ArrayList<Expr>> visPairs;
	public Map<Expr, ArrayList<Expr>> WRPairs;
	public Map<Expr, ArrayList<Expr>> WWPairs;
	public Map<Expr, ArrayList<Expr>> RWPairs;
	public Map<Expr, ArrayList<Expr>> parentChildPairs;
	public Map<Expr, Expr> cycle;
	public Map<Expr, Expr> otypes;
	public Map<Expr, Expr> ttypes;
	public Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	public Map<Expr, Expr> otimes;
	public Map<Expr, Expr> opart;
	public List<Expr> isUpdate;
	private List<Tuple<String, Tuple<String, String>>> cycleStructure;
	private List<Tuple<String, String>> cycleTxns;
	private Map<Tuple<String, String>, Set<String>> completeStructure;

	private Map<String, Set<Tuple<String, String>>> coreStructure;
	private Application app;
	private boolean isCore;

	public List<Expr> Ts, Os;
	ArrayList<Table> tables;

	public Anomaly(Model model, Context ctx, DeclaredObjects objs, ArrayList<Table> tables, Application app,
			boolean isCore) {
		this.model = model;
		this.ctx = ctx;
		this.objs = objs;
		this.isCore = isCore;
		this.tables = tables;
		this.app = app;
	}

	public void closeCtx() {
		this.ctx.close();
	}

	private int OpTypeToOrder(String o) {
		if (o.startsWith("|"))
			return Integer.valueOf(o.substring(o.length() - 2, o.length() - 1));
		else
			return Integer.valueOf(o.substring(o.length() - 1, o.length()));
	}

	public void generateCycleStructure() {
		Map<String, FuncDecl> functions = getFunctions();
		conflictingRow = new HashMap<>();
		parentChildPairs = getParentChild(functions.get("parent"));
		WWPairs = getWWPairs(functions.get("WW_O"));
		WRPairs = getWRPairs(functions.get("WR_O"));
		RWPairs = getRWPairs(functions.get("RW_O"));
		visPairs = getVisPairs(functions.get("vis"));
		cycle = getCycle(functions.get("D"));
		otypes = getOType(functions.get("otype"));
		otimes = getOTime(functions.get("otime"));
		opart = getOPart(functions.get("opart"));
		ttypes = getTType(functions.get("ttype"));
		isUpdate = getIsUpdate(functions.get("is_update"));
		this.Ts = Arrays.asList(model.getSortUniverse(objs.getSort("T")));
		this.Os = Arrays.asList(model.getSortUniverse(objs.getSort("O")));

		this.cycleStructure = new ArrayList<>();
		this.completeStructure = new HashMap<>();
		this.coreStructure = new HashMap<>();
		this.cycleTxns = new ArrayList<>();
		FuncDecl ttypeFunc = objs.getfuncs("ttype");
		FuncDecl parentFunc = objs.getfuncs("parent");
		FuncDecl otypeFunc = objs.getfuncs("otype");

		Os = Os.stream().filter(o -> cycle.keySet().contains(o) || cycle.values().contains(o))
				.collect(Collectors.toList());
		//////////////////////////////////
		// here we construct the complete structure of the anomaly, which includes other
		////////////////////////////////// operations from the transaction's of already
		////////////////////////////////// instantiated operations
		Map<Expr, Set<String>> txnToLeftAloneChildren = new HashMap<>();
		for (Expr t : Ts) {
			Set<String> allChildren = objs.getAllOTypes().keySet();
			allChildren = allChildren.stream().filter(c -> c.contains(ttypes.get(t).toString()))
					.collect(Collectors.toSet());
			if (parentChildPairs.get(t) != null) {
				int maxOrderSeen = 0;
				for (Expr o : parentChildPairs.get(t)) {
					String thisOType = otypes.get(o).toString();
					allChildren.removeIf(s -> thisOType.contains(s));
					if (maxOrderSeen < OpTypeToOrder(thisOType))
						maxOrderSeen = OpTypeToOrder(thisOType);
				}
				final int finalMaxOrderSeen = maxOrderSeen;

				if (ConstantArgs._INSTANTIATE_PREVIOUS_ONLY)
					allChildren.removeIf(s -> OpTypeToOrder(s) > finalMaxOrderSeen);
				txnToLeftAloneChildren.put(t, allChildren);
			}
		}
		//////////////////////////////////
		if (Os.size() <= 0)
			System.out.println("~~cycle: " + cycle + "\n~~filtered Os: " + Os);
		Expr e = Os.get(0);
		for (int i = 0; i < Os.size(); i++) {
			Expr y = this.cycle.get(e);
			Expr t = model.eval(parentFunc.apply(e), true);
			String eType = model.eval(otypeFunc.apply(e), true).toString();
			// System.out.println("---->>>" + eType);
			completeStructure.put(new Tuple<String, String>(t.toString(), eType), txnToLeftAloneChildren.get(t));
			txnToLeftAloneChildren.remove(t);
			Expr firstParent = t;
			if (y == null) {
				// since there is no outgoing edge from this node, we should look for a sibling
				// which is on the cycle
				y = returnNextSibling(e);
				if (y == null) {
					System.out.println("~~~~>>" + cycle);
					System.out.println("~~~~>>" + y);
					System.out.println("~~~~>>" + e);
				}

				Tuple<String, String> newTuple = new Tuple<String, String>(
						model.eval(otypeFunc.apply(e), true).toString(),
						model.eval(otypeFunc.apply(y), true).toString());

				this.cycleStructure.add(new Tuple<String, Tuple<String, String>>("sibling", newTuple));
			} else {
				Tuple<String, String> newTuple = new Tuple<String, String>(
						model.eval(otypeFunc.apply(e), true).toString(),
						model.eval(otypeFunc.apply(y), true).toString());
				if (RWPairs.get(e) != null && RWPairs.get(e).contains(y))
					this.cycleStructure.add(new Tuple<String, Tuple<String, String>>("RW", newTuple));
				else if (WRPairs.get(e) != null && WRPairs.get(e).contains(y))
					this.cycleStructure.add(new Tuple<String, Tuple<String, String>>("WR", newTuple));
				else if (WWPairs.get(e) != null && WWPairs.get(e).contains(y))
					this.cycleStructure.add(new Tuple<String, Tuple<String, String>>("WW", newTuple));
			}
			Expr secondParent = model.eval(parentFunc.apply(y), true);
			this.cycleTxns.add(new Tuple<String, String>(firstParent.toString(), secondParent.toString()));
			e = y;
		}

		// generating the core for the innerloop iterations
		for (Expr x : this.cycle.keySet()) {
			Expr y = this.cycle.get(x);
			// System.out.println("---->>>>>"+x+"---"+y);
			Tuple<String, String> newTupleCore = new Tuple<String, String>(
					model.eval(ttypeFunc.apply(parentFunc.apply(x)), true).toString(),
					model.eval(ttypeFunc.apply(parentFunc.apply(y)), true).toString());
			if (RWPairs.get(x) != null && RWPairs.get(x).contains(y)) {
				if (this.coreStructure.get("RW") == null)
					this.coreStructure.put("RW", new HashSet<>());
				this.coreStructure.get("RW").add(newTupleCore);
			} else if (WRPairs.get(x) != null && WRPairs.get(x).contains(y)) {
				if (this.coreStructure.get("WR") == null)
					this.coreStructure.put("WR", new HashSet<>());
				this.coreStructure.get("WR").add(newTupleCore);
			} else if (WWPairs.get(x) != null && WWPairs.get(x).contains(y)) {
				if (this.coreStructure.get("WW") == null)
					this.coreStructure.put("WW", new HashSet<>());
				this.coreStructure.get("WW").add(newTupleCore);
			}
		}
	}

	private Expr returnNextSibling(Expr o1) {
		// System.out.println("~~~" + o1);
		// System.out.println("~~~" + cycle);
		for (Expr o2 : Os)
			if (areSibling(o2, o1) && cycle.keySet().contains(o2))
				return o2;
		return null;
	}

	public String getTypeOfTxnByName(String txnName) {
		Expr tInstance = this.Ts.stream().filter(t -> t.toString().contains(txnName)).collect(Collectors.toList())
				.get(0);
		FuncDecl ttypeFunc = objs.getfuncs("ttype");
		return this.model.eval(ttypeFunc.apply(tInstance), true).toString();
	}

	private boolean areSibling(Expr o1, Expr o2) {
		return (!o1.equals(o2))
				&& this.parentChildPairs.values().stream().anyMatch(set -> (set.contains(o1) && set.contains(o2)));

	}

	public List<Tuple<String, String>> getCycleTxns() {
		return this.cycleTxns;
	}

	public Map<Tuple<String, String>, Set<String>> getCompleteStructure() {
		return completeStructure;
	}

	public List<Tuple<String, Tuple<String, String>>> getCycleStructure() {
		return this.cycleStructure;
	}

	public Map<String, Set<Tuple<String, String>>> getCoreCycleStructure() {
		return this.coreStructure;
	}

	public void announce(boolean isCore, int anmlNo) {
//		if (!isCore)
		addData("------------------------\n--- Model #" + anmlNo + " --- "+"\n------------------------\n");
//		else
//			System.out.println("------------------\n--- Core Model --- ");
//		// announce the non-core model
		if (!isCore) {
			addData("\\l {T}:		" + Ts );
			//drawLine();
			addData("\\l ttype:	" + ttypes);
			addData("\\l {O}:		" + Arrays.asList(model.getSortUniverse(objs.getSort("O"))));
			addData("\\l ParChld:	" + parentChildPairs);
			addData("\\l otype:	" + otypes);
			addData("\\l isUpdate:	" + isUpdate);
			addData("\\l WW:	" + WWPairs);
			addData("\\l RW:		" + RWPairs);
			addData("\\l WR:		" + WRPairs);
			addData("\\l vis:		" + visPairs);
			addData("\\l cyc:		" + cycle);
			addData("\\l otime:	" + otimes);
			addData("\\l opart:	" + opart);
			// if (ConstantArgs.DEBUG_MODE)
			// System.out.println(model);
			addData("\\l");
			addData("\\l ---------------------------------------------------------------------------");

			// if (ConstantArgs.DEBUG_MODE)
			// printAllVersions();
// XXX
			// System.out.println("--- TXN Params --- ");
			addData("\\l");
			for (Expr t : Ts) {
				String tVal = t.toString().replaceAll("!val!", "") + ": ";
				// System.out.print(tVal);
				addData(tVal);
				Expr ttype = model.eval(objs.getfuncs("ttype").apply(t), true);
				// System.out.print(ttype + "(");
				addData(ttype + "(");
				Transaction txn = app.getTxnByName(ttype.toString());
				String delim = "";
				for (String pm : txn.getParams().keySet()) {
					// System.out.print(delim + pm + "=");
					addData(delim + pm + "=");
					String modelVal = (model.eval(ctx.mkApp(objs.getfuncs(ttype.toString() + "_PARAM_" + pm), t), true))
							.toString();
					// System.out.print(modelVal);
					addData(modelVal);
					delim = ", ";
				}
				// System.out.println(")");
				addData(")" + "\\l");
			}
			this.rawData = this.rawData.replaceAll("\"", "\\\\\"");
			// visualize records (this should go before the rest of the visualization steps
			// since it clears the previous data)
			RecordsVisualizer rv = new RecordsVisualizer(ctx, model, objs, tables, conflictingRow);
			rv.createGraph("anomaly#" + anmlNo + "/records_" + anmlNo + ".dot", anmlNo);
			// visualize the raw data
			addData("\\l---------------------------------------------------------------------------");
			//addData("\\l primitive anomaly extraction:   " + String.valueOf(stepOneTime) + "ms");
			addData("\\l Anomaly extraction time: " + String.valueOf(stepTwoTime) + " ms");
			addData("\\l");

			RawDataVisualizer dv = new RawDataVisualizer(this.rawData);
			dv.createGraph("anomaly#" + anmlNo + "/data_" + anmlNo + ".dot");
			// visualize the cycle
			AnomalyVisualizer av = new AnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model, objs,
					parentChildPairs, otypes, opart, conflictingRow);
			av.createGraph("anomaly#" + anmlNo + "/anomaly_" + anmlNo + ".dot");

			// create schedule
			scheduleGen sg = new scheduleGen(ctx, model, objs, tables, conflictingRow, this.Ts, this.Os, this.app);
			sg.createSchedule("anomaly#" + anmlNo + "/schedule" + ".json");
			sg.createInstance("anomaly#" + anmlNo + "/instance" + ".json");
			sg.createData("anomaly#" + anmlNo + "/" + anmlNo + ".cql");
			sg.createDump("anomaly#" + anmlNo + "/" + anmlNo + ".dump");

		}
		// announce core model
		else {
			List<Set<Expr>> coreOpSets = getCoreOps();
			Map<Expr, Expr> coreDep = new HashMap<>();
			System.out.println("Core Os:     " + coreOpSets);
			// find connections between core operations
			for (int i = 0; i < coreOpSets.size(); i++)
				for (int j = 0; j < coreOpSets.size(); j++)
					if (i != j) {
						Set<Expr> set1 = coreOpSets.get(i);
						Set<Expr> set2 = coreOpSets.get(j);
						for (Expr o1 : set1)
							for (Expr o2 : set2)
								if (areConnected(o1, o2))
									coreDep.put(o1, o2);
					}
			CoreAnomalyVisualizer av = new CoreAnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model,
					objs, parentChildPairs, otypes, opart, coreDep, coreOpSets);
			av.createGraph("anomaly_core.dot");
			System.out.println("Core Edges:  " + coreDep);
		}
	}

	private void printAllVersions() {
		System.out.println("\n\n==== ROWS AND VERSIONS:\n===========================");
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		for (Table t : tables) {
			Expr[] Rs = model.getSortUniverse(objs.getSort(t.getName()));
			FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
			for (Expr r1 : Rs) {
				if (conflictingRow.values().stream().map(tuple -> tuple.x).collect(Collectors.toList()).contains(r1)) {
					System.out.println("\n===" + r1);
					for (Expr o : Os)
						System.out.print("(" + o.toString().replaceAll("!val!", "") + ","
								+ model.eval(verFunc.apply(r1, o), true) + ")");
				}

			}
			System.out.println();
		}
		System.out.println("===========================\n\n\n");

	}

	private List<Expr> getIsUpdate(FuncDecl isUpdate) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		List<Expr> result = new ArrayList<>();

		for (Expr o : Os) {
			if (model.eval(isUpdate.apply(o), true).toString().equals("true"))
				result.add(o);
		}
		return result;
	}

	private Map<Expr, Expr> getCycle(FuncDecl x) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new LinkedHashMap<>();

		for (Expr o1 : Os)
			for (Expr o2 : Os) {
				if (model.eval(x.apply(o1, o2), true).toString().equals("true")) {
					String o1Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o1), true).toString();
					String o2Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o2), true).toString();
					FuncDecl func = objs.getfuncs(o1Type.substring(1, o1Type.length() - 1) + "_"
							+ o2Type.substring(1, o2Type.length() - 1) + "_conflict_rows");
					Expr row = model.eval(ctx.mkApp(func, o1, o2), true);
					String tableName = row.getSort().toString();
					BitVecNum version = (BitVecNum) model
							.eval(ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), row, o1), true);
					result.put(o1, o2);
					conflictingRow.put(new Tuple<Expr, Expr>(o1, o2), new Tuple<Expr, Integer>(row, version.getInt()));
				}
			}

		return result;
	}

	private Map<Expr, Expr> moreStuff(FuncDecl x) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new LinkedHashMap<>();

		for (Expr o1 : Os)
			for (Expr o2 : Os) {
				if (model.eval(x.apply(o1, o2), true).toString().equals("true")) {
					String o1Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o1), true).toString();
					String o2Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o2), true).toString();
					FuncDecl func = objs.getfuncs(o1Type.substring(1, o1Type.length() - 1) + "_"
							+ o2Type.substring(1, o2Type.length() - 1) + "_conflict_rows");
					Expr row = model.eval(ctx.mkApp(func, o1, o2), true);
					String tableName = row.getSort().toString();
					BitVecNum version = (BitVecNum) model
							.eval(ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), row, o1), true);
					result.put(o1, o2);
					conflictingRow.put(new Tuple<Expr, Expr>(o1, o2), new Tuple<Expr, Integer>(row, version.getInt()));
				}
			}

		return result;
	}

	public Map<String, FuncDecl> getFunctions() {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));
		Map<String, FuncDecl> result = new HashMap<>();
		for (FuncDecl f : model.getFuncDecls()) {
			if (f.getName().toString().contains("parent"))
				result.put("parent", f);
			else if (f.getName().toString().equals("vis"))
				result.put("vis", f);
			else if (f.getName().toString().equals("WW_O"))
				result.put("WW_O", f);
			else if (f.getName().toString().equals("WR_O"))
				result.put("WR_O", f);
			else if (f.getName().toString().equals("RW_O"))
				result.put("RW_O", f);
			else if (f.getName().toString().equals("D"))
				result.put("D", f);
			else if (f.getName().toString().equals("X"))
				result.put("X", f);
			else if (f.getName().toString().equals("ttype"))
				result.put("ttype", f);
			else if (f.getName().toString().equals("is_update"))
				result.put("is_update", f);
			else if (f.getName().toString().equals("otime"))
				result.put("otime", f);
			else if (f.getName().toString().equals("opart"))
				result.put("opart", f);
			else if (f.getName().toString().equals("otype")) {
				result.put("otype", f);
			}
		}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getVisPairs(FuncDecl vis) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(vis.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getWWPairs(FuncDecl ww) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(ww.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getRWPairs(FuncDecl rw) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(rw.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getWRPairs(FuncDecl wr) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(wr.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, Expr> getOTime(FuncDecl otimes) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		if (otimes != null)
			for (Expr o : Os) {
				t = model.eval(otimes.apply(o), true);
				result.put(o, t);
			}
		return result;
	}

	private Map<Expr, Expr> getOPart(FuncDecl opart) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		if (opart != null)
			for (Expr o : Os) {
				t = model.eval(opart.apply(o), true);
				result.put(o, t);
			}
		return result;
	}

	private Map<Expr, Expr> getOType(FuncDecl oType) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		for (Expr o : Os) {
			t = model.eval(oType.apply(o), true);
			result.put(o, t);
		}
		return result;
	}

	private Map<Expr, Expr> getTType(FuncDecl ttype) {
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr tp;
		for (Expr t : Ts) {
			tp = model.eval(ttype.apply(t), true);
			result.put(t, tp);
		}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getParentChild(FuncDecl parent) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		Expr t;
		ArrayList<Expr> child;
		for (Expr o : Os) {
			t = model.eval(parent.apply(o), true);
			child = result.get(t);
			if (child == null)
				child = new ArrayList<Expr>();
			child.add(o);
			result.put(t, child);
		}
		return result;
	}

	private List<Set<Expr>> getCoreOps() {
		ArrayList<Set<Expr>> result = new ArrayList<>();
		Set<Expr> newSet = null;
		List<Expr> Os = Arrays.asList(model.getSortUniverse(objs.getSort("O")));
		for (Expr o1 : Os) {
			for (Expr o2 : Os) {
				if (areSibling(o1, o2))
					// check if set already exists
					if (setIsCreated(result, o1) == null) {
						newSet = new HashSet<>();
						newSet.add(o1);
						newSet.add(o2);
						result.add(newSet);
					} else {
						newSet = setIsCreated(result, o1);
						newSet.add(o1);
						newSet.add(o2);
					}
			}
		}
		return result.stream().filter(s -> s.size() > 1).collect(Collectors.toList());
	}

	private boolean areConnected(Expr o1, Expr o2) {
		Expr next = this.cycle.get(o1);
		if (next == null)
			return false;
		else if (next.equals(o2))
			return true;
		else
			return areConnected(next, o2);
	}

	private Set<Expr> setIsCreated(ArrayList<Set<Expr>> sets, Expr o1) {
		for (Set<Expr> set : sets)
			if (set.contains(o1))
				return set;
		return null;
	}

	private void drawLine() {
		// System.out.println("--------------------------------------");
	}

	public void setExtractionTime(long step1, long step2) {
		this.stepOneTime = step1;
		this.stepTwoTime = step2;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public Context getCtx() {
		return ctx;
	}

	public void setCtx(Context ctx) {
		this.ctx = ctx;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	public boolean isCore() {
		return isCore;
	}

	public void setCore(boolean isCore) {
		this.isCore = isCore;
	}

	public void addData(String s) {
		this.rawData += (s);
	}

}
