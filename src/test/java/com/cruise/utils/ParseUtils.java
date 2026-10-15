package com.cruise.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParseUtils {
	private ParseUtils() {
	}

	public static final String DATE_RE = "[A-Za-z]{3,9}\\.?\\s+\\d{1,2},?\\s+\\d{4}";
	private static final List<String> MONTHS = Arrays.asList("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug",
			"sep", "oct", "nov", "dec");

	private static final Pattern DATE = Pattern.compile("([A-Za-z]{3,9})\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})");
	private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*([AaPp])[Mm]");
	private static final Pattern RUPEE = Pattern.compile("\u20B9\\s*([0-9][0-9,]*)");
	private static final Pattern ANY_NUMBER = Pattern.compile("([0-9][0-9,]*)");
	private static final Pattern EMI = Pattern.compile("(?i)EMI\\s+starts\\s+at\\s*\u20B9?\\s*([0-9][0-9,]*)");
	private static final Pattern MONTHLY = Pattern.compile("(?i)\u20B9\\s*([0-9][0-9,]*)\\s*/\\s*month");
	private static final Pattern NIGHTS = Pattern.compile("(?i)(\\d+)\\s*-?\\s*Night");
	private static final Pattern ND = Pattern.compile("(?i)\\b(\\d+)\\s*N\\s*/\\s*(\\d+)\\s*D\\b");

	public static int parsePrice(String text) {
		if (text == null)
			throw new IllegalArgumentException("price text is null");
		Matcher m = RUPEE.matcher(text);
		if (m.find())
			return toInt(m.group(1));
		m = ANY_NUMBER.matcher(text);
		if (m.find())
			return toInt(m.group(1));
		throw new IllegalArgumentException("No price in: " + text);
	}

	public static int extractEmiPrice(String cardText) {
		Matcher m = EMI.matcher(cardText == null ? "" : cardText);
		return m.find() ? toInt(m.group(1)) : -1;
	}

	public static List<Integer> allMonthlyPrices(String text) {
		List<Integer> out = new ArrayList<>();
		Matcher m = MONTHLY.matcher(text == null ? "" : text);
		while (m.find())
			out.add(toInt(m.group(1)));
		return out;
	}

	private static int toInt(String s) {
		return Integer.parseInt(s.replace(",", ""));
	}

	public static LocalDate parseDate(String text) {
		List<LocalDate> all = findAllDates(text);
		if (all.isEmpty())
			throw new IllegalArgumentException("No date in: " + text);
		return all.get(0);
	}

	public static List<LocalDate> findAllDates(String text) {
		List<LocalDate> out = new ArrayList<>();
		Matcher m = DATE.matcher(text == null ? "" : text);
		while (m.find()) {
			LocalDate d = toDate(m.group(1), m.group(2), m.group(3));
			if (d != null)
				out.add(d);
		}
		return out;
	}

	private static LocalDate toDate(String mon, String day, String year) {
		int idx = MONTHS.indexOf(mon.substring(0, 3).toLowerCase());
		if (idx < 0)
			return null;
		try {
			return LocalDate.of(Integer.parseInt(year), idx + 1, Integer.parseInt(day));
		} catch (Exception e) {
			return null;
		}
	}

	public static LocalDateTime parseDateTime(String text) {
		LocalDate d = parseDate(text);
		Matcher t = TIME.matcher(text);
		if (!t.find())
			throw new IllegalArgumentException("No time in: " + text);
		return LocalDateTime.of(d, toTime(t));
	}

	private static LocalTime toTime(Matcher t) {
		int h = Integer.parseInt(t.group(1)) % 12;
		if (t.group(3).equalsIgnoreCase("p"))
			h += 12;
		return LocalTime.of(h, Integer.parseInt(t.group(2)));
	}

	private static Matcher labelMatcher(String text, String label) {
		String re = "(?i)(?<![A-Za-z])" + label + "(?:\\s*Date)?(?:\\s*&\\s*Time)?\\s*[:\\-]?\\s*(" + DATE_RE + ")"
				+ "(?:\\s*,?\\s*(?:at\\s*)?(\\d{1,2}:\\d{2}\\s*[AaPp][Mm]))?";
		return Pattern.compile(re).matcher(text == null ? "" : text);
	}

	public static LocalDate dateAfterLabel(String text, String label) {
		Matcher m = labelMatcher(text, label);
		if (!m.find())
			return null;
		List<LocalDate> d = findAllDates(m.group(1));
		return d.isEmpty() ? null : d.get(0);
	}

	public static LocalDateTime dateTimeAfterLabel(String text, String label) {
		Matcher m = labelMatcher(text, label);
		if (!m.find() || m.group(2) == null)
			return null;
		return parseDateTime(m.group(1) + ", " + m.group(2));
	}

	public static int extractNights(String title) {
		Matcher m = NIGHTS.matcher(title == null ? "" : title);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	public static int[] extractNightsDays(String text) {
		Matcher m = ND.matcher(text == null ? "" : text);
		return m.find() ? new int[] { Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) } : null;
	}

	public static List<String> extractPorts(String text, String title) {
		if (text == null)
			return new ArrayList<>();
		for (String raw : text.split("\\R")) {
			String line = raw.trim();
			if (line.isEmpty() || line.equals(title))
				continue;
			if (!findAllDates(line).isEmpty())
				continue;
			boolean hasSep = line.contains("\u2013") || line.contains("\u2014") || line.contains("\u2192")
					|| line.contains("\u2022") || line.contains(" - ") || line.contains(" > ");
			if (!hasSep)
				continue;
			line = line.replaceFirst("(?i)^\\s*ports?\\s*:\\s*", "");
			line = line.replaceAll("(?i)[,(]?\\s*\\d+\\s*N\\s*/\\s*\\d+\\s*D\\s*\\)?", "");
			List<String> ports = new ArrayList<>();
			for (String p : line.split("\\s*[\u2013\u2014\u2192\u2022]\\s*|\\s+-\\s+|\\s+>\\s+")) {
				p = p.replace("\u2026", "").replace("...", "").trim();
				if (!p.isEmpty())
					ports.add(p);
			}
			if (ports.size() >= 2)
				return ports;
		}
		return new ArrayList<>();
	}

	public static List<LocalDateTime> findAllDateTimes(String text) {
		List<LocalDateTime> out = new ArrayList<>();
		Pattern p = Pattern
				.compile("(" + DATE_RE + ")\\s*,?\\s*(?:[A-Za-z]{3,9}\\s*,\\s*)?(\\d{1,2}:\\d{2}\\s*[AaPp][Mm])");
		Matcher m = p.matcher(text == null ? "" : text);
		while (m.find()) {
			try {
				out.add(parseDateTime(m.group(1) + ", " + m.group(2)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return out;
	}

	public static List<String> missingPorts(String pageText, List<String> ports) {
		List<String> missing = new ArrayList<>();
		String t = pageText == null ? "" : pageText.toLowerCase();
		for (String p : ports)
			if (!t.contains(p.toLowerCase()))
				missing.add(p);
		return missing;
	}
}
