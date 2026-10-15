package com.cruise.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.cruise.utils.DriverFactory;
import com.cruise.utils.ReportManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.IExecutionListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestListener implements ITestListener, IExecutionListener {

    @Override
    public void onTestStart(ITestResult r) {
        String name = r.getMethod().getMethodName();
        Object[] p = r.getParameters();
        if (p != null && p.length > 0) name += " [" + p[0] + "]";
        String browser = r.getTestContext().getCurrentXmlTest().getParameter("browser");
        if (browser != null) name += " (" + browser + ")";
        ReportManager.start(name);
    }

    @Override
    public void onTestSuccess(ITestResult r) {
        if (ReportManager.get() != null) ReportManager.get().pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult r) {
        ExtentTest t = ReportManager.get();
        if (t == null) return;
        t.fail(r.getThrowable());
        WebDriver d = DriverFactory.get();
        if (d == null) return;
        try {
            String b64 = ((TakesScreenshot) d).getScreenshotAs(OutputType.BASE64);
            t.addScreenCaptureFromBase64String(b64, "Screenshot on failure");
            File png = ((TakesScreenshot) d).getScreenshotAs(OutputType.FILE);
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            String file = r.getMethod().getMethodName().replaceAll("[^A-Za-z0-9]", "_")
                    + "_" + System.currentTimeMillis() + ".png";
            Files.copy(png.toPath(), dir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            t.warning("Could not take screenshot: " + e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult r) {
        if (ReportManager.get() != null) ReportManager.get().skip("Skipped: " + r.getThrowable());
    }

    @Override
    public void onExecutionFinish() {
        ReportManager.flush();
    }
}
