package com.cruise.pages;

import com.cruise.utils.ParseUtils;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;

//Page 2: itinerary opened by "View Itinerary".

public class ItineraryPage extends BasePage {

    public ItineraryPage(WebDriver driver) {
        super(driver);
    }

    public void waitLoaded() {
        waitForBodyText("Embarkation");
        waitForRealData();
    }

    public String text() { 
    	
    	return flatText(); }
    
    public String rawText() { 
    	
    	return bodyText(); }

    public LocalDate embarkationDate() { 
    	
    	return ParseUtils.dateAfterLabel(text(), "Embarkation"); }

    public LocalDate disembarkationDate() {
    	
    	return ParseUtils.dateAfterLabel(text(), "Disembarkation"); }

    public int[] nightsDays() { return ParseUtils.extractNightsDays(text()); }

    public List<Integer> monthlyPrices() { return ParseUtils.allMonthlyPrices(text()); }

    public List<String> missingPorts(List<String> ports) { return ParseUtils.missingPorts(text(), ports); }

	
}
