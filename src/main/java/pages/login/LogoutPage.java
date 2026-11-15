package pages.login;

import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import reports.Report;
import utils.Config;

/** Cierre de sesión de ParaBank. */
public class LogoutPage {

    /** Navegador. */
    private final WebDriver driver;

    /** Reporte del caso (pasos, capturas y errores). */
    private final Report report;

    /** Espera hasta que los elementos estén listos (timeout.seconds de config.properties). */
    private final WebDriverWait wait;

    // ─── Localizadores ───

    /** Enlace Log Out (menú izquierdo, visible solo con sesión iniciada). */
    @FindBy(xpath = "//a[@href='logout.htm']")
    private WebElement btnLogout;

    /** Campo usuario: si aparece después del logout, la sesión se cerró. */
    @FindBy(xpath = "//*[@name='username']")
    private WebElement campoUsuario;

    public LogoutPage(WebDriver driver, Report report) {
        this.driver = driver;
        this.report = report;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
        PageFactory.initElements(driver, this);
    }

    /** Cierra la sesión y valida que vuelve al formulario de login. */
    public void cerrarSesion() {
        try {

            // 1. Clic en Log Out
            try {
                wait.until(ExpectedConditions.elementToBeClickable(btnLogout)).click();
                report.testPassed("Se hizo clic en Log Out.");
            } catch (Exception e) {
                report.testFailed("No se pudo hacer clic en Log Out.", e);
                throw e;
            }

            // 2. Validar que volvió al login
            try {
                wait.until(ExpectedConditions.visibilityOf(campoUsuario));
                report.testPassed("Se cerró la sesión: se muestra el formulario de login.");
            } catch (Exception e) {
                report.testFailed("La sesión no se cerró: no apareció el formulario de login.", e);
                throw e;
            }

        } catch (Exception e) {
            report.testFailed("El flujo de logout terminó con error.", e);
            throw e;
        }
    }
}