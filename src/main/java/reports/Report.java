package reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import reports.ReportModels.CaseResult;
import reports.ReportModels.StepLog;
import reports.ReportModels.StepStatus;
import utils.Config;

/**
 * Reporte de UN caso de prueba.
 *
 * Lo crea BaseTest antes de cada test. Los Page Objects registran los pasos
 * con testPassed / testFailed / info, y al terminar el test ReportExtension
 * llama a finish() para guardar todo en la carpeta del caso:
 *
 *   reports/ejecuciones/Ejecucion_2026-11-14_201502/
 *     └── TC-001 Login exitoso - chrome-a1b2c3/
 *           ├── reporte_TC-001 Login exitoso - chrome.html
 *           ├── reporte_TC-001 Login exitoso - chrome.pdf
 *           ├── case.json
 *           └── screenshots/paso-01-exitoso.png ...
 */
public class Report {

    /** Código al inicio del nombre: "TC-001 Login", "[BAC-002] Pago", "TC-LOG-003 | ...". */
    private static final Pattern CODE_IN_TITLE = Pattern.compile("^\\[?([A-Za-z][A-Za-z0-9]*(?:-[A-Za-z][A-Za-z0-9]*)*-\\d+(?:-\\d+)?)\\]?\\s*[-|:·]?\\s*");

    private final WebDriver driver;
    private final CaseResult data = new CaseResult();
    private final Path dir;
    private final OffsetDateTime startedAt = OffsetDateTime.now();
    private long lastStepAt = System.currentTimeMillis();
    /** Mensaje del catch del test: se muestra como resumen de la falla. */
    private String failureSummary;

    /**
     * @param driver      navegador del test
     * @param displayName nombre del test (ej: "TC-001 Login exitoso")
     * @param testClass   clase del test
     * @param tags        etiquetas JUnit (@Tag)
     */
    public Report(WebDriver driver, String displayName, String testClass, Collection<String> tags) {
        this.driver = driver;
        Matcher m = CODE_IN_TITLE.matcher(displayName);
        data.title = displayName;
        data.code = m.find() ? m.group(1) : "";
        data.name = data.code.isEmpty() ? displayName : displayName.substring(m.end()).trim();
        data.testClass = testClass;
        data.tags = new ArrayList<>(tags);
        data.browser = Config.browser();
        data.environment = Config.testEnv();
        data.baseUrl = Config.baseUrl();
        data.executor = Config.executor();
        data.startedAt = startedAt.toString();

        // Nombres cortos: Windows no abre archivos cuya ruta completa pase de 260 caracteres.
        String label = safeName((data.code.isEmpty() ? "" : data.code + " ") + data.name, 40) + " - " + safeName(data.browser, 12);
        // Sufijo corto para que dos casos con el mismo nombre nunca compartan carpeta.
        data.id = label + "-" + UUID.randomUUID().toString().substring(0, 6);
        data.htmlFile = "reporte_" + label + ".html";

        dir = ReportBuilder.currentRunDir().resolve(data.id);
        try {
            Files.createDirectories(dir.resolve("screenshots"));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta del reporte: " + dir, e);
        }
    }

    // ─── API para Page Objects y tests ─────────────────────────────

    /** Paso exitoso (con captura, según report.screenshots). */
    public void testPassed(String message) {
        addStep(StepStatus.PASSED, message, null, Config.reportAllScreenshots());
    }

    /** Paso exitoso indicando si se toma captura. */
    public void testPassed(String message, boolean screenshot) {
        addStep(StepStatus.PASSED, message, null, screenshot);
    }

    /** Paso fallido con captura. NO detiene el test (lánzalo tú con throw). */
    public void testFailed(String message) {
        testFailed(message, null);
    }

    /**
     * Paso fallido con captura y el error. Pensado para el catch del test:
     * si el error ya lo registró un Page Object, no se duplica el paso y el
     * mensaje se usa como resumen de la falla.
     */
    public void testFailed(String message, Throwable error) {
        String errorText = error == null ? null : cleanError(error);
        StepLog last = data.steps.isEmpty() ? null : data.steps.get(data.steps.size() - 1);
        if (last != null && last.status == StepStatus.FAILED && errorText != null && errorText.equals(last.error)) {
            failureSummary = message;
            return;
        }
        if (failureSummary == null) failureSummary = message;
        addStep(StepStatus.FAILED, message, errorText, true);
    }

