package utils;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Servidor web local para la carpeta WebSite/ (sin instalar nada).
 * Se levanta solo al iniciar las pruebas cuando base.url está vacío,
 * y se apaga al terminar. Usa el servidor HTTP que ya trae Java.
 *
 * Para ver el sitio tú mismo: ejecuta el main de esta clase en IntelliJ
 * (clic derecho → Run 'WebServer.main()') y abre http://localhost:3000
 */
public final class WebServer {

    private static final Path ROOT = Path.of("WebSite").toAbsolutePath().normalize();
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "svg", "image/svg+xml",
            "ico", "image/x-icon");

    private static HttpServer server;

    private WebServer() {
    }

    public static synchronized void start(int port) {
        if (server != null) return;
        if (!Files.isDirectory(ROOT)) {
            System.out.println("[Aviso] base.url está vacío y no existe la carpeta WebSite/. "
                    + "Configura base.url en config.properties o agrega el sitio en WebSite/.");
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (IOException e) {
            // El puerto ya está en uso: probablemente el sitio ya está levantado.
            System.out.println("[Info] El puerto " + port + " ya está en uso; se usará el servidor existente.");
            return;
        }
        server.createContext("/", exchange -> {
            String urlPath = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
            Path file = ROOT.resolve(urlPath.substring(1)).normalize();
            if (Files.isDirectory(file)) file = file.resolve("index.html");

            if (!file.startsWith(ROOT) || !Files.isRegularFile(file)) {
                byte[] body = "No encontrado".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }
            String name = file.getFileName().toString();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
            byte[] body = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(ext, "application/octet-stream"));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();
        System.out.println("[Web] Sitio WebSite/ disponible en http://localhost:" + port);
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public static void main(String[] args) {
        start(Config.webPort());
        System.out.println("Presiona el botón Stop de IntelliJ para detenerlo.");
    }
}
