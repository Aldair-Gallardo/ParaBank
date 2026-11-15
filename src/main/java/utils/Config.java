package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración central del proyecto. TODO el proyecto lee la configuración desde aquí.
 *
 * Orden de prioridad (el primero que tenga valor gana):
 *   1. Variable de entorno       → BASE_URL, BROWSER, HEADLESS, TEST_USER, TEST_PASSWORD...
 *   2. Propiedad del sistema     → mvn test -Dbase.url=https://... -Dbrowser=firefox
 *   3. config.local.properties   → tus valores personales (NO se sube a GitHub)
 *   4. config.properties         → valores por defecto del proyecto
 */
public final class Config {

    private static final Properties PROPS = new Properties();

    static {
        load(Path.of("config.properties"));
        load(Path.of("config.local.properties"));
    }

    private Config() {
    }

    private static void load(Path file) {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Properties p = new Properties();
            p.load(reader);
            p.forEach((k, v) -> {
                if (!String.valueOf(v).isBlank()) PROPS.put(k, v);
            });
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + file.toAbsolutePath(), e);
        }
    }

    /** Nombre de la variable de entorno para una clave: base.url → BASE_URL. */
    private static String envName(String key) {
        return switch (key) {
            case "user" -> "TEST_USER";
            case "password" -> "TEST_PASSWORD";
            default -> key.toUpperCase().replace('.', '_');
        };
    }

    public static String get(String key, String defaultValue) {
        String env = System.getenv(envName(key));
        if (env != null && !env.isBlank()) return env.trim();
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String file = PROPS.getProperty(key);
        if (file != null && !file.isBlank()) return file.trim();
        return defaultValue;
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String v = get(key, null);
        return v == null ? defaultValue : v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("si");
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ─── Accesos directos ─────────────────────────────────────────

    public static boolean isCI() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }

    public static String projectName() { return get("project.name", "Proyecto de Automatización"); }

    public static String testEnv() { return get("test.env", "local"); }

    public static int webPort() { return getInt("web.port", 3000); }

    /** true cuando no hay base.url: se usa el sitio local de WebSite/. */
    public static boolean useLocalWebSite() { return get("base.url", null) == null; }

    /** URL base sin "/" al final. */
    public static String baseUrl() {
        String url = get("base.url", "http://localhost:" + webPort());
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String browser() { return get("browser", "chrome").toLowerCase(); }

    public static boolean headless() { return isCI() || getBool("headless", false); }

    public static int timeoutSeconds() { return getInt("timeout.seconds", 10); }

    public static String executor() {
        return get("executor", System.getProperty("user.name", "Desconocido"));
    }

    public static String user() { return get("user", ""); }

    public static String password() { return get("password", ""); }

    public static boolean reportPdf() { return getBool("report.pdf", true); }

    public static boolean reportAllScreenshots() { return !"failed".equalsIgnoreCase(get("report.screenshots", "all")); }

    public static boolean reportOpen() { return !isCI() && getBool("report.open", true); }

    /**
     * Dominios que el navegador no debe cargar (anuncios, analítica...), separados por coma.
     * Ej: googlesyndication.com, doubleclick.net. Solo Chrome y Edge.
     */
    public static java.util.List<String> blockedHosts() {
        String v = get("blocked.hosts", "");
        return java.util.Arrays.stream(v.split(",")).map(String::trim).filter(h -> !h.isEmpty()).toList();
    }
}
