package anomaly;

/*
 * creates a .dot file to be used for visualization 
 * 
 * 
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ar.ddl.Column;
import ar.ddl.Table;
import utils.Tuple;
import cons.ConstantArgs;
import Z3.DeclaredObjects;

public class RecordsVisualizer {

	Model model;
	DeclaredObjects objs;
	Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	List<Tuple<Expr, Integer>> orderedConflictingRows;
	ArrayList<Table> tables;
	Context ctx;

	public RecordsVisualizer(Context ctx, Model model, DeclaredObjects objs, ArrayList<Table> tables,
			Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow) {
		this.ctx = ctx;
		this.model = model;
		this.objs = objs;
		this.tables = tables;
		this.conflictingRow = conflictingRow;
		orderedConflictingRows = new ArrayList<>();

	}

	public void createGraph(String fileName, int anmlNo) {
		try {
			if (anmlNo == 1 && !ConstantArgs._CONTINUED_ANALYSIS) {
				File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME);
				file.getParentFile().mkdirs();
				FileUtils.deleteDirectory(file);

				File resultFile = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/results.csv");

				resultFile.getParentFile().mkdirs();
				resultFile.createNewFile();
				String line = "Anomaly,category,length,#txns,Description,Internal/External,Analysis Time (ms),Annot. Time (ms)";
				try {
					Files.write(Paths.get("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/results.csv"),
							(line + "\n").getBytes(), StandardOpenOption.APPEND);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		FileWriter writer = null;

		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();

		PrintWriter printer;
		String node_style = "node[shape=record, color=midnightblue, fontcolor=midnightblue, fontsize=10, fontname=\"Helvetica\"]";

		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		model.getSortUniverse(objs.getSort("O"));
		printer.append("digraph {\n" + node_style);

		Map<Expr, Set<Integer>> orderedVersionsMap = new HashMap<>();

		
		for (Tuple<Expr, Integer> versionedRow : conflictingRow.values()) {
			Table table = tables.stream().filter(t -> versionedRow.x.toString().contains(t.getName())).findAny().get();
			String content = "";
			String lable = versionedRow.x.toString().replaceAll("!val!", "");
			String labelVersion = String.valueOf(versionedRow.y);
			// book keeping for the next loop which prints the edges
			if (orderedVersionsMap.get(versionedRow.x) == null)
				orderedVersionsMap.put(versionedRow.x, new HashSet<>());
			orderedVersionsMap.get(versionedRow.x).add(versionedRow.y);
			String tableStringBeing = "\n" + lable + labelVersion + "" + " [label=\"{";
			String tableStringEnd = "\"];";
			content += "{";
			content += "{Z3 label|" + lable + "(v" + labelVersion + ")}";
			for (Column column : table.getColumns()) {
				FuncDecl projFunc = objs.getfuncs(table.getName() + "_PROJ_" + column.name);
				String value = (model.eval(
						projFunc.apply(versionedRow.x, ctx.mkBV(versionedRow.y, ConstantArgs._MAX_BV_)), true))
								.toString();
				content += "|{";
				content += column.name;
				content += "|";
				content += value.replaceAll("\"", "-");
				content += "}";
			}

			content += "}}";
			printer.append(tableStringBeing + content + tableStringEnd);
			printer.flush();

		}
		
		
		
		
		List<Integer> orderedListOfVersions = new ArrayList<>();
		Set<Expr> seenExprs = new HashSet<>();
		String edgeStyle = "[concentrate=true,weight=10, arrowhead=normal, arrowsize=0.7, color=gray70]";
		for (Tuple<Expr, Integer> versionedRow : conflictingRow.values()) {
			Expr currentExpr = versionedRow.x;
			if (!seenExprs.contains(currentExpr)) {
				seenExprs.add(currentExpr);
				orderedListOfVersions.clear();
				orderedListOfVersions.addAll(orderedVersionsMap.get(currentExpr));
				Collections.sort(orderedListOfVersions);
				for (int i = 0; i < orderedListOfVersions.size() - 1; i++) {
					
					String label = currentExpr.toString().replaceAll("!val!", "");
					printer.append("\n" + label + orderedListOfVersions.get(i));
					printer.append(" -> " + label + orderedListOfVersions.get(i + 1));
					printer.append(edgeStyle);
				}
			}
		}

		printer.append("\n}");
		printer.flush();



	}

	private void printTables() {
		for (Table table : tables) {
			System.out.println("\n\n----------------------------------------------------------");
			System.out.println("---- Table: " + table.getName());
			for (Expr row : Arrays.asList(model.getSortUniverse(objs.getSort(table.getName()))))
				printRow(row);
		}

	}

	private void printRow(Expr row) {
		System.out.println(row);

	}
}
