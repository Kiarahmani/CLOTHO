package com.github.kiarahmani.replayer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Transaction {
	private Client client;
	private int id;
	private String methodName;
	private ArrayList<Class> argTypes = new ArrayList<Class>();
	private ArrayList<Object> args = new ArrayList<Object>();
	private Class[] argTypesArray;
	private Object[] argsArray;

	// constructor
	public Transaction(int id) {
		this.id = id;
		client = new Client(id);
		// read the test config file
		JSONParser jsonParser = new JSONParser();
		try (FileReader reader = new FileReader("../tests/instance.json")) {
			// Read JSON file
			Object obj = jsonParser.parse(reader);
			JSONArray txnList = (JSONArray) obj;
			for (Object txnObj : txnList) {
				JSONObject txn = (JSONObject) txnObj;
				JSONObject config = (JSONObject) txn.get(String.valueOf(this.id));
				if (config != null) {
					methodName = (String) config.get("methodName");
					for (Object o : (JSONArray) config.get("args")) {
						argTypes.add(o.getClass());
						args.add(o);
					}
					argsArray = new Object[args.size()];
					argTypesArray = new Class[argTypes.size()];
					for (int i = 0; i < argTypes.size(); i++) {
						argTypesArray[i] = argTypes.get(i);
						argsArray[i] = args.get(i);
					}
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	// run the transaction wrapper
	public void run() {

		System.out.println("\n\n\n\n=============================================================");
		System.out.println("@R" + id + "    " + methodName + "(" + args + ")");
		System.out.println("=============================================================\n");

		// invoke the req
		try {
			Method method = client.getClass().getMethod(methodName, argTypesArray);
			method.invoke(client, argsArray);
		} catch (NoSuchMethodException e2) {
			System.err.println("Unknown Transaction Name: " + methodName);
			e2.printStackTrace();
		} catch (SecurityException e2) {
			e2.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		System.out.println("\n=============================================================\n\n\n\n\n\n\n");
	}

}
