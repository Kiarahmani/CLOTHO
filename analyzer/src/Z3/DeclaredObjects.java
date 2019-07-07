package Z3;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.microsoft.z3.*;

import cons.ConstantArgs;

public class DeclaredObjects {

	private Map<String, Sort> sorts;
	private Map<String, Symbol> symbols;
	private Map<String, FuncDecl> funcs;
	private Map<String, DatatypeSort> datatypes;
	private Map<String, BoolExpr> assertions;
	private Map<String, Map<String, FuncDecl>> constructors;
	PrintWriter printer;

	public void addConstructor(String type, String cnstrctrName, FuncDecl cnstrctr) {
		if (this.constructors.get(type) == null)
			this.constructors.put(type, new HashMap<String, FuncDecl>());
		Map<String, FuncDecl> map = this.constructors.get(type);
		map.put(cnstrctrName, cnstrctr);
		this.constructors.put(type, map);
	}

	public Map<String, FuncDecl> getAllNextVars() {
		List<String> allKeys = funcs.keySet().stream().filter(x -> x.contains("next")).collect(Collectors.toList());
		Map<String, FuncDecl> results = new HashMap<>();
		for (String key : allKeys)
			results.put(key, funcs.get(key));
		return results;
	}

	public Map<String, FuncDecl> getAllTTypes() {
		return this.constructors.get("TType");
	}

	public Map<String, FuncDecl> getAllOTypes() {
		return this.constructors.get("OType");
	}

	public FuncDecl getConstructor(String type, String cnstrctrName) {
		return this.constructors.get(type).get(cnstrctrName);
	}

	public void addSort(String key, Sort value) {
		sorts.put(key, value);
		LogZ3("(declare-sort " + value.toString() + ")");
	}

	public void addSymbol(String key, Symbol value) {
		symbols.put(key, value);
	}

	public void addFunc(String key, FuncDecl value) {
		funcs.put(key, value);
		LogZ3(value.toString());
	}

	public void addDataType(String key, DatatypeSort value) {
		datatypes.put(key, value);
		LogZ3(key);
		String s = "";
		for (FuncDecl x : value.getConstructors()) {
			this.addConstructor(key, x.getName().toString(), x);
			s += ("	" + x + "\n");
		}
		LogZ3(s);
	}

	public void addAssertion(String key, BoolExpr value) {
		assertions.put(key, value);
		LogZ3(value.toString());
	}

	public Sort getSort(String key) {
		return sorts.get(key);
	}

	public Symbol getSymbol(String key) {
		return symbols.get(key);
	}

	public FuncDecl getfuncs(String key) {
		return funcs.get(key);
	}

	public DatatypeSort getDataTypes(String key) {
		return datatypes.get(key);
	}

	public BoolExpr getAssertions(String key) {
		return assertions.get(key);
	}

	private void LogZ3(String s) {
		if (ConstantArgs._LOG_ASSERTIONS) {
			printer.append(s + "\n");
			printer.flush();
		}
	}

	public DeclaredObjects(PrintWriter printer) {
		sorts = new HashMap<String, Sort>();
		symbols = new HashMap<String, Symbol>();
		funcs = new HashMap<String, FuncDecl>();
		datatypes = new HashMap<String, DatatypeSort>();
		assertions = new HashMap<String, BoolExpr>();
		this.constructors = new HashMap<String, Map<String, FuncDecl>>();
		this.printer = printer;
	}

}
