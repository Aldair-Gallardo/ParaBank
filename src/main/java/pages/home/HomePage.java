package pages.home;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.BasePage;
import reports.Report;

/** Page Object de la página de inicio (WebSite/home.html). */
public class HomePage extends BasePage {

    private final By bienvenida = By.id("welcome");
    private final By tablaCuentas = By.cssSelector("table[aria-label='Mis cuentas']");
    private final By botonCerrarSesion = By.id("logout");

    public HomePage(WebDriver driver, Report report) {
        super(driver, report);
    }

    @Override
    protected String path() {
        return "/home.html";
    }

    public void validarBienvenida(String usuario) {
        expectText(bienvenida, "Bienvenido, " + usuario, "Validar bienvenida para \"" + usuario + "\"");
    }

    public void validarTablaCuentas() {
        expectVisible(tablaCuentas, "Validar que se muestra la tabla de cuentas");
    }

    public void cerrarSesion() {
        click(botonCerrarSesion, "Cerrar sesión");
    }
}
