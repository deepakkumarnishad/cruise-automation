package com.cruise.utils;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class TestData {
	private TestData() {
	}

	public static class Rule {
		public String name;
		public List<String> keywords;
		public boolean wholeLine;

		public boolean matches(String cardText) {
			if (wholeLine) {
				for (String line : cardText.split("\\R")) {
					for (String k : keywords) {
						if (line.trim().equalsIgnoreCase(k))
							return true;
					}
				}
				return false;
			}
			for (String k : keywords) {
				Pattern p = Pattern.compile("(?<![A-Za-z])" + Pattern.quote(k) + "(?![A-Za-z])",
						Pattern.CASE_INSENSITIVE);
				if (p.matcher(cardText).find())
					return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class Root {
		String baseUrl;
		int timeoutSeconds;
		int minCapacity;
		int maxCapacity;
		Map<String, String> labels;
		List<Rule> sailingRules;
	}

	private static final Root ROOT;

	static {
		try (InputStreamReader r = new InputStreamReader(TestData.class.getResourceAsStream("/testdata.json"),
				StandardCharsets.UTF_8)) {
			ROOT = new Gson().fromJson(r, Root.class);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static String baseUrl() {
		return ROOT.baseUrl;
	}

	public static int timeout() {
		return ROOT.timeoutSeconds;
	}

	public static int minCapacity() {
		return ROOT.minCapacity;
	}

	public static int maxCapacity() {
		return ROOT.maxCapacity;
	}

	public static List<Rule> rules() {
		return ROOT.sailingRules;
	}

	public static String label(String key) {
		String v = ROOT.labels.get(key);
		if (v == null)
			throw new IllegalArgumentException("Missing label in testdata.json: " + key);
		return v;
	}
}