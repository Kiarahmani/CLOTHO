package fec.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ar.ddl.Column;
import ar.ddl.Table;
import utils.Utils;
import cons.ConstantArgs;
import exceptions.SqlTypeNotFoundException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;

public class DDLParser {

	public ArrayList<Table> parse() {
		BufferedReader in = null;
		ArrayList<String> statements = new ArrayList<String>();
		ArrayList<Table> tables = new ArrayList<Table>();
		// pasre the file
		try {
			in = new BufferedReader(new FileReader(
					"src/benchmarks/" + ConstantArgs._BENCHMARK_NAME + "/" + ConstantArgs._BENCHMARK_NAME + ".sql"));
			String read = null;
			String iter_s = "";
			while ((read = in.readLine()) != null) {
				iter_s += read;
				if (read.contains(";")) {
					statements.add(iter_s);
					iter_s = "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// create statements and later the tables
		try {
			Statement statement;
			for (String s : statements) {
				statement = CCJSqlParserUtil.parse(s);
				try {
					CreateTable createStatement = (CreateTable) statement;
					tables.add(createTable(createStatement));
				} catch (java.lang.ClassCastException e) {
					// e.printStackTrace();
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tables;
	}

	private Table createTable(CreateTable ct) {
		Table t = new Table(ct.getTable().getName());
		List<String> pkList = new ArrayList<String>();
		// filter out non interesting (for now) schema constraints
		for (Index i : ct.getIndexes())
			if (i.getType().equals("PRIMARY KEY"))
				pkList.addAll(i.getColumnsNames());

		for (ColumnDefinition cd : ct.getColumnDefinitions()) {
			try {
				t.addColumn(new Column(cd.getColumnName(), Utils.convertType(cd.getColDataType().getDataType()),
						pkList.contains(cd.getColumnName())));
			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}
		return t;
	}
}

//