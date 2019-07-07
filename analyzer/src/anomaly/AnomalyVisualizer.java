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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import utils.Tuple;
import cons.ConstantArgs;
import Z3.DeclaredObjects;

public class AnomalyVisualizer {

	Map<Expr, ArrayList<Expr>> WWPairs;
	Map<Expr, ArrayList<Expr>> WRPairs;
	Map<Expr, ArrayList<Expr>> RWPairs;
	Map<Expr, ArrayList<Expr>> visPairs;
	Map<Expr, Expr> cycle;
	Map<Expr, Expr> otype;
	Map<Expr, Expr> opart;
	Map<Expr, Expr> Rs;
	Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRows;
	Model model;
	DeclaredObjects objs;
	Map<Expr, ArrayList<Expr>> parentChildPairs;

	public AnomalyVisualizer(Map<Expr, ArrayList<Expr>> wWPairs, Map<Expr, ArrayList<Expr>> wRPairs,
			Map<Expr, ArrayList<Expr>> rWPairs, Map<Expr, ArrayList<Expr>> visPairs, Map<Expr, Expr> cycle, Model model,
			DeclaredObjects objs, Map<Expr, ArrayList<Expr>> parentChildPairs, Map<Expr, Expr> otype,
			Map<Expr, Expr> opart, Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRows) {
		this.WRPairs = wRPairs;
		this.WWPairs = wWPairs;
		this.visPairs = visPairs;
		this.RWPairs = rWPairs;
		this.cycle = cycle;
		this.model = model;
		this.objs = objs;
		this.Rs = Rs;
		this.parentChildPairs = parentChildPairs;
		this.otype = otype;
		this.opart = opart;
		this.conflictingRows = conflictingRows;
	}

	private String rwB_edge_setting(String rowName, String rowVersion, String boldStyle) {
		return "[label = \"RW\n" + rowName + "(v" + rowVersion + ")\"," + boldStyle + "]";
	}

	private String wrB_edge_setting(String rowName, String rowVersion, String boldStyle) {
		return "[label = \"WR\n" + rowName + "(v" + rowVersion + ")\"," + boldStyle + "]";
	}

	private String wwB_edge_setting(String rowName, String rowVersion, String boldStyle) {
		return "[label = \"WW\n" + rowName + "(v" + rowVersion + ")\"," + boldStyle + "]";
	}

