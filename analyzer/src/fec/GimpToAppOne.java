package fec;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exceptions.SqlTypeNotFoundException;
import exceptions.UnknownUnitException;
import ar.Application;
import ar.Transaction;
import ar.Type;
import ar.expression.vals.ParamValExp;
import ar.ddl.Table;
import ar.statement.Statement;
import cons.ConstantArgs;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.Transformer;
import soot.Unit;
import soot.Value;

public class GimpToAppOne extends GimpToApp {
	private static final Logger LOG = LogManager.getLogger(Transformer.class);

	public GimpToAppOne(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		super(v2, bodies, tables);
	}

	public Application transform() throws UnknownUnitException {
		Application app = new Application();
		for (Body b : bodies) {
			if (!b.getMethod().getName().contains("init")
					&& !ConstantArgs._EXCLUDED_TXNS.contains(b.getMethod().getName())) {
				Transaction txn = extractTxn(b);
				LOG.info("Transaction <<" + b.getMethod().getName() + ">> compiled to AR");

				if (txn != null)
					app.addTxn(txn);
			}
		}
		LOG.info("AR application successfully generated");
		return app;
	}

	private Transaction extractTxn(Body b) throws UnknownUnitException {

		if (ConstantArgs.DEBUG_MODE)
			super.printGimpBody(b);
		String name = b.getMethod().getName();
		Transaction txn = new Transaction(name);
		UnitHandler unitHandler = new UnitHandler(b, super.tables);
		// INTERNAL ANALYSIS
		// Parameter extraction
		unitHandler.extractParams();
		LOG.info("Transaction <<" + name + ">> parameters extracted");
		for (Local l : unitHandler.data.getParams().keySet()) {
			Type t = Type.INT; // just to instantiate it, needed for calling the typing function
			Value v = unitHandler.data.getParams().get(l);

			try {
				ParamValExp exp = (ParamValExp) new ParamValExp(l.toString(), t.fromJavaTypes(v), "to-do");
				txn.addParam(l.toString(), exp);
				// Also add it the unit data
				unitHandler.data.addExp(l, exp);
			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}

		unitHandler.InitialAnalysis();
		LOG.info("Initial analysis done");
		unitHandler.extractStatements();
		LOG.info("Statements extracted");
		unitHandler.finalAnalysis();
		LOG.info("Final analysis done");
		unitHandler.finalizeStatements();
		LOG.info("Statements finalized");

		// craft the output transaction from the extracted data
		for (Statement s : unitHandler.data.getStmts()) {
			txn.addStmt(s);
		}
		txn.setExps(unitHandler.data.getExps());
		txn.setTypes();
		// if (ConstantArgs.DEBUG_MODE)
		// printExpressions(unitHandler);
		return txn;

	}

	// just a helping function for dev phase
	private void printExpressions(UnitHandler unitHandler) {
		if (ConstantArgs.DEBUG_MODE) {
			System.out.println("===== LOOPS");
			for (Unit x : unitHandler.data.units)
				if (unitHandler.data.getLoopNo(x) == -1)
					System.out.println("" + unitHandler.data.units.indexOf(x));
				else
					System.out.println(
							"__" + unitHandler.data.units.indexOf(x) + "(" + unitHandler.data.getLoopNo(x) + ")");
		}

		System.out.println("=============================");
		System.out.println("===	VARIABLES");
		for (Value x : unitHandler.data.getExps().keySet()) {
			System.out.println(x + " := " + unitHandler.data.getExps().get(x));
		}
	}

}
