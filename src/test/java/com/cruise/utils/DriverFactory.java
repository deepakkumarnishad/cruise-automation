package com.cruise.utils;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

public final class DriverFactory {
	private DriverFactory() {
	}

	private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

	public static WebDriver create(String browser) {
		boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));
		WebDriver d;
		if ("firefox".equalsIgnoreCase(browser)) {
			FirefoxOptions o = new FirefoxOptions();
			if (headless)
				o.addArguments("-headless");
			d = new FirefoxDriver(o);
		} else {
			ChromeOptions o = new ChromeOptions();
			if (headless)
				o.addArguments("--headless=new");
			o.addArguments("--window-size=1920,1080", "--disable-notifications");
			d = new ChromeDriver(o);
		}
		d.manage().window().setSize(new Dimension(1920, 1080));
		d.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
		DRIVER.set(d);
		return d;
	}

	public static WebDriver get() {
		return DRIVER.get();
	}

	public static void quit() {
		WebDriver d = DRIVER.get();
		if (d != null) {
			d.quit();
			DRIVER.remove();
		}
	}
}