	public void createGraph(String fileName) {
		String[] colors = new String[] { "lightyellow", "darkkhaki", "cornsilk1", "rosybrown1", "thistle", "lavender",
				"ivory3", "mintcream" };
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[ color=darkgoldenrod4, fontcolor=darkgoldenrod4, fontsize=10, fontname=\"Helvetica\"]";
		String edge_style = "\nedge[fontsize=12, fontname=\"Helvetica\"]";
		String invis_edge_style = "\n[style=invis,weight=4]";
		String graph_style1 = "\nrankdir=RL\n" + "style=filled\n" + "fontname=\"Helvetica\"\n"
				+ "fontcolor=darkgoldenrod4\n" + "color=";
		String graph_style2 = "\n style=\"rounded,filled\"\n" + "fontsize=10\n";
		String bold_style = " penwidth=2.0,weight=2, style=bold, arrowhead=normal, arrowtail=inv, arrowsize=0.9, color=red3, fontsize=11, fontcolor=red3";
		String normal_style = " style=solid,weight=0.2, arrowhead=normal, arrowtail=inv, arrowsize=0.7, color=gray70, fontsize=10, fontcolor=gray60";
		String rw_edge_setting = "[label = \"rw\", " + normal_style + "]";
		String wr_edge_setting = "[label = \"wr\", " + normal_style + "]";
		String ww_edge_setting = "[label = \"ww\", " + normal_style + "]";

		String vis_edge_setting = "[label = \"vis\",concentrate=true, style=dotted,weight=2, arrowhead=normal, arrowtail=inv, arrowsize=0.7, color=gray70, fontsize=10, fontcolor=gray60]";
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		printer.append("digraph {" + node_style + edge_style);
		String ttype = "";
		String opart = "";
		int iter = 0;
		int randomColor = ThreadLocalRandom.current().nextInt(0, 10);
		for (Expr t : Ts) {
			ttype = model.eval(objs.getfuncs("ttype").apply(t), true).toString();
			// TODO -> The below coloring is based on the first child - what if childs are
			// at different partitions?
			if (parentChildPairs.get(t) != null) {
				opart = model.eval(objs.getfuncs("opart").apply(parentChildPairs.get(t).get(0)), true).toString();
				printer.append("\nsubgraph cluster_"
						+ iter + "{" + (graph_style1
								+ colors[(Integer.valueOf(opart) + randomColor) % (colors.length - 1)] + graph_style2)
						+ " \n");
				printer.append("label=\" " + t.toString().replaceAll("!val!", "") + "\n(" + ttype + ")" + "\";\n");
				for (Expr o : parentChildPairs.get(t)) {
					String otime = model.eval(objs.getfuncs("otime").apply(o), true).toString();
					String name = o.toString().replaceAll("!val!", "");
					String title = "[ label=\"@" +otime + " " + name + ":"
 							+ otype.get(o).toString().replace('|', ' ').replace('-', ' ').split(" ")[2] + "\"]";
					printer.append(name + title + ";\n ");
				}
				printer.append("}");
				iter++;
			}
		}
		// print invisible edges to put clusters close to each other
		printer.append("\n\n");
		String opart1, opart2;
		for (Expr o1 : Os) {
			for (Expr o2 : Os) {
				opart1 = model.eval(objs.getfuncs("opart").apply(o1), true).toString();
				opart2 = model.eval(objs.getfuncs("opart").apply(o2), true).toString();
				String name1 = o1.toString().replaceAll("!val!", "");
				String name2 = o2.toString().replaceAll("!val!", "");
				if (opart1.equals(opart2))
					printer.append(name1 + " -> " + name2 + invis_edge_style + "\n");
			}
		}

		printer.append("\n\n");
		for (Expr t : Ts) {
			if (parentChildPairs.get(t) != null) {
				for (Expr o : parentChildPairs.get(t)) {

					// vis
					if (visPairs.get(o) != null)
						for (Expr o1 : visPairs.get(o))
							if (o1 != null)
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + vis_edge_setting + ";\n");

					// WW
					if (WWPairs.get(o) != null)
						for (Expr o1 : WWPairs.get(o))
							if (o1 != null) {
								if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString())) {
									Expr confRow = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).x;
									String confRowName = confRow.toString().replaceAll("!val!", "");
									int confRowVersion = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).y;
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "")
											+ wwB_edge_setting(confRowName, String.valueOf(confRowVersion), bold_style)
											+ ";\n");
								} else
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "") + ww_edge_setting + ";\n");
							}
					// WR
					if (WRPairs.get(o) != null)
						for (Expr o1 : WRPairs.get(o))
							if (o1 != null)
								if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString())) {
									Expr confRow = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).x;
									String confRowName = confRow.toString().replaceAll("!val!", "");
									int confRowVersion = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).y;
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "")
											+ wrB_edge_setting(confRowName, String.valueOf(confRowVersion), bold_style)
											+ ";\n");
								} else
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "") + wr_edge_setting + ";\n");
					// RW
					if (RWPairs.get(o) != null)
						for (Expr o1 : RWPairs.get(o))
							if (o1 != null)
								if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString())) {
									Expr confRow = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).x;
									String confRowName = confRow.toString().replaceAll("!val!", "");
									int confRowVersion = conflictingRows.get(new Tuple<Expr, Expr>(o, o1)).y;
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "")
											+ rwB_edge_setting(confRowName, String.valueOf(confRowVersion), bold_style)
											+ ";\n");
								} else
									printer.append(o.toString().replaceAll("!val!", "") + " -> "
											+ o1.toString().replaceAll("!val!", "") + rw_edge_setting + ";\n");
				}
			}

		}

		printer.append("\n}");
		printer.flush();

	}
}
