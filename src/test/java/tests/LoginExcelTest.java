package tests;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utils.DataSet;

/**
 * Login con datos desde Excel (src/main/resources/Datos/data.xlsx, hoja "Login").
 * Cada fila genera su PROPIO test, con su propio reporte HTML/PDF.
 *
 * Columnas: caseCode | nombreTest | usuario | password | esperado
 *   esperado = "ok"    → el login debe entrar
 *   esperado = <texto> → debe mostrarse ese mensaje de error
 */
class LoginExcelTest extends BaseTest {

    /** Lee la hoja y entrega una fila por test: (código, nombre, datos de la fila). */
    static Stream<Arguments> datosLogin() throws IOException {
        return DataSet.leerHoja("Login").stream()
                .map(fila -> Arguments.of(fila.get("caseCode"), fila.get("nombreTest"), fila));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("datosLogin")
    void loginDesdeExcel(String caseCode, String nombreTest, Map<String, String> fila) {
        try {
            report.info("Datos: usuario=\"" + fila.get("usuario") + "\", resultado esperado=\"" + fila.get("esperado") + "\"");
            loginPage.open();
            loginPage.login(fila.get("usuario"), fila.get("password"));

            if ("ok".equalsIgnoreCase(fila.get("esperado"))) {
                homePage.validarBienvenida(fila.get("usuario"));
            } else {
                loginPage.validarMensajeError(fila.get("esperado"));
            }
        } catch (Throwable e) {
            report.testFailed("Falló el caso " + caseCode + " con el usuario \"" + fila.get("usuario") + "\"", e);
            throw e;
        }
    }
}
