package cons;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import soot.Transformer;
import cons.ConstantArgs;

public class ConstantArgs {
	// add name of transactions to be excluded from the analysis for testing
	public static List<String> _EXCLUDED_TXNS = Arrays.asList("initialize");
	public static boolean DEBUG_MODE = (LogManager.getLogger(Transformer.class).getLevel() == Level.DEBUG);
	public static String _BENCHMARK_NAME;
	public static boolean EXTRACT_ONLY;
	public static boolean _INSTANTIATE_NON_CYCLE_OPS, _INSTANTIATE_PREVIOUS_ONLY;
	public static boolean _CONTINUED_ANALYSIS;
	public static int _MAX_BV_;
	public static boolean _LOG_ASSERTIONS, _ENFORCE_ROW_INSTANCE_LIMITS;
	public static int _MAX_NUM_PARTS, _LIMIT_ITERATIONS_PER_RUN;
	public static int _current_partition_size, _MAX_ROW_INSTANCES, _Minimum_Cycle_Length;
	public static int _Current_Cycle_Length, _MAX_CYCLE_LENGTH;
	public static boolean _ENFORCE_VERSIONING, _current_version_enforcement, _DEP_ONLY_ON_READ_WRITES;
	public static boolean _NO_WW, _NO_WR, _NO_RW;
	public static int _MAX_TXN_INSTANCES;
	public static boolean _ENFORCE_OPTIMIZED_ALGORITHM;

	// read config.properties file and set the constants
	public ConstantArgs() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			ConstantArgs._current_partition_size = 1;
			ConstantArgs._ENFORCE_VERSIONING = true;
			ConstantArgs._current_version_enforcement = true;
			ConstantArgs._DEP_ONLY_ON_READ_WRITES = true; // XXX
			input = new FileInputStream("config.properties");
			prop.load(input);
			ConstantArgs._BENCHMARK_NAME = prop.getProperty("_BENCHMARK_NAME");
			ConstantArgs.EXTRACT_ONLY = Boolean.parseBoolean(prop.getProperty("EXTRACT_ONLY", "false"));
			ConstantArgs.EXTRACT_ONLY = Boolean.parseBoolean(prop.getProperty("EXTRACT_ONLY", "false"));
			ConstantArgs._INSTANTIATE_NON_CYCLE_OPS = Boolean
					.parseBoolean(prop.getProperty("_INSTANTIATE_NON_CYCLE_OPS", "false"));
			ConstantArgs._LOG_ASSERTIONS = Boolean
					.parseBoolean(prop.getProperty("_LOG_ASSERTIONS", "true"));
			ConstantArgs._INSTANTIATE_PREVIOUS_ONLY = Boolean
					.parseBoolean(prop.getProperty("_INSTANTIATE_PREVIOUS_ONLY", "true"));
			ConstantArgs._CONTINUED_ANALYSIS = Boolean.parseBoolean(prop.getProperty("_CONTINUED_ANALYSIS", "false"));
			ConstantArgs._MAX_BV_ = Integer.parseInt(prop.getProperty("_MAX_BV_", "4"));
			ConstantArgs._MAX_NUM_PARTS = Integer.parseInt(prop.getProperty("_MAX_NUM_PARTS", "1"));
			ConstantArgs._MAX_ROW_INSTANCES = Integer.parseInt(prop.getProperty("_MAX_ROW_INSTANCES", "1"));
			ConstantArgs._ENFORCE_ROW_INSTANCE_LIMITS = Boolean
					.parseBoolean(prop.getProperty("_ENFORCE_ROW_INSTANCE_LIMITS", "false"));
			ConstantArgs._Minimum_Cycle_Length = Integer.parseInt(prop.getProperty("_Minimum_Cycle_Length", "3"));
			ConstantArgs._MAX_CYCLE_LENGTH = Integer.parseInt(prop.getProperty("_MAX_CYCLE_LENGTH", "3"));
			ConstantArgs._NO_WW = true; //Boolean.parseBoolean(prop.getProperty("_NO_WW", "false"));
			ConstantArgs._NO_RW = false; //Boolean.parseBoolean(prop.getProperty("_NO_RW", "false"));
			ConstantArgs._NO_WR = false; //Boolean.parseBoolean(prop.getProperty("_NO_WR", "false"));
			ConstantArgs._MAX_TXN_INSTANCES = Integer.parseInt(prop.getProperty("_MAX_TXN_INSTANCES", "-1"));

			ConstantArgs._ENFORCE_OPTIMIZED_ALGORITHM = Boolean
					.parseBoolean(prop.getProperty("_ENFORCE_OPTIMIZED_ALGORITHM", "true"));
			ConstantArgs._LIMIT_ITERATIONS_PER_RUN = Integer
					.parseInt(prop.getProperty("_LIMIT_ITERATIONS_PER_RUN", "-1"));

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
