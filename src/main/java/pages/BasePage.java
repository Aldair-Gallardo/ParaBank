package pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import reports.Report;
import utils.Config;

/**
 * Clase base de todos los Page Objects.
 *
 * Centraliza el driver, el reporte y las esperas. Cada acción (click, escribir,
 * validar) se ejecuta con step(): si sale bien registra el paso como EXITOSO con
 * captura; si falla registra el paso FALLIDO con captura y el error, y relanza
 * la excepción para que el test la reciba en su catch.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Report report;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver, Report report) {
        this.driver = driver;
        this.report = report;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
    }

    /** Ruta de la página relativa a base.url: "/index.htm", "/overview.htm"... */
    protected abstract String path();

    /** Navega a la página. */
    public void open() {
        step("Abrir la página " + path(), () -> driver.get(Config.baseUrl() + path()));
    }

    /**
     * Ejecuta una acción como un paso del reporte (aquí está el try/catch de todas las páginas).
     * Ejemplo: step("Aceptar términos", () -> driver.findElement(checkbox).click());
     */
    protected void step(String description, Runnable action) {
        try {
            action.run();
            report.testPassed(description);
        } catch (RuntimeException | AssertionError e) {
            report.testFailed(description, e);
            throw e;
        }
    }

    // ─── Acciones comunes ─────────────────────────────────────────

    /** Espera a que el elemento sea visible y lo devuelve. */
    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Hace clic cuando el elemento se puede presionar. */
    protected void click(By locator, String description) {
        step(description, () -> wait.until(ExpectedConditions.elementToBeClickable(locator)).click());
    }

    /** Escribe en un campo (el valor aparece en el reporte). */
    protected void type(By locator, String value, String description) {
        step(description + ": \"" + value + "\"", () -> {
            WebElement el = visible(locator);
            el.clear();
            el.sendKeys(value);
        });
    }

    /** Escribe sin mostrar el valor en el reporte (contraseñas). */
    protected void typeSecret(By locator, String value, String description) {
        step(description, () -> {
            WebElement el = visible(locator);
            el.clear();
            el.sendKeys(value);
        });
    }

    /** Elige una opción de una lista desplegable (<select>) por su texto visible. */
    protected void select(By locator, String visibleText, String description) {
        step(description + ": \"" + visibleText + "\"", () -> new Select(visible(locator)).selectByVisibleText(visibleText));
    }

    /** Devuelve el texto de un elemento (no se registra como paso). */
    protected String getText(By locator) {
        return visible(locator).getText().trim();
    }

    // ─── Validaciones comunes ─────────────────────────────────────

    /** Valida que el elemento se vea en pantalla. */
    protected void expectVisible(By locator, String description) {
        step(description, () -> visible(locator));
    }

    /** Valida que el elemento contenga el texto esperado. */
    protected void expectText(By locator, String expected, String description) {
        step(description, () -> {
            try {
                wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expected));
            } catch (TimeoutException e) {
                String actual;
                try {
                    actual = driver.findElement(locator).getText();
                } catch (RuntimeException notFound) {
                    actual = "(no se encontró el elemento " + locator + ")";
                }
                throw new AssertionError("Se esperaba el texto \"" + expected + "\" pero se encontró \"" + actual + "\"");
            }
        });
    }

    /** Valida que la URL contenga un fragmento (ej: "overview.htm"). */
    public void expectUrlContains(String fragment, String description) {
        step(description, () -> {
            try {
                wait.until(ExpectedConditions.urlContains(fragment));
            } catch (TimeoutException e) {
                throw new AssertionError("Se esperaba que la URL contenga \"" + fragment + "\" pero es " + driver.getCurrentUrl());
            }
        });
    }
}