package anomaly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import utils.Tuple;
import cons.ConstantArgs;

public class AnomalyStructure {
	private List<List<Tuple<String, Tuple<String, String>>>> structures = new ArrayList<>();

	public List<List<Tuple<String, Tuple<String, String>>>> getStructures() {
		return this.structures;
	}

	public void addStructure(List<Tuple<String, Tuple<String, String>>> struct) {
		this.structures.add(struct);

	}

	public void writeToCSV(int anmlNo, int run, Anomaly anml) {
		String line = "";
		line += ("#" + String.valueOf(anmlNo) + ","); // anomaly number
		line += String.valueOf(run) + ","; // category
		line += String.valueOf(anml.getCycleStructure().size()) + ","; // cycle length
		line += String.valueOf(anml.parentChildPairs.size()) + ","; // number of txns
		line += " " + ","; // description
		line += " " + ","; // internal/external
		line += String.valueOf(anml.getStepOneTime() + ",");
		line += String.valueOf(anml.getStepTwoTime());

		try {
			Files.write(Paths.get("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/results.csv"),
					(line + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
		}

	}

	// TODO
	public void save() throws IOException {

		try {
			File oldFile = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
			oldFile.delete();
			FileOutputStream fout;
			fout = new FileOutputStream("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(this.structures);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		}

	}

	// TODO: verify it works correctly
	@SuppressWarnings("unchecked")
	public void load() throws ClassNotFoundException, IOException {

		FileInputStream streamIn;
		try {
			streamIn = new FileInputStream("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
			ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
			this.structures = (List<List<Tuple<String, Tuple<String, String>>>>) objectinputstream.readObject();
			objectinputstream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
		}

	}

	public int size() {
		return this.structures.size();
	}

}
