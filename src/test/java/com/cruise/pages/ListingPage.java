package com.cruise.pages;

import com.cruise.model.Sailing;
import com.cruise.utils.ParseUtils;
import com.cruise.utils.TestData;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Page 1: 

public class ListingPage extends BasePage {

	public static class CardHandle {
		public final WebElement card;
		public final Sailing sailing;

		CardHandle(WebElement card, Sailing sailing) {
			this.card = card;
			this.sailing = sailing;
		}
	}

	private static final String FIND_CARDS_JS = "var label=arguments[0]; var res=[]; var all=document.body.getElementsByTagName('*');"
			+ "var cnt=function(t){return t.toLowerCase().split(label).length-1;};"
			+ "for(var i=0;i<all.length;i++){ var e=all[i];"
			+ " if((e.textContent||'').trim().toLowerCase()!==label){continue;}" + " var kidSame=false;"
			+ " for(var k=0;k<e.children.length;k++){ if((e.children[k].textContent||'').trim().toLowerCase()===label){kidSame=true;break;} }"
			+ " if(kidSame){continue;}" + " var p=e; var card=null;"
			+ " while(p && p!==document.body){ var t=p.innerText||'';"
			+ "  if(/\\d+\\s*-?\\s*Night/i.test(t) && /EMI/i.test(t)){ card=p; break; }" + "  p=p.parentElement; }"
			+ " if(!card){continue;}" + " while(card.parentElement && card.parentElement!==document.body){"
			+ "  var pt=card.parentElement.innerText||'';" + "  if(cnt(pt)!==1 || pt.length>2500){break;}"
			+ "  card=card.parentElement; }" + " var dup=false;"
			+ " for(var j=0;j<res.length;j++){ if(res[j]===card){dup=true;break;} }" + " if(!dup){res.push(card);} }"
			+ "return res;";

	public ListingPage(WebDriver driver) {
		super(driver);
	}

	public void open() {
		driver.get(TestData.baseUrl());
		waitLoaded();
	}

	public void waitLoaded() {
		waitForBodyText(TestData.label("resultsHeading"));
		wait.until(d -> !cards().isEmpty());
	}

	@SuppressWarnings("unchecked")
	public List<WebElement> cards() {
		java.util.function.Function<WebDriver, List<WebElement>> find = d -> (List<WebElement>) js
				.executeScript(FIND_CARDS_JS, TestData.label("viewItinerary").toLowerCase());
		FluentWait<WebDriver> fw = new FluentWait<WebDriver>(driver).withTimeout(Duration.ofSeconds(10))
				.pollingEvery(Duration.ofMillis(500)).ignoring(JavascriptException.class);
		try {
			return fw.until(find);
		} catch (TimeoutException e) {

			driver.navigate().refresh();
			waitForBodyText(TestData.label("resultsHeading"));
			return fw.until(find);
		}
	}

	private String matchText(WebElement card) {
		Object o = js.executeScript("var r=[]; var n=arguments[0].getElementsByTagName('*');"
				+ "for(var i=0;i<n.length;i++){ var a=n[i].getAttribute('alt')||n[i].getAttribute('title')||n[i].getAttribute('aria-label');"
				+ " if(a){r.push(a);} }" + "return r.join('\\n');", card);
		return card.getText() + "\n" + (o == null ? "" : o.toString());
	}

	public void loadAllCards() {
		final int[] last = { -1 };
		final int[] stable = { 0 };
		new WebDriverWait(driver, Duration.ofSeconds(120), Duration.ofSeconds(1)).until(d -> {
			int n = cards().size();
			js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
			if (n == last[0]) {
				stable[0]++;
			} else {
				stable[0] = 0;
				last[0] = n;
			}
			return stable[0] >= 3;
		});

		js.executeScript("window.scrollTo(0, 0);");
	}

	// ---------------------------------------------------------------- reading
	// values

	public int resultCount() {
		Matcher m = Pattern
				.compile("(?i)" + Pattern.quote(TestData.label("resultsHeading")) + "\\s*\\(\\s*(\\d+)\\s*\\)")
				.matcher(flatText());
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}

	public List<Sailing> allSailings() {
		loadAllCards();
		List<Sailing> out = new ArrayList<>();
		for (WebElement c : cards())
			out.add(parseCard(c));
		return out;
	}

	public Sailing parseCard(WebElement card) {
		String text = card.getText();
		Sailing s = new Sailing();
		s.rawText = text;
		for (String raw : text.split("\\R")) {
			String line = raw.trim();
			if (ParseUtils.extractNights(line) > 0 && !line.toLowerCase().contains("emi")) {
				s.title = line;
				s.nights = ParseUtils.extractNights(line);
				break;
			}
		}
		List<java.time.LocalDate> dates = ParseUtils.findAllDates(text);
		if (dates.size() >= 1)
			s.start = dates.get(0);
		if (dates.size() >= 2)
			s.end = dates.get(1);
		s.price = ParseUtils.extractEmiPrice(text);
		s.ports = ParseUtils.extractPorts(text, s.title);
		return s;
	}

	public CardHandle findFirstMatching(TestData.Rule rule) {
		final int[] last = { -1 };
		final int[] stable = { 0 };
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(120), Duration.ofSeconds(1))
					.ignoring(StaleElementReferenceException.class).until(d -> {
						List<WebElement> cs = cards();
						for (WebElement c : cs) {

							if (rule.matches(matchText(c)))
								return new CardHandle(c, parseCard(c));
						}
						js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
						if (cs.size() == last[0]) {
							stable[0]++;
						} else {
							stable[0] = 0;
							last[0] = cs.size();
						}
						if (stable[0] >= 3) {
							throw new IllegalStateException("No sailing on the page matches rule: " + rule.name);
						}
						return null;
					});
		} catch (TimeoutException e) {
			throw new IllegalStateException("Timed out looking for sailing: " + rule.name, e);
		}
	}

	public boolean clickViewItinerary(CardHandle h) {
		return clickAndSwitch(h.card.findElement(insideExactText(TestData.label("viewItinerary"))));
	}

	public boolean clickBookNow(CardHandle h) {
		return clickAndSwitch(h.card.findElement(insideExactText(TestData.label("bookNow"))));
	}

	public void sortBy(String optionLabel) {
		List<WebElement> before = cards();
		WebElement firstBefore = before.isEmpty() ? null : before.get(0);

		boolean optionVisible = driver.findElements(byExactText(optionLabel)).stream()
				.anyMatch(WebElement::isDisplayed);
		if (!optionVisible) {
			js.executeScript("window.scrollTo(0, 0);");
			WebElement trigger = wait
					.until(ExpectedConditions.presenceOfElementLocated(byContainsText(TestData.label("sortTrigger"))));
			safeClick(trigger);
		}
		WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(byExactText(optionLabel)));
		safeClick(option);

		if (firstBefore != null) {
			try {
				new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.stalenessOf(firstBefore));
			} catch (TimeoutException ignored) {
			}
		}
		loadAllCards();
	}
}
