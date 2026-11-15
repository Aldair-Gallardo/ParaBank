package utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * Crea el navegador según la configuración (browser = chrome | firefox | edge).
 *
 * No hace falta descargar drivers: Selenium Manager (incluido en Selenium 4)
 * descarga automáticamente el chromedriver / geckodriver / msedgedriver correcto
 * para la versión de tu navegador la primera vez que se ejecuta.
 */
public final class DriverFactory {

    private static final int WIDTH = 1440;
    private static final int HEIGHT = 900;

    private DriverFactory() {
    }

    public static WebDriver create() {
        boolean headless = Config.headless();
        WebDriver driver = switch (Config.browser()) {
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) options.addArguments("-headless");
                if (!Config.blockedHosts().isEmpty()) {
                    System.out.println("[Info] blocked.hosts solo funciona en Chrome y Edge; en Firefox se ignora.");
                }
                yield new FirefoxDriver(options);
            }
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) options.addArguments("--headless=new");
                options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--lang=es");
                blockHosts(options);
                yield new EdgeDriver(options);
            }
            case "chrome" -> {
                ChromeOptions options = chromeOptions(headless);
                blockHosts(options);
                yield new ChromeDriver(options);
            }
            default -> throw new IllegalArgumentException(
                    "Navegador \"" + Config.browser() + "\" no soportado. Usa: chrome, firefox o edge");
        };
        driver.manage().window().setSize(new Dimension(WIDTH, HEIGHT));
        return driver;
    }

    /**
     * Bloquea los dominios de blocked.hosts (anuncios que tapan botones o abren
     * ventanas). Chrome y Edge los tratan como si no existieran.
     */
    private static void blockHosts(org.openqa.selenium.chromium.ChromiumOptions<?> options) {
        List<String> hosts = Config.blockedHosts();
        if (hosts.isEmpty()) return;
        String rules = hosts.stream()
                .map(h -> "MAP " + h + " ~NOTFOUND, MAP *." + h + " ~NOTFOUND")
                .collect(Collectors.joining(", "));
        options.addArguments("--host-resolver-rules=" + rules);
    }

    /** Opciones de Chrome (también las usa el generador de PDF). */
    public static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) options.addArguments("--headless=new");
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--lang=es",
                "--window-size=" + WIDTH + "," + HEIGHT);

        // Desactiva el gestor de contraseñas de Chrome y su aviso de "contraseña filtrada":
        // es una ventana del navegador (no de la página) y Selenium no puede cerrarla.
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);
        return options;
    }
}
