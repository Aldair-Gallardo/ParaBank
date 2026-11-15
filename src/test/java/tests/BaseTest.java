package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import pages.login.LoginPage;
import pages.login.LogoutPage;
import reports.Report;
import reports.ReportExtension;
import utils.DriverFactory;

@ExtendWith(ReportExtension.class)
public abstract class BaseTest implements ReportExtension.HasReport {

    protected WebDriver driver;
    protected Report report;

    // ─── Páginas ───
    protected LoginPage login;
    protected LogoutPage logout;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        driver = DriverFactory.create();
        report = new Report(driver, testInfo.getDisplayName(),
                testInfo.getTestClass().map(Class::getSimpleName).orElse(""), testInfo.getTags());

        login = new LoginPage(driver, report);
        logout = new LogoutPage(driver, report);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
    }

    @Override
    public Report report() {
        return report;
    }
}