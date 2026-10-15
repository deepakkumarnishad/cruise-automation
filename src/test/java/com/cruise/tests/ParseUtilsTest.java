package com.cruise.tests;

import com.cruise.utils.ParseUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

/** Unit tests of the helper - no browser. */
public class ParseUtilsTest {

	@Test
	public void priceWithRupeeAndComma() {
		Assert.assertEquals(ParseUtils.parsePrice("\u20B96,333 /month"), 6333);
	}

	@Test
	public void priceInsideLongText() {
		Assert.assertEquals(ParseUtils.parsePrice("No-Cost EMI starts at \u20B912,500 /month"), 12500);
	}

	@Test
	public void emiPriceFromCardText() {
		String card = "3-Night Goa Weekend Cruise\nNo-Cost EMI starts at \u20B96,333 /month\nBook Now";
		Assert.assertEquals(ParseUtils.extractEmiPrice(card), 6333);
		Assert.assertEquals(ParseUtils.extractEmiPrice("no price here"), -1);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void priceWithoutNumberThrows() {
		ParseUtils.parsePrice("Sold Out");
	}

	@Test
	public void dateParsing() {
		Assert.assertEquals(ParseUtils.parseDate("Oct 23, 2026"), LocalDate.of(2026, 10, 23));
		Assert.assertEquals(ParseUtils.parseDate("Sept 5, 2026"), LocalDate.of(2026, 9, 5));
	}

	@Test
	public void allDatesInText() {
		Assert.assertEquals(ParseUtils.findAllDates("Oct 23, 2026 - Oct 26, 2026").size(), 2);
	}

	@Test
	public void dateTimeParsing() {
		Assert.assertEquals(ParseUtils.parseDateTime("Oct 23, 2026, 05:00 PM"), LocalDateTime.of(2026, 10, 23, 17, 0));
		Assert.assertEquals(ParseUtils.parseDateTime("Oct 26, 2026, 08:30 AM"), LocalDateTime.of(2026, 10, 26, 8, 30));
		Assert.assertEquals(ParseUtils.parseDateTime("Oct 26, 2026, 12:15 AM"), LocalDateTime.of(2026, 10, 26, 0, 15));
		Assert.assertEquals(ParseUtils.parseDateTime("Oct 26, 2026, 12:15 PM"), LocalDateTime.of(2026, 10, 26, 12, 15));
	}

	@Test
	public void dateAfterLabelDoesNotMixEmbarkAndDisembark() {
		String t = "Embarkation: Oct 23, 2026 Disembarkation: Oct 26, 2026";
		Assert.assertEquals(ParseUtils.dateAfterLabel(t, "Embarkation"), LocalDate.of(2026, 10, 23));
		Assert.assertEquals(ParseUtils.dateAfterLabel(t, "Disembarkation"), LocalDate.of(2026, 10, 26));
	}

	@Test
	public void dateTimeAfterLabel() {
		String t = "Embarkation Oct 23, 2026, 05:00 PM Disembarkation Oct 26, 2026, 08:30 AM";
		Assert.assertEquals(ParseUtils.dateTimeAfterLabel(t, "Embarkation"), LocalDateTime.of(2026, 10, 23, 17, 0));
		Assert.assertEquals(ParseUtils.dateTimeAfterLabel(t, "Disembarkation"), LocalDateTime.of(2026, 10, 26, 8, 30));
	}

	@Test
	public void nightsFromTitle() {
		Assert.assertEquals(ParseUtils.extractNights("3-Night Goa Weekend Cruise"), 3);
		Assert.assertEquals(ParseUtils.extractNights("No nights"), -1);
	}

	@Test
	public void nightsDays() {
		Assert.assertEquals(ParseUtils.extractNightsDays("Mumbai - Goa - Mumbai (3N/4D)"), new int[] { 3, 4 });
		Assert.assertNull(ParseUtils.extractNightsDays("nothing"));
	}

	@Test
	public void portsFromRouteLine() {
		String card = "3-Night Goa Weekend Cruise\nMumbai \u2013 Goa \u2013 \u2026 \u2013 Mumbai, 3N/4D\nOct 23, 2026";
		Assert.assertEquals(ParseUtils.extractPorts(card, "3-Night Goa Weekend Cruise"),
				Arrays.asList("Mumbai", "Goa", "Mumbai"));
	}

	@Test
	public void missingPortsIsCaseInsensitive() {
		Assert.assertTrue(ParseUtils.missingPorts("Mumbai Goa", Arrays.asList("mumbai", "GOA")).isEmpty());
		Assert.assertEquals(ParseUtils.missingPorts("Mumbai", Arrays.asList("Mumbai", "Kochi")),
				Arrays.asList("Kochi"));
	}

	@Test
	public void portsWithBulletAndPrefix() {
		String card = "5-Night Goa & Lakshadweep Cruise\nOct 26, 2026\nOct 31, 2026\nPorts:Mumbai \u2022 Goa \u2022 Agatti \u2022 Mumbai";
		Assert.assertEquals(ParseUtils.extractPorts(card, "5-Night Goa & Lakshadweep Cruise"),
				Arrays.asList("Mumbai", "Goa", "Agatti", "Mumbai"));
	}

	@Test
	public void allDateTimesWithWeekday() {
		String t = "Mumbai Oct 26, 2026 Mon, 04:30 PM ------- Mumbai Oct 31, 2026 Sat, 08:30 AM";
		Assert.assertEquals(ParseUtils.findAllDateTimes(t).size(), 2);
		Assert.assertEquals(ParseUtils.findAllDateTimes(t).get(0), LocalDateTime.of(2026, 10, 26, 16, 30));
		Assert.assertEquals(ParseUtils.findAllDateTimes(t).get(1), LocalDateTime.of(2026, 10, 31, 8, 30));
	}

}
