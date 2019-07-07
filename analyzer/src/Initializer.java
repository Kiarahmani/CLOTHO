
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cons.ConstantArgs;
import soot.PackManager;
import soot.Transform;

public class Initializer {
	private static final Logger LOG = LogManager.getLogger(Transformer.class);
	static String _RT_PATH = System.getenv("CLOTHO_RT_PATH");
	static String _JCE_PATH = System.getenv("CLOTHO_JCE_PATH");
	private static final String altClassPathOptionName = "alt-class-path";
	static final String graphTypeOptionName = "graph-type";
	static final String defaultGraph = "BriefUnitGraph";
	static final String irOptionName = "ir";
	static final String defaultIR = "Grimp";
	static final String multipageOptionName = "multipages";
	static final String briefLabelOptionName = "brief";

	public String[] initialize() {
		LOG.info("Beginning initialize method");
		Transformer viewer = new Transformer();
		Transform printTransform = new Transform("jtp.printcfg", viewer);
		printTransform.setDeclaredOptions("enabled " + altClassPathOptionName + ' ' + graphTypeOptionName + ' '
				+ irOptionName + ' ' + multipageOptionName + ' ' + briefLabelOptionName + ' ');
		printTransform.setDefaultOptions("enabled " + altClassPathOptionName + ": " + graphTypeOptionName + ':'
				+ defaultGraph + ' ' + irOptionName + ':' + defaultIR + ' ' + multipageOptionName + ":false " + ' '
				+ briefLabelOptionName + ":false ");
		PackManager.v().getPack("jtp").add(printTransform);
		LOG.info("Preparing Args");
		String[] soot_args = new String[3];
		soot_args[0] = "--soot-classpath";
		if (_RT_PATH == null)
			LOG.fatal("CLOTHO_RT_PATH = \"\". Make sure environment variables are correctly set");
		if (_JCE_PATH == null)
			LOG.fatal("CLOTHO_JCE_PATH = \"\". Make sure environment variables are correctly set");
		soot_args[1] = System.getProperty("user.dir") + "/bin:" + _RT_PATH + ":" + _JCE_PATH;
		soot_args[2] = "benchmarks." + ConstantArgs._BENCHMARK_NAME + "." + ConstantArgs._BENCHMARK_NAME;
		LOG.info(soot_args[1]);
		LOG.info("Args prepared: ready to return");

		return soot_args;
	}
}
