package com.cruise.pages;

import com.cruise.utils.TestData;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final JavascriptExecutor js;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(TestData.timeout()));
        this.js = (JavascriptExecutor) driver;

        driver.manage().window().maximize();
    }

    public String bodyText() {
        Object o = js.executeScript("return document.body ? document.body.innerText : '';");
        return o == null ? "" : o.toString();
    }

    public String flatText() {
        return bodyText().replaceAll("\\s+", " ").trim();
    }

    protected void waitForBodyText(String text) {
        wait.until(d -> bodyText().toLowerCase().contains(text.toLowerCase()));
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("Invalid date|(?<![A-Za-z])NaN(?![a-z])");

    protected void waitForRealData() {
        try {
            wait.until(d -> !PLACEHOLDER.matcher(bodyText()).find());
        } catch (TimeoutException ignored) {
        }
    }

    protected By byExactText(String text) {
        return By.xpath("//*[not(self::script or self::style)][normalize-space(.)='" + text
                + "'][not(*[normalize-space(.)='" + text + "'])]");
    }

    protected By byContainsText(String text) {
        return By.xpath("//*[not(self::script or self::style)][contains(normalize-space(.),'" + text
                + "')][not(*[contains(normalize-space(.),'" + text + "')])]");
    }

    protected By insideExactText(String text) {
        return By.xpath(".//*[normalize-space(.)='" + text + "'][not(*[normalize-space(.)='" + text + "'])]");
    }

    protected void safeClick(WebElement el) throws ElementClickInterceptedException {
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(el));
            el.click();
        } catch (ElementNotInteractableException | TimeoutException e) {
            js.executeScript("arguments[0].click();", el);
        }
    }

   
    protected boolean clickAndSwitch(WebElement el) {
        Set<String> before = new HashSet<>(driver.getWindowHandles());
        safeClick(el);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(6)).until(d -> d.getWindowHandles().size() > before.size());
        } catch (TimeoutException e) {
            return false;
        }
        for (String h : driver.getWindowHandles()) {
            if (!before.contains(h)) {
                driver.switchTo().window(h);
                break;
            }
        }
        return true;
    }
}