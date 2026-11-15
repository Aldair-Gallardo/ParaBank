package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import pages.home.HomePage;
import pages.login.LoginPage;
import reports.Report;
import reports.ReportExtension;
import utils.DriverFactory;

/**
 * Clase base de TODOS los tests (extiéndela: public class MiTest extends BaseTest).
 *
 * Antes de cada test: abre el navegador, crea el reporte y los Page Objects.
 * Después de cada test: ReportExtension guarda el reporte y aquí se cierra el navegador.
 *
 * Para agregar una página nueva: declara el campo y créalo en setUp().
 */
@ExtendWith(ReportExtension.class)
public abstract class BaseTest implements ReportExtension.HasReport {

    protected WebDriver driver;
    protected Report report;

    // ─── Page Objects ───
    protected LoginPage loginPage;
    protected HomePage homePage;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        driver = DriverFactory.create();
        report = new Report(driver, testInfo.getDisplayName(),
                testInfo.getTestClass().map(Class::getSimpleName).orElse(""), testInfo.getTags());

        loginPage = new LoginPage(driver, report);
        homePage = new HomePage(driver, report);
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
