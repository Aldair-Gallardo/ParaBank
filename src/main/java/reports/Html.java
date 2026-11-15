package reports;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Utilidades de formato y piezas HTML reutilizadas por las plantillas. */
public final class Html {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", ES);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Html() {
    }

    /** Escapa texto para insertarlo en HTML de forma segura. */
    public static String esc(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Convierte una ruta relativa en un href válido (espacios, #, tildes...). */
    public static String href(String relativePath) {
        return Arrays.stream(relativePath.split("[\\\\/]"))
                .map(s -> URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    // ─── Fechas y duraciones ──────────────────────────────────────

    private static OffsetDateTime local(String iso) {
        return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static String fmtDate(String iso) { return DATE.format(local(iso)).replace(".", ""); }

    public static String fmtTime(String iso) { return TIME.format(local(iso)); }

    public static String fmtDateTime(String iso) { return fmtDate(iso) + " · " + fmtTime(iso); }

    public static String fmtLongDay(String iso) {
        String s = LONG_DAY.format(local(iso));
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String dayKey(String iso) { return DAY_KEY.format(local(iso)); }

    public static String fmtDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        double s = ms / 1000.0;
        if (s < 60) return String.format(Locale.US, "%.1f s", s);
        long m = (long) (s / 60);
        long rest = Math.round(s % 60);
        if (m < 60) return m + " min " + String.format("%02d", rest) + " s";
        return (m / 60) + " h " + String.format("%02d", m % 60) + " min";
    }

    public static int rate(int passed, int total) {
        return total > 0 ? Math.round(passed * 100f / total) : 0;
    }

    // ─── Iconos SVG (sin depender de internet) ────────────────────

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("check", "<path d=\"M20 6 9 17l-5-5\"/>"),
            Map.entry("x", "<path d=\"M18 6 6 18M6 6l12 12\"/>"),
            Map.entry("minus", "<path d=\"M5 12h14\"/>"),
            Map.entry("info", "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 11v5M12 8h.01\"/>"),
            Map.entry("clock", "<circle cx=\"12\" cy=\"12\" r=\"9\"/><path d=\"M12 7v5l3 2\"/>"),
            Map.entry("user", "<circle cx=\"12\" cy=\"8\" r=\"4\"/><path d=\"M4 21a8 8 0 0 1 16 0\"/>"),
            Map.entry("server", "<rect x=\"3\" y=\"4\" width=\"18\" height=\"7\" rx=\"2\"/><rect x=\"3\" y=\"13\" width=\"18\" height=\"7\" rx=\"2\"/><path d=\"M7 7.5h.01M7 16.5h.01\"/>"),
            Map.entry("link", "<path d=\"M10 14a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-1 1\"/><path d=\"M14 10a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l1-1\"/>"),
            Map.entry("browser", "<rect x=\"3\" y=\"4\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M3 9h18M7 6.5h.01M10 6.5h.01\"/>"),
            Map.entry("calendar", "<rect x=\"3\" y=\"5\" width=\"18\" height=\"16\" rx=\"2\"/><path d=\"M3 10h18M8 3v4M16 3v4\"/>"),
            Map.entry("file", "<path d=\"M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z\"/><path d=\"M14 3v6h6\"/>"),
            Map.entry("pdf", "<path d=\"M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z\"/><path d=\"M14 3v6h6M8 13h8M8 17h5\"/>"),
            Map.entry("sheet", "<rect x=\"3\" y=\"3\" width=\"18\" height=\"18\" rx=\"2\"/><path d=\"M3 9h18M3 15h18M9 3v18\"/>"),
            Map.entry("home", "<path d=\"m3 11 9-8 9 8\"/><path d=\"M5 10v10h14V10\"/>"),
            Map.entry("layers", "<path d=\"m12 3 9 5-9 5-9-5z\"/><path d=\"m3 13 9 5 9-5\"/>"),
            Map.entry("tag", "<path d=\"M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9z\"/><path d=\"M7.5 7.5h.01\"/>"),
            Map.entry("cpu", "<rect x=\"6\" y=\"6\" width=\"12\" height=\"12\" rx=\"2\"/><path d=\"M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4\"/>"),
            Map.entry("alert", "<path d=\"M12 3 2 20h20z\"/><path d=\"M12 10v4M12 17h.01\"/>"),
            Map.entry("search", "<circle cx=\"11\" cy=\"11\" r=\"7\"/><path d=\"m20 20-4-4\"/>"),
            Map.entry("sort", "<path d=\"M7 4v16M3 16l4 4 4-4M17 20V4M13 8l4-4 4 4\"/>"),
            Map.entry("chevron", "<path d=\"m9 6 6 6-6 6\"/>"),
            Map.entry("play", "<path d=\"M7 4v16l13-8z\"/>"),
            Map.entry("zoom", "<circle cx=\"11\" cy=\"11\" r=\"7\"/><path d=\"m20 20-4-4M11 8v6M8 11h6\"/>"));

    public static String icon(String name) {
        return icon(name, "ic");
    }

    public static String icon(String name, String cls) {
        return "<svg class=\"" + cls + "\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" "
                + "stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">" + ICONS.getOrDefault(name, "") + "</svg>";
    }

    // ─── Estados ──────────────────────────────────────────────────

    /** Clase CSS del estado: ok | bad | skip | info. */
    public static String statusClass(String status) {
        return switch (status == null ? "" : status) {
            case "EXITOSO", "PASSED" -> "ok";
            case "FALLIDO", "FAILED" -> "bad";
            case "OMITIDO", "SIN CASOS" -> "skip";
            default -> "info";
        };
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "PASSED" -> "EXITOSO";
            case "FAILED" -> "FALLIDO";
            default -> status;
        };
    }

    /** Etiqueta de estado con color sólido. */
    public static String pill(String status) {
        return pill(status, false);
    }

    public static String pill(String status, boolean large) {
        String cls = statusClass(status);
        String ic = switch (cls) {
            case "ok" -> "check";
            case "bad" -> "x";
            case "skip" -> "minus";
            default -> "info";
        };
        return "<span class=\"pill pill-" + cls + (large ? " pill-lg" : "") + "\">" + icon(ic) + esc(statusLabel(status)) + "</span>";
    }

    /** Dona de porcentaje hecha solo con CSS. */
    public static String donut(int rate, String label) {
        int r = Math.max(0, Math.min(100, rate));
        String tone = r == 100 ? "ok" : r >= 70 ? "warn" : "bad";
        return "<div class=\"donut donut-" + tone + "\" style=\"--p:" + r + "\"><div class=\"donut-hole\"><strong>" + r
                + "%</strong><span>" + esc(label) + "</span></div></div>";
    }

    /** Documento HTML base con su hoja de estilos externa. */
    public static String layout(String title, String css, String js, String body, String bodyClass) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta name="generator" content="selenium-pom-base">
                  <title>%s</title>
                  <link rel="stylesheet" href="%s">
                </head>
                <body class="%s">
                %s
                <script src="%s"></script>
                </body>
                </html>
                """.formatted(esc(title), css, bodyClass, body, js);
    }
}
