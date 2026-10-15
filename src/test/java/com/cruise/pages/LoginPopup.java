package com.cruise.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPopup extends BasePage {

	private static final String MOBILE_COND = "[@type='tel' or contains(translate(@placeholder,'MOBILE','mobile'),'mobile')"
			+ " or contains(translate(@name,'MOBILE','mobile'),'mobile')]";

	private static final String OTP_COND = "[contains(translate(normalize-space(.),'getop','GETOP'),'GET OTP')]"
			+ "[not(*[contains(translate(normalize-space(.),'getop','GETOP'),'GET OTP')])]";

	private static final String DIALOG = "//*[@role='dialog' or @aria-modal='true'"
			+ " or contains(translate(@class,'MODALPUP','modalpup'),'modal')"
			+ " or contains(translate(@class,'MODALPUP','modalpup'),'popup')]";

	private static final By MOBILE_FIELD = By.xpath("//input" + MOBILE_COND);
	private static final By DIALOG_FIELD = By.xpath(DIALOG + "//input" + MOBILE_COND);

	private static final By GET_OTP = By.xpath("//*[self::button or self::a or self::div or self::span]" + OTP_COND);
	private static final By DIALOG_OTP = By
			.xpath(DIALOG + "//*[self::button or self::a or self::div or self::span]" + OTP_COND);

	private static final By CLOSE = By.xpath(
			"//*[@role='dialog']//*[@aria-label='Close' or @aria-label='close' or normalize-space(.)='\u00D7' or normalize-space(.)='X']"
					+ " | //button[@aria-label='Close' or @aria-label='close']");

	private static final By DIALOG_CLOSE = By.xpath(DIALOG
			+ "//*[contains(translate(@aria-label,'CLOSE','close'),'close')"
			+ " or contains(translate(@alt,'CLOSE','close'),'close')"
			+ " or contains(translate(@class,'CLOSE','close'),'close')"
			+ " or normalize-space(.)='\u00D7' or normalize-space(.)='\u2715' or normalize-space(.)='X' or normalize-space(.)='x']");

	private final int baseFields;
	private final int baseOtp;

	public LoginPopup(WebDriver driver) {
		super(driver);
		baseFields = visibleCount(MOBILE_FIELD);
		baseOtp = visibleCount(GET_OTP);
	}

	private int visibleCount(By by) {
		int n = 0;
		for (WebElement e : driver.findElements(by)) {
			try {
				if (e.isDisplayed())
					n++;
			} catch (StaleElementReferenceException ignored) {
			}
		}
		return n;
	}

	private boolean popupFieldVisible() {
		return visibleCount(DIALOG_FIELD) > 0 || visibleCount(MOBILE_FIELD) > baseFields;
	}

	private boolean popupOtpVisible() {
		return visibleCount(DIALOG_OTP) > 0 || visibleCount(GET_OTP) > baseOtp;
	}

	public void waitDisplayed() {
		wait.until(d -> popupFieldVisible());
	}

	public boolean isMobileFieldDisplayed() {
		return popupFieldVisible();
	}

	public boolean isGetOtpDisplayed() {
		return popupOtpVisible();
	}

	private boolean waitClosed(int seconds) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(d -> !popupFieldVisible());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	private void clickFirstVisible(By by) {
		List<WebElement> l = driver.findElements(by);
		for (WebElement c : l) {
			try {
				if (c.isDisplayed()) {
					safeClick(c);
					return;
				}
			} catch (StaleElementReferenceException ignored) {
			}
		}
	}

	public void close() {
		clickFirstVisible(CLOSE);
		if (waitClosed(5))
			return;
		clickFirstVisible(DIALOG_CLOSE);
		if (waitClosed(5))
			return;
		new Actions(driver).sendKeys(Keys.ESCAPE).perform();
		if (waitClosed(5))
			return;
		driver.navigate().refresh();
	}
}