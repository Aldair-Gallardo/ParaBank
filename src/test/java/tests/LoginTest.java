package tests;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.DataSet;

class LoginTest extends BaseTest {

    @Test
    @DisplayName("TC-001 Login satisfactorio")
    void loginSatisfactorio() throws IOException {
        Map<String, String> datos = DataSet.fila("Login", "TC-001");
        try {
            login.loginSatisfactorio(datos);
            logout.cerrarSesion();
        } catch (Throwable e) {
            report.testFailed("Falló el login satisfactorio", e);
            throw e;
        }
    }

    @Test
    @DisplayName("TC-002 Login con contraseña incorrecta")
    void loginContrasenaIncorrecta() throws IOException {
        Map<String, String> datos = DataSet.fila("Login", "TC-002");
        try {
            login.loginIncorrecto(datos);
        } catch (Throwable e) {
            report.testFailed("Falló la validación de login incorrecto", e);
            throw e;
        }
    }
}