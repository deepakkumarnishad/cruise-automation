package com.cruise.tests;

import com.cruise.model.Sailing;
import com.cruise.pages.ItineraryPage;
import com.cruise.pages.ListingPage;
import com.cruise.pages.LoginPopup;
import com.cruise.pages.SelectCabinPage;
import com.cruise.utils.ParseUtils;
import com.cruise.utils.ReportManager;
import com.cruise.utils.TestData;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * S4 - S7 for 3 sailings.
 */
public class SailingFlowTests extends BaseTest {

	@DataProvider(name = "sailings")
	public Object[][] sailings() {
		List<TestData.Rule> rules = TestData.rules();
		Object[][] data = new Object[rules.size()][1];
		for (int i = 0; i < rules.size(); i++)
			data[i][0] = rules.get(i);
		return data;
	}

	private static class Ctx {
		ListingPage.CardHandle handle;
		boolean itineraryInNewTab;
		boolean cabinInNewTab;
	}

	@Test(dataProvider = "sailings")
	public void sailingFlow(TestData.Rule rule) {
		final SoftAssert soft = new SoftAssert();
		final Ctx ctx = new Ctx();
		final String mainTab = driver.getWindowHandle();
		final ListingPage listing = new ListingPage(driver);

		listing.open();
		ctx.handle = listing.findFirstMatching(rule);
		final Sailing s = ctx.handle.sailing;
		ReportManager.info("<b>Picked by rule '" + rule.name + "':</b> " + s);
		ReportManager.info("Card text: <pre>" + s.rawText + "</pre>");

		soft.assertNotNull(s.title, "[S4] title not read from card");
		soft.assertNotNull(s.start, "[S4] start date not read from card");
		soft.assertNotNull(s.end, "[S4] end date not read from card");
		soft.assertTrue(s.price > 0, "[S4] EMI price not read from card");

		// S4: Listing -> Itinerary
		step(soft, "S4 Listing -> Itinerary", () -> {
			ctx.itineraryInNewTab = listing.clickViewItinerary(ctx.handle);
			ItineraryPage it = new ItineraryPage(driver);
			it.waitLoaded();

			ReportManager.info("Itinerary page text: <pre>" + it.rawText() + "</pre>");

			soft.assertTrue(it.monthlyPrices().contains(s.price),
					"[S4] EMI price " + s.price + " not found on itinerary. Found: " + it.monthlyPrices());
			soft.assertEquals(it.embarkationDate(), s.start, "[S4] embarkation date");
			soft.assertEquals(it.disembarkationDate(), s.end, "[S4] disembarkation date");

			int[] nd = it.nightsDays();
			if (nd == null) {
				soft.fail("[S4] 'XN/YD' not found on itinerary page");
			} else {
				soft.assertEquals(nd[0], s.nights, "[S4] nights in XN/YD");
				soft.assertEquals(nd[1], s.nights + 1, "[S4] days in XN/YD (must be nights + 1)");
			}
			soft.assertTrue(s.ports.isEmpty() ? false : it.missingPorts(s.ports).isEmpty(),
					"[S4] ports missing on itinerary: " + it.missingPorts(s.ports) + " (saved: " + s.ports + ")");
		});

		step(soft, "Back to listing", () -> {
			if (ctx.itineraryInNewTab) {
				driver.close();
				driver.switchTo().window(mainTab);
			} else {
				driver.navigate().back();
				listing.waitLoaded();
			}
			ctx.handle = listing.findFirstMatching(rule);
			soft.assertEquals(ctx.handle.sailing.title, s.title, "[S5] same sailing found again after returning");
		});

		// S5: Book Now -> Select Cabin summary
		final SelectCabinPage cabin = new SelectCabinPage(driver);
		step(soft, "S5 Select Cabin summary", () -> {
			ctx.cabinInNewTab = listing.clickBookNow(ctx.handle);
			cabin.waitLoaded();

			soft.assertTrue(cabin.stepOneIsSelectCabin(), "[S5] stepper must show step 1 = 'Select Cabin'");

			LocalDateTime emb = cabin.embarkationDateTime();
			LocalDateTime dis = cabin.disembarkationDateTime();
			soft.assertNotNull(emb, "[S5] embarkation date + time not found in summary panel");
			soft.assertNotNull(dis, "[S5] disembarkation date + time not found in summary panel");
			LocalDate embDate = cabin.embarkationDate();
			LocalDate disDate = cabin.disembarkationDate();
			soft.assertEquals(embDate, s.start, "[S5] embarkation date vs card");
			soft.assertEquals(disDate, s.end, "[S5] disembarkation date vs card");

			int[] nd = cabin.nightsDays();
			if (nd == null) {
				soft.fail("[S5] 'XN/YD' not found in summary panel");
			} else {
				soft.assertEquals(nd[0], s.nights, "[S5] nights in summary");
				soft.assertEquals(nd[1], s.nights + 1, "[S5] days in summary (nights + 1)");
			}
			soft.assertTrue(!s.ports.isEmpty() && cabin.missingPorts(s.ports).isEmpty(),
					"[S5] ports missing in summary: " + cabin.missingPorts(s.ports) + " (saved: " + s.ports + ")");
			ReportManager.info("Summary panel text: <pre>" + cabin.summaryRawText() + "</pre>");
		});

		// S6: cabin categories
		final List<SelectCabinPage.Category> cats = new java.util.ArrayList<>();
		step(soft, "S6 Cabin categories", () -> {
			cats.addAll(cabin.categories());
			soft.assertFalse(cats.isEmpty(), "[S6] no cabin categories found");

			StringBuilder table = new StringBuilder();
			table.append(String.format("%-35s | %-9s | %-6s | %s%n", "CATEGORY", "CAPACITY", "DECK", "STATUS"));
			for (SelectCabinPage.Category c : cats) {
				table.append(String.format("%-35s | %-9s | %-6s | %s%n", c.name, c.capacityRaw,
						c.deckShown ? "yes" : "NO", c.status()));

				soft.assertTrue(c.name != null && !c.name.isEmpty(), "[S6] category name missing");
				String label = "[S6] '" + c.name + "' ";
				if (c.capacityRaw == null) {
					soft.fail(label + "Max Capacity missing");
				} else {
					boolean whole = c.capacityRaw.matches("\\d+");
					soft.assertTrue(whole, label + "capacity must be a whole number, was " + c.capacityRaw);
					if (whole) {
						int cap = Integer.parseInt(c.capacityRaw);
						soft.assertTrue(cap >= TestData.minCapacity() && cap <= TestData.maxCapacity(),
								label + "capacity must be " + TestData.minCapacity() + "-" + TestData.maxCapacity()
										+ ", was " + cap);
					}
				}
				soft.assertTrue(c.deckShown, label + "deck location not shown");
				if (c.soldOut) {
					soft.assertFalse(c.hasChooseCabin, label + "is Sold Out but still shows Choose Cabin");
				} else {
					soft.assertTrue(c.hasChooseCabin, label + "is available but has no Choose Cabin button");
				}
			}
			ReportManager.info("<b>Category table</b><pre>" + table + "</pre>");
		});

		// S7: login wall (NEVER type anything)
		step(soft, "S7 Login wall", () -> {
			SelectCabinPage.Category first = null;
			for (SelectCabinPage.Category c : cats) {
				if (!c.soldOut && c.hasChooseCabin) {
					first = c;
					break;
				}
			}
			if (first == null) {
				soft.fail("[S7] no available category to click Choose Cabin on");
				return;
			}
			LoginPopup login = new LoginPopup(driver);
			cabin.clickChooseCabin(first);
			login.waitDisplayed();
			soft.assertTrue(login.isMobileFieldDisplayed(), "[S7] mobile number field not shown");
			soft.assertTrue(login.isGetOtpDisplayed(), "[S7] 'Get OTP' button not shown");
			login.close(); // closes only - nothing is typed
		});

		soft.assertAll();
	}

	private void step(SoftAssert soft, String name, Runnable body) {
		ReportManager.info("<b>--- " + name + " ---</b>");
		try {
			body.run();
		} catch (Throwable t) {
			String msg = String.valueOf(t.getMessage()).split("\\R")[0];
			soft.fail("[" + name + "] crashed: " + t.getClass().getSimpleName() + " - " + msg);
		}
	}
}
