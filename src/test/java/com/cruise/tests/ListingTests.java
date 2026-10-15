package com.cruise.tests;

import com.cruise.model.Sailing;
import com.cruise.pages.ListingPage;
import com.cruise.utils.ReportManager;
import com.cruise.utils.TestData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.temporal.ChronoUnit;
import java.util.List;

/** S1, S2, S3 - checks on the listing page. */
public class ListingTests extends BaseTest {
	private ListingPage listing;

	@BeforeMethod(alwaysRun = true)
	public void openListing() {
		listing = new ListingPage(driver);
		listing.open();
	}

	@Test(description = "S1 - count in heading equals number of cards")
	public void s1_listingLoads() {
		listing.loadAllCards();
		int headingCount = listing.resultCount();
		int cards = listing.cards().size();
		ReportManager.info("Heading count = " + headingCount + ", cards on page = " + cards);
		org.testng.Assert.assertTrue(headingCount >= 0, "Could not read 'Cruise Search Results(N)'");
		org.testng.Assert.assertEquals(cards, headingCount, "[S1] sailing cards vs heading count");
	}

	@Test(description = "S2 - EMI price > 0 and sorting")
	public void s2_priceCheckAndSorting() {
		SoftAssert soft = new SoftAssert();

		List<Sailing> all = listing.allSailings();
		for (Sailing s : all) {
			soft.assertTrue(s.price > 0, "[S2] price must be > 0 for: " + s.title + " (read " + s.price + ")");
		}

		listing.sortBy(TestData.label("sortLowHigh"));
		List<Sailing> asc = listing.allSailings();
		for (int i = 1; i < asc.size(); i++) {
			soft.assertTrue(asc.get(i).price >= asc.get(i - 1).price,
					"[S2] Low->High broke at #" + i + ": " + asc.get(i - 1).price + " then " + asc.get(i).price);
		}

		listing.sortBy(TestData.label("sortHighLow"));
		List<Sailing> desc = listing.allSailings();
		for (int i = 1; i < desc.size(); i++) {
			soft.assertTrue(desc.get(i).price <= desc.get(i - 1).price,
					"[S2] High->Low broke at #" + i + ": " + desc.get(i - 1).price + " then " + desc.get(i).price);
		}

		listing.sortBy(TestData.label("sortEarliest"));
		List<Sailing> early = listing.allSailings();
		for (int i = 1; i < early.size(); i++) {
			Sailing a = early.get(i - 1);
			Sailing b = early.get(i);
			if (a.start == null || b.start == null) {
				soft.fail("[S2] start date missing at #" + i);
				continue;
			}
			soft.assertFalse(b.start.isBefore(a.start),
					"[S2] Earliest Date broke at #" + i + ": " + a.start + " then " + b.start);
		}
		soft.assertAll();
	}

	@Test(description = "S3 - end date minus start date equals N in 'N-Night'")
	public void s3_cardDates() {
		SoftAssert soft = new SoftAssert();
		for (Sailing s : listing.allSailings()) {
			if (s.start == null || s.end == null || s.nights < 0) {
				soft.fail("[S3] could not read dates/nights for: " + s.title + " | " + s.rawText);
				continue;
			}
			long days = ChronoUnit.DAYS.between(s.start, s.end);
			soft.assertEquals(days, (long) s.nights, "[S3] " + s.title + " (" + s.start + " -> " + s.end + ")");
		}
		soft.assertAll();
	}
}
