package schedules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import sync.OpType;

public final class DR_schedule {
	ArrayList<OpType> execOrder;

	public DR_schedule() {
		execOrder = new ArrayList<OpType>();

		// read and par the test config file
		// JSONParser jsonParser = new JSONParser();
		try {
			FileReader reader = new FileReader("../../../tests/schedule.json");
			BufferedReader bufferedReader = new BufferedReader(reader);
			StringBuffer stringBuffer = new StringBuffer();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] arr = line.split(",");
				execOrder.add(new OpType(Integer.valueOf(arr[0]), -1, "", arr[1], arr[2], Integer.valueOf(arr[3])));
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<OpType> getSchedule() {
		return this.execOrder;
	}

}
