package anomaly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import cons.ConstantArgs;

public class RawDataVisualizer {
	String text;

	public RawDataVisualizer(String text) {
		this.text = text;
	}

	public void createGraph(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[shape=box, color=gray100, fontcolor=maroon, fontsize=12, fontname=\"Helvetica\"]";
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);

		printer.append("digraph {" + node_style);
		printer.append("1 [label=\"");
		printer.append(this.text.replaceAll("\"", "\\\""));
		printer.append("\"]");
		printer.append("\n}");
		printer.flush();

	}

}
