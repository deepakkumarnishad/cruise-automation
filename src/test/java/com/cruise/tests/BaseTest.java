package com.cruise.tests;

import com.cruise.utils.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {
	protected WebDriver driver;

	@Parameters("browser")
	@BeforeClass(alwaysRun = true)
	public void setUp(@Optional("chrome") String browser) {
		driver = DriverFactory.create(browser);
	}

	@AfterMethod(alwaysRun = true)
	public void resetTabs() {
		if (driver == null)
			return;
		List<String> handles = new ArrayList<>(driver.getWindowHandles());
		for (int i = handles.size() - 1; i > 0; i--) {
			driver.switchTo().window(handles.get(i));
			driver.close();
		}
		driver.switchTo().window(handles.get(0));
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() {
		DriverFactory.quit();
	}
}