    /** Nota informativa sin captura (ej: datos usados). */
    public void info(String message) {
        addStep(StepStatus.INFO, message, null, false);
    }

    public int failedCount() {
        return (int) data.steps.stream().filter(s -> s.status == StepStatus.FAILED).count();
    }

    // ─── Interno ───────────────────────────────────────────────────

    private void addStep(StepStatus status, String message, String error, boolean takeScreenshot) {
        long now = System.currentTimeMillis();
        StepLog step = new StepLog();
        step.index = data.steps.size() + 1;
        step.status = status;
        step.message = message;
        step.error = error;
        step.time = OffsetDateTime.now().toString();
        step.durationMs = now - lastStepAt;
        lastStepAt = now;

        if (takeScreenshot && driver != null) {
            String suffix = switch (status) {
                case PASSED -> "exitoso";
                case FAILED -> "fallido";
                case INFO -> "info";
            };
            String rel = "screenshots/paso-" + String.format("%02d", step.index) + "-" + suffix + ".png";
            try {
                byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Files.write(dir.resolve(rel), png);
                step.screenshot = rel;
            } catch (Exception e) {
                System.err.println("[Aviso] No se pudo tomar la captura del paso " + step.index + ": " + firstLine(e.getMessage()));
            }
        }
        data.steps.add(step);
    }

    /**
     * Cierra el reporte al terminar el test (lo llama ReportExtension).
     * @param error   excepción con la que terminó el test, o null si pasó
     * @param skipped true si el test fue omitido (Assumptions)
     */
    public CaseResult finish(Throwable error, boolean skipped) {
        String status;
        if (skipped) status = "OMITIDO";
        else if (error == null && failedCount() == 0) status = "EXITOSO";
        else status = "FALLIDO";

        if ("FALLIDO".equals(status)) {
            StepLog firstFailed = data.steps.stream().filter(s -> s.status == StepStatus.FAILED).findFirst().orElse(null);
            String detail = error != null ? cleanError(error)
                    : firstFailed != null ? (firstFailed.error != null ? firstFailed.error : firstFailed.message) : null;
            data.reason = failureSummary != null ? (failureSummary + "\n\n" + (detail == null ? "" : detail)).trim() : detail;

            // Si el error no quedó como paso (ej: un assert directo en el test), se agrega con su captura.
            StepLog last = data.steps.isEmpty() ? null : data.steps.get(data.steps.size() - 1);
            if (error != null && (last == null || last.status != StepStatus.FAILED)) {
                addStep(StepStatus.FAILED, "La prueba terminó con error", cleanError(error), true);
            }
        }

        OffsetDateTime endedAt = OffsetDateTime.now();
        data.status = status;
        data.endedAt = endedAt.toString();
        data.durationMs = java.time.Duration.between(startedAt, endedAt).toMillis();
        data.counts.passed = (int) data.steps.stream().filter(s -> s.status == StepStatus.PASSED).count();
        data.counts.failed = failedCount();
        data.counts.info = (int) data.steps.stream().filter(s -> s.status == StepStatus.INFO).count();
        data.counts.total = data.steps.size();
        try {
            if (driver instanceof HasCapabilities hc) data.browserVersion = hc.getCapabilities().getBrowserVersion();
        } catch (Exception ignored) {
            // navegador ya cerrado
        }

        ReportBuilder.writeCaseFiles(dir, data);
        return data;
    }

    // ─── Utilidades ────────────────────────────────────────────────

    /** Nombre de archivo seguro: sin tildes ni caracteres especiales. */
    public static String safeName(String text) {
        return safeName(text, 80);
    }

    /** Nombre de archivo seguro, recortado a {@code max} caracteres. */
    public static String safeName(String text, int max) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9 _.-]", "_")
                .replaceAll("\\s+", " ")
                .replaceAll("_+", "_")
                .trim();
        return s.length() > max ? s.substring(0, max).trim() : s;
    }

    /** Mensaje de error sin la información técnica extra que agrega Selenium. */
    public static String cleanError(Throwable error) {
        String msg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        for (String cut : new String[]{"\nBuild info:", "\nFor documentation on this error", "\nSystem info:", "\nSession ID:"}) {
            int i = msg.indexOf(cut);
            if (i > 0) msg = msg.substring(0, i);
        }
        return msg.trim();
    }

    private static String firstLine(String text) {
        if (text == null) return "";
        int i = text.indexOf('\n');
        return i < 0 ? text : text.substring(0, i);
    }
}
