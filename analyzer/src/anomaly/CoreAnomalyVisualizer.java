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
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;

import Z3.DeclaredObjects;

public class CoreAnomalyVisualizer {

	Map<Expr, ArrayList<Expr>> WWPairs;
	Map<Expr, ArrayList<Expr>> WRPairs;
	Map<Expr, ArrayList<Expr>> RWPairs;
	Map<Expr, ArrayList<Expr>> visPairs;
	Map<Expr, Expr> cycle;
	Map<Expr, Expr> otype;
	Map<Expr, Expr> opart;
	Map<Expr, Expr> coreDep;
	List<Set<Expr>> coreOpSets;
	Model model;
	DeclaredObjects objs;
	Map<Expr, ArrayList<Expr>> parentChildPairs;

	public CoreAnomalyVisualizer(Map<Expr, ArrayList<Expr>> wWPairs, Map<Expr, ArrayList<Expr>> wRPairs,
			Map<Expr, ArrayList<Expr>> rWPairs, Map<Expr, ArrayList<Expr>> visPairs, Map<Expr, Expr> cycle, Model model,
			DeclaredObjects objs, Map<Expr, ArrayList<Expr>> parentChildPairs, Map<Expr, Expr> otype,
			Map<Expr, Expr> opart, Map<Expr, Expr> coreDep, List<Set<Expr>> coreOpSets) {
		this.WRPairs = wRPairs;
		this.WWPairs = wWPairs;
		this.visPairs = visPairs;
		this.RWPairs = rWPairs;
		this.cycle = cycle;
		this.model = model;
		this.objs = objs;
		this.parentChildPairs = parentChildPairs;
		this.otype = otype;
		this.opart = opart;
		this.coreDep = coreDep;
		this.coreOpSets = coreOpSets;
	}

	public void createGraph(String fileName) {
		File file = new File("anomalies/" + fileName);
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[ color=midnightblue, fontcolor=midnightblue, fontsize=10, fontname=\"Helvetica\"]";
		String edge_style = "\nedge[fontsize=12, fontname=\"Helvetica\"]";
		String graph_style1 = "\nrankdir=RL\n" + "style=filled\n" + "fontname=\"Helvetica\"\n"
				+ "fontcolor=midnightblue\n" + "color=";
		String graph_style2 = "\n style=\"rounded,filled\"\n" + "fontsize=10\n";
		String bold_style = " penwidth=4.0,weight=2, style=bold, arrowhead=normal, arrowtail=inv, arrowsize=1, color=navyblue, fontsize=11, fontcolor=navyblue";
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		model.getSortUniverse(objs.getSort("O"));
		printer.append("digraph {" + node_style + edge_style);
		int iter = 0;
		ThreadLocalRandom.current().nextInt(0, 10);

		for (Set<Expr> set : coreOpSets) {
			printer.append(
					"\nsubgraph cluster_" + iter + "{" + (graph_style1 + "lightskyblue2" + graph_style2) + " \n");
			printer.append("label=\" " + set.toString().replaceAll("!val!", "") + "\";\n");
			for (Expr o : set) {
				model.eval(objs.getfuncs("otime").apply(o), true).toString();
				String name = o.toString().replaceAll("!val!", "");
				String title = "[ label=\"" + otype.get(o).toString().replace('|', ' ').replace('-', ' ').split(" ")[2]
						+ "\"]";
				printer.append(name + title + ";\n ");
			}
			printer.append("}");
			iter++;
		}

		for (Expr x1 : coreDep.keySet()) {
			Expr x2 = coreDep.get(x1);
			String name1 = x1.toString().replaceAll("!val!", "");
			String name2 = x2.toString().replaceAll("!val!", "");
			printer.append("\n"+name1 + " -> " + name2 +" ["+bold_style+"]") ;
		}

		// ttype = model.eval(objs.getfuncs("ttype").apply(t), true).toString();

		printer.append("\n}");
		printer.flush();

	}
}
