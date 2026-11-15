package pages.login;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;   // ← NUEVO
import org.openqa.selenium.support.ui.WebDriverWait;
import reports.Report;
import utils.Config;

import java.time.Duration;
import java.util.Map;


/** Login de ParaBank **/
public class LoginPage  {

    //Navegador
    private final WebDriver driver;

    /** Reporte del caso (pasos, capturas y errores). */
    private final Report report;

    /** Espera hasta que los elementos estén listos (timeout.seconds de config.properties). */
    private final WebDriverWait wait;


    // Localizadores de mi pagina

    //Usuario
    @FindBy(xpath = "//*[@name='username']")
    private WebElement Campousuario;

    //Contraseña
    @FindBy(xpath = "//*[@name='password']")
    private WebElement Campocontraseña;

    //Botón de login
    @FindBy(xpath = "//*[@value='Log In']")
    private WebElement btnlogin;

    //Título "Accounts Overview" (aparece al iniciar sesión)
    @FindBy (xpath = "//div[@id='showOverview']/h1")
    private WebElement lista;

    /** Mensaje de error del login. */
    @FindBy(xpath = "//div[@id='rightPanel']//p[@class='error']")
    private WebElement mensajeError;

    public LoginPage(WebDriver driver, Report report ){
        this.driver = driver;
        this.report = report;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
        PageFactory.initElements(driver, this);
    }

    /**
     * Login satisfactorio.
     * @param testData datos del Excel (columnas "usuario" y "password")
     */

    public void loginSatisfactorio(Map<String, String> testData){
        try {

            // 1 acceder a la página web
            try {
                driver.get(Config.baseUrl() + "/index.htm");
                report.testPassed("Se accedio a la página ParaBank");
            } catch (Exception e) {
                report.testFailed("No se pudo acceder a la página de ParaBank", e);
                throw e;
            }

            // 2 Ingresar al usuario

            try {
                Campousuario.clear();
                Campousuario.sendKeys(testData.get("usuario"));
                report.testPassed("Se ingresó el usuario correctamente.");   // ← decía "contraseña"
            } catch (Exception e) {
                report.testFailed("No se pudo ingresar el usuario. " , e);
                throw e;
            }

            // 3 Ingresar la contraseña

            try {
                Campocontraseña.clear();
                Campocontraseña.sendKeys(testData.get("password"));
                report.testPassed("Contraseña ingresada correctamente");
            } catch (Exception e){
                report.testFailed("No se pudo ingresar la contraseña", e);
                throw e;
            }

            // 4 click en log in

            try {
                btnlogin.click();
                report.testPassed("Se hizo clic en Log In");
            } catch (Exception e){
                report.testFailed("No se puedo hacer click en Log In.", e);
                throw e;
            }

            // 5 validar que inició sesión                              ← NUEVO

            try {
                wait.until(ExpectedConditions.visibilityOf(lista));
                report.testPassed("Se inició sesión: se muestra Accounts Overview");
            } catch (Exception e){
                report.testFailed("No se inició sesión: no apareció Accounts Overview", e);
                throw e;
            }


        } catch (Exception e) {
            report.testFailed("El flujo de login terminó con error.", e);
            throw e ;
        }



    }


    /**                                                                  ← NUEVO
     * Login con contraseña incorrecta.
     * @param testData datos del Excel (columnas "usuario", "password" y "esperado")
     */

    public void loginIncorrecto(Map<String, String> testData){
        try {

            // 1 acceder a la página web
            try {
                driver.get(Config.baseUrl() + "/index.htm");
                report.testPassed("Se accedio a la página ParaBank");
            } catch (Exception e) {
                report.testFailed("No se pudo acceder a la página de ParaBank", e);
                throw e;
            }

            // 2 Ingresar al usuario

            try {
                Campousuario.clear();
                Campousuario.sendKeys(testData.get("usuario"));
                report.testPassed("Se ingresó el usuario correctamente.");
            } catch (Exception e) {
                report.testFailed("No se pudo ingresar el usuario. " , e);
                throw e;
            }

            // 3 Ingresar la contraseña incorrecta

            try {
                Campocontraseña.clear();
                Campocontraseña.sendKeys(testData.get("password"));
                report.testPassed("Contraseña incorrecta ingresada");
            } catch (Exception e){
                report.testFailed("No se pudo ingresar la contraseña", e);
                throw e;
            }

            // 4 click en log in

            try {
                btnlogin.click();
                report.testPassed("Se hizo clic en Log In");
            } catch (Exception e){
                report.testFailed("No se puedo hacer click en Log In.", e);
                throw e;
            }

            // 5 validar el mensaje de error

            try {
                wait.until(ExpectedConditions.textToBePresentInElement(mensajeError, testData.get("esperado")));
                report.testPassed("Se mostró el mensaje de error: " + testData.get("esperado"));
            } catch (Exception e){
                report.testFailed("No apareció el mensaje de error esperado: " + testData.get("esperado"), e);
                throw e;
            }


        } catch (Exception e) {
            report.testFailed("El flujo de login incorrecto terminó con error.", e);
            throw e ;
        }



    }


}