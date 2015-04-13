package com.zpush.util;

public class DebugFlag {
	static public boolean debug = false;
	static {
		String debugString = System.getProperty("zpush.debug");
		if (debugString != null) {
			if (debugString.equals("1") || debugString.equalsIgnoreCase("true")) {
				debug = true;
			}
		}
	}
}
