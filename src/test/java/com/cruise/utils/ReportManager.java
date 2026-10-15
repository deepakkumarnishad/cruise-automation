package com.cruise.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/** Single HTML report: target/reports/ExtentReport.html */
public final class ReportManager {
    private ReportManager() { }

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private static synchronized ExtentReports extent() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("target/reports/ExtentReport.html");
            spark.config().setReportName("Cordelia Cruises - Booking Flow");
            spark.config().setDocumentTitle("Cruise Booking Flow Report");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        return extent;
    }

    public static void start(String name) { TEST.set(extent().createTest(name)); }

    public static ExtentTest get() { return TEST.get(); }

    /** log a step (HTML allowed) to console + report */
    public static void info(String msg) {
        System.out.println(msg);
        if (TEST.get() != null) TEST.get().info(msg);
    }

    public static synchronized void flush() {
        if (extent != null) extent.flush();
    }
}
