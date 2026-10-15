package com.cruise.pages;

import com.cruise.utils.ParseUtils;
import com.cruise.utils.TestData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Page 3: 
public class SelectCabinPage extends BasePage {

	public static class Category {
		public String name;
		public String capacityRaw; 
		public boolean deckShown;
		public boolean soldOut;
		public boolean hasChooseCabin;
		public WebElement chooseCabinButton;

		public String status() {
			if (soldOut && !hasChooseCabin)
				return "SOLD OUT";
			if (!soldOut && hasChooseCabin)
				return "AVAILABLE";
			return "INCONSISTENT (soldOut=" + soldOut + ", chooseCabin=" + hasChooseCabin + ")";
		}
	}

	private static final String SUMMARY_JS = "var all=document.querySelectorAll('body *');"
			+ "for(var i=0;i<all.length;i++){ var e=all[i];"
			+ " if(e.children.length===0 && /\\d+\\s*N\\s*\\/\\s*\\d+\\s*D/i.test(e.textContent||'')){"
			+ "  var p=e; while(p && p!==document.body){ var t=p.innerText||'';"
			+ "   if(/visiting\\s*ports/i.test(t)) return t;" + "   p=p.parentElement; } } }" + "return null;";

	private static final String CATEGORIES_JS = "var res=[]; var all=document.body.getElementsByTagName('*');"
			+ "var cnt=function(t){return (t.match(/Max\\s*Capacity/gi)||[]).length;};"
			+ "var has=function(c){return /Max\\s*Capacity/i.test(c.textContent||'');};"
			+ "for(var i=0;i<all.length;i++){ var e=all[i];" + " if(!has(e)){continue;}" + " var kid=false;"
			+ " for(var k=0;k<e.children.length;k++){ if(has(e.children[k])){kid=true;break;} }" + " if(kid){continue;}"
			+ " var best=e; var p=e;" + " while(p && p!==document.body){ var t=p.innerText||'';"
			+ "  if(cnt(t)>1 || t.length>1500){break;} best=p; p=p.parentElement; }" + " var dup=false;"
			+ " for(var j=0;j<res.length;j++){ if(res[j]===best){dup=true;break;} }" + " if(!dup){res.push(best);} }"
			+ "return res;";

	public SelectCabinPage(WebDriver driver) {
		super(driver);
	}

	public void waitLoaded() {
		waitForBodyText(TestData.label("stepper"));
		wait.until(d -> flatText().toLowerCase().contains("max capacity"));
		waitForRealData();
	}

	public boolean stepOneIsSelectCabin() {
		return Pattern.compile("(?i)(?:^|\\s)(?:1\\s*)?" + Pattern.quote(TestData.label("stepper"))
				+ "\\s*2\\s*Payment\\s*Summary\\s*3\\s*Payments").matcher(flatText()).find();
	}

	public String summaryText() {
		Object o = js.executeScript(SUMMARY_JS);
		String t = o == null ? bodyText() : o.toString();
		return t.replaceAll("\\s+", " ").trim();
	}

	public String summaryRawText() {
		Object o = js.executeScript(SUMMARY_JS);
		return o == null ? bodyText() : o.toString();
	}

	public LocalDateTime embarkationDateTime() {
		List<LocalDateTime> l = ParseUtils.findAllDateTimes(summaryText());
		return l.size() >= 1 ? l.get(0) : null;
	}

	public LocalDateTime disembarkationDateTime() {
		List<LocalDateTime> l = ParseUtils.findAllDateTimes(summaryText());
		return l.size() >= 2 ? l.get(1) : null;
	}

	public LocalDate embarkationDate() {
		LocalDateTime d = embarkationDateTime();
		return d == null ? null : d.toLocalDate();
	}

	public LocalDate disembarkationDate() {
		LocalDateTime d = disembarkationDateTime();
		return d == null ? null : d.toLocalDate();
	}

	public int[] nightsDays() {
		return ParseUtils.extractNightsDays(summaryText());
	}

	public List<String> missingPorts(List<String> ports) {
		return ParseUtils.missingPorts(summaryText(), ports);
	}

	// categories

	@SuppressWarnings("unchecked")
	public List<Category> categories() {
		List<WebElement> cards = (List<WebElement>) js.executeScript(CATEGORIES_JS);
		List<Category> out = new ArrayList<>();
		for (WebElement c : cards)
			out.add(parseCategory(c));
		return out;
	}

	private Category parseCategory(WebElement card) {
		String raw = card.getText();
		String flat = raw.replaceAll("\\s+", " ");
		Category c = new Category();

		Matcher cap = Pattern.compile("(?i)Max\\s*Capacity\\s*:?\\s*(\\d+(?:\\.\\d+)?)").matcher(flat);
		c.capacityRaw = cap.find() ? cap.group(1) : null;

		c.soldOut = flat.toLowerCase().contains(TestData.label("soldOut").toLowerCase());

		List<WebElement> btns = card.findElements(insideExactText(TestData.label("chooseCabin")));
		for (WebElement b : btns) {
			if (b.isDisplayed()) {
				c.hasChooseCabin = true;
				c.chooseCabinButton = b;
				break;
			}
		}

		String[] lines = raw.split("\\R");
		for (String l : lines) {
			String t = l.trim();
			if (t.isEmpty())
				continue;
			if (t.matches("(?i).*(max capacity|deck|choose cabin|sold out|\u20B9|per person|starting|details).*"))
				continue;
			if (t.matches("[0-9\\s.,]+"))
				continue;
			c.name = t;
			break;
		}

		for (int i = 0; i < lines.length; i++) {
			if (lines[i].toLowerCase().contains("deck")) {
				String rest = lines[i].replaceAll("(?i)deck\\s*(location|no\\.?|number)?\\s*:?", "").trim();
				boolean nextHasText = i + 1 < lines.length && !lines[i + 1].trim().isEmpty()
						&& !lines[i + 1].toLowerCase().contains("max capacity");
				if (!rest.isEmpty() || nextHasText) {
					c.deckShown = true;
					break;
				}
			}
		}
		return c;
	}

	public void clickChooseCabin(Category c) {
		safeClick(c.chooseCabinButton);
	}

	public By chooseCabinLocator() {
		return byExactText(TestData.label("chooseCabin"));
	}
}
