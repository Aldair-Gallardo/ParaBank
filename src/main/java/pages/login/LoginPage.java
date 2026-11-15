package pages.login;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.BasePage;
import reports.Report;

/**
 * Page Object del login (WebSite/index.html).
 * Plantilla para tus páginas: localizadores privados arriba,
 * acciones públicas con nombre de negocio abajo.
 */
public class LoginPage extends BasePage {

    // ─── Localizadores ───
    private final By campoUsuario = By.id("username");
    private final By campoPassword = By.id("password");
    private final By botonIngresar = By.cssSelector("#login-form button[type='submit']");
    private final By mensajeError = By.id("error");

    public LoginPage(WebDriver driver, Report report) {
        super(driver, report);
    }

    @Override
    protected String path() {
        return "/";
    }

    // ─── Acciones ───
    public void login(String usuario, String password) {
        type(campoUsuario, usuario, "Ingresar usuario");
        typeSecret(campoPassword, password, "Ingresar contraseña");
        click(botonIngresar, "Hacer clic en \"Ingresar\"");
    }

    // ─── Validaciones ───
    public void validarPantallaLogin() {
        expectVisible(botonIngresar, "Validar que se muestra el formulario de login");
    }

    public void validarMensajeError(String mensaje) {
        expectText(mensajeError, mensaje, "Validar mensaje de error \"" + mensaje + "\"");
    }
}
