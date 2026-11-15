package tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import utils.Config;

/**
 * Pruebas de Login con POM.
 *
 * Estructura de cada test:
 *   try   → los pasos del caso (cada acción del Page Object ya queda en el reporte)
 *   catch → se registra un mensaje claro de la falla y se relanza el error
 *           para que JUnit marque el test como FALLIDO.
 *
 * No hace falta finally ni guardar el reporte: se guarda solo al terminar.
 * El código al inicio de @DisplayName (TC-001) aparece en los reportes.
 */
class LoginTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("TC-001 Login exitoso")
    void loginExitoso() {
        try {
            loginPage.open();
            loginPage.login(Config.user(), Config.password());
            homePage.expectUrlContains("home.html", "Validar que se abre la página de inicio");
            homePage.validarBienvenida(Config.user());
            homePage.validarTablaCuentas();
        } catch (Throwable e) {
            report.testFailed("Falló el login exitoso", e);
            throw e;
        }
    }

    @Test
    @DisplayName("TC-002 Login con contraseña incorrecta muestra error")
    void loginContrasenaIncorrecta() {
        try {
            report.info("Se usa una contraseña inválida a propósito");
            loginPage.open();
            loginPage.login(Config.user(), "clave-incorrecta");
            loginPage.validarMensajeError("Usuario o contraseña incorrectos");
        } catch (Throwable e) {
            report.testFailed("No se mostró el mensaje de credenciales incorrectas", e);
            throw e;
        }
    }

    @Test
    @DisplayName("TC-003 Cerrar sesión")
    void cerrarSesion() {
        try {
            loginPage.open();
            loginPage.login(Config.user(), Config.password());
            homePage.cerrarSesion();
            loginPage.validarPantallaLogin();
        } catch (Throwable e) {
            report.testFailed("Falló el cierre de sesión", e);
            throw e;
        }
    }
}
