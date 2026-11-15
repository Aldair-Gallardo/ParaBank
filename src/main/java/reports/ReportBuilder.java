package reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.PrintsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.print.PageMargin;
import org.openqa.selenium.print.PageSize;
import org.openqa.selenium.print.PrintOptions;
import reports.ReportModels.CaseResult;
import reports.ReportModels.CaseSummary;
import reports.ReportModels.RunInfo;
import utils.Config;
import utils.DriverFactory;
import utils.WebServer;

/**
 * Arma los reportes de la ejecución.
 *
 * Al iniciar   → crea reports/ejecuciones/Ejecucion_FECHA_HORA/ (y levanta WebSite/ si hace falta)
 * Al terminar  → PDF de cada caso, consolidado HTML + Excel y reports/index.html
 *
 * Si una ejecución anterior quedó "EN CURSO" (detuviste la prueba con el botón
 * Stop de IntelliJ, se cerró la consola...), se completa sola al iniciar la
 * siguiente o al ejecutar ReportBuilder.main().
 *
 * Lo llama ReportListener (JUnit) una vez al inicio y una vez al final,
 * tanto desde IntelliJ como con "mvn test".
 */
public final class ReportBuilder {

    public static final Path REPORTS_DIR = Path.of("reports").toAbsolutePath();
    public static final Path RUNS_DIR = REPORTS_DIR.resolve("ejecuciones");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter RUN_ID = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private static Path runDir;
    private static boolean finished;

    private ReportBuilder() {
    }

    // ═══════════════════════════════════════════════════════════════
    // Inicio
    // ═══════════════════════════════════════════════════════════════

    /** Carpeta de la ejecución actual (la crea si todavía no existe). */
    public static synchronized Path currentRunDir() {
        if (runDir == null) start();
        return runDir;
    }

    public static synchronized void start() {
        if (runDir != null) return;
        recoverUnfinishedRuns();
        if (Config.useLocalWebSite()) WebServer.start(Config.webPort());

        OffsetDateTime now = OffsetDateTime.now();
        String id = RUN_ID.format(now);
        Path dir = RUNS_DIR.resolve("Ejecucion_" + id);
        for (int i = 2; Files.exists(dir); i++) {
            id = RUN_ID.format(now) + "-" + i;
            dir = RUNS_DIR.resolve("Ejecucion_" + id);
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear " + dir, e);
        }
        runDir = dir;
        finished = false;

        RunInfo run = new RunInfo();
        run.id = id;
        run.folder = dir.getFileName().toString();
        run.projectName = Config.projectName();
        run.environment = Config.testEnv();
        run.baseUrl = Config.baseUrl();
        run.executor = Config.executor();
        run.browsers.add(Config.browser());
        run.startedAt = now.toString();
        run.status = "EN CURSO";
        run.system.java = System.getProperty("java.version");
        run.system.os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")";
        run.system.selenium = seleniumVersion();
        run.htmlFile = run.folder + ".html";
        writeJson(dir.resolve("run.json"), run);

        // Respaldo: si nadie llama a finish(), se genera igual al cerrar la JVM.
        Runtime.getRuntime().addShutdownHook(new Thread(ReportBuilder::finish));
        System.out.println("\n[Reportes] Reportes de esta ejecución: " + Path.of("").toAbsolutePath().relativize(dir) + "\n");
    }

    private static String seleniumVersion() {
        try {
            return new BuildInfo().getReleaseLabel();
        } catch (Throwable e) {
            return "desconocida";
        }
    }

    /** Escribe case.json y el HTML del caso (lo usa Report.finish()). */
    public static void writeCaseFiles(Path caseDir, CaseResult c) {
        String runFolder = caseDir.getParent().getFileName().toString();
        String runId = runFolder.replaceFirst("^Ejecucion_", "");
        writeJson(caseDir.resolve("case.json"), c);
        writeText(caseDir.resolve(c.htmlFile), ReportTemplates.casePage(c, runId, runFolder + ".html", Config.projectName()));
    }

    // ═══════════════════════════════════════════════════════════════
    // Final
    // ═══════════════════════════════════════════════════════════════

    public static synchronized void finish() {
        if (runDir == null || finished) return;
        finished = true;
        try {
            buildRun(runDir, Config.reportOpen());
        } catch (Exception e) {
            System.err.println("[Aviso] Error generando los reportes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            WebServer.stop();
        }
    }

    /**
     * Completa las ejecuciones que quedaron "EN CURSO" porque las pruebas se
     * detuvieron antes de terminar. Devuelve cuántas completó.
     */
    public static int recoverUnfinishedRuns() {
        if (!Files.isDirectory(RUNS_DIR)) return 0;
        int count = 0;
        try (Stream<Path> s = Files.list(RUNS_DIR)) {
            for (Path dir : s.filter(Files::isDirectory).sorted().toList()) {
                if (dir.equals(runDir)) continue; // la ejecución actual
                Path json = dir.resolve("run.json");
                if (!Files.isRegularFile(json)) continue;
                try {
                    if (!"EN CURSO".equals(readJson(json, RunInfo.class).status)) continue;
                    System.out.println("[Info] Completando el reporte pendiente " + dir.getFileName() + "...");
                    buildRun(dir, false);
                    count++;
                } catch (Exception e) {
                    System.err.println("[Aviso] No se pudo completar " + dir.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Aviso] No se pudo revisar " + RUNS_DIR + ": " + e.getMessage());
        }
        return count;
    }

    private static void buildRun(Path dir, boolean open) throws IOException {
        RunInfo run = readJson(dir.resolve("run.json"), RunInfo.class);

        List<Path> caseDirs;
        try (Stream<Path> s = Files.list(dir)) {
            caseDirs = s.filter(p -> Files.isRegularFile(p.resolve("case.json"))).toList();
        }
        if (caseDirs.isEmpty()) {
            deleteRecursively(dir);
            buildIndex();
            System.out.println("[Info] No se ejecutaron casos; no se guardó la ejecución.");
            return;
        }

        List<CaseResult> cases = new ArrayList<>();
        for (Path p : caseDirs) cases.add(readJson(p.resolve("case.json"), CaseResult.class));
        cases.sort(Comparator.comparing(c -> c.startedAt));

        // 1) PDF de cada caso
        if (Config.reportPdf()) generatePdfs(dir, cases);

        // 2) Totales
        int passed = (int) cases.stream().filter(c -> "EXITOSO".equals(c.status)).count();
        int failed = (int) cases.stream().filter(c -> "FALLIDO".equals(c.status)).count();
        int skipped = (int) cases.stream().filter(c -> "OMITIDO".equals(c.status)).count();
        OffsetDateTime endedAt = OffsetDateTime.now();

        run.endedAt = endedAt.toString();
        run.durationMs = Duration.between(OffsetDateTime.parse(run.startedAt), endedAt).toMillis();
        run.totals.cases = cases.size();
        run.totals.passed = passed;
        run.totals.failed = failed;
        run.totals.skipped = skipped;
        run.totals.steps = cases.stream().mapToInt(c -> c.counts.total).sum();
        run.successRate = Html.rate(passed, passed + failed);
        run.status = failed > 0 ? "FALLIDO" : passed > 0 ? "EXITOSO" : "SIN CASOS";
        run.browsers = new ArrayList<>(cases.stream().map(c -> c.browser).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        run.cases = new ArrayList<>();
        for (CaseResult c : cases) {
            CaseSummary s = new CaseSummary();
            s.id = c.id;
            s.code = c.code;
            s.name = c.name;
            s.browser = c.browser;
            s.status = c.status;
            s.reason = c.reason;
            s.durationMs = c.durationMs;
            s.startedAt = c.startedAt;
            s.counts = c.counts;
            s.htmlPath = c.id + "/" + c.htmlFile;
            s.pdfPath = c.pdfFile != null ? c.id + "/" + c.pdfFile : null;
            run.cases.add(s);
        }

        // 3) Excel + consolidado
        try {
            run.excelFile = ReportExcel.write(dir, run, cases);
        } catch (Exception e) {
            System.err.println("[Aviso] No se pudo generar el Excel: " + e.getMessage());
        }
        writeText(dir.resolve(run.htmlFile), ReportTemplates.runPage(run));
        writeJson(dir.resolve("run.json"), run);

        // 4) Página principal
        Path index = buildIndex();

        Path cwd = Path.of("").toAbsolutePath();
        System.out.println("\n============ Reportes ============");
        System.out.println("  Resultado   : " + run.status + " (" + passed + " OK / " + failed + " fallidos" + (skipped > 0 ? " / " + skipped + " omitidos" : "") + ")");
        System.out.println("  Consolidado : " + cwd.relativize(dir.resolve(run.htmlFile)));
        System.out.println("  Principal   : " + cwd.relativize(index));
        System.out.println("==================================\n");

        if (open) openInBrowser(index);
    }

    // ═══════════════════════════════════════════════════════════════
    // Página principal reports/index.html
    // ═══════════════════════════════════════════════════════════════

    public static List<RunInfo> readAllRuns() {
        List<RunInfo> runs = new ArrayList<>();
        if (!Files.isDirectory(RUNS_DIR)) return runs;
        try (Stream<Path> s = Files.list(RUNS_DIR)) {
            for (Path p : s.filter(Files::isDirectory).toList()) {
                Path json = p.resolve("run.json");
                if (!Files.isRegularFile(json)) continue;
                try {
                    RunInfo r = readJson(json, RunInfo.class);
                    if (!"EN CURSO".equals(r.status)) runs.add(r);
                } catch (Exception e) {
                    System.err.println("[Aviso] run.json ilegible en " + p.getFileName() + ", se omite.");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return runs;
    }

    public static Path buildIndex() {
        Path file = REPORTS_DIR.resolve("index.html");
        writeText(file, ReportTemplates.indexPage(readAllRuns(), Config.projectName()));
        return file;
    }

    // ═══════════════════════════════════════════════════════════════
    // PDF (Chrome sin ventana imprime cada HTML)
    // ═══════════════════════════════════════════════════════════════

    private static void generatePdfs(Path dir, List<CaseResult> cases) {
        WebDriver chrome;
        try {
            chrome = new ChromeDriver(DriverFactory.chromeOptions(true));
        } catch (Exception e) {
            System.err.println("[Aviso] No se generaron PDF (se necesita Google Chrome): " + Report.cleanError(e).lines().findFirst().orElse(""));
            return;
        }
        try {
            PrintOptions options = new PrintOptions();
            options.setBackground(true);
            options.setPageSize(new PageSize(29.7, 21.0)); // A4 (alto, ancho en cm)
            options.setPageMargin(new PageMargin(1.0, 1.0, 1.0, 1.0));
            for (CaseResult c : cases) {
                Path caseDir = dir.resolve(c.id);
                try {
                    chrome.get(caseDir.resolve(c.htmlFile).toUri().toString());
                    Pdf pdf = ((PrintsPage) chrome).print(options);
                    String pdfFile = c.htmlFile.replaceAll("\\.html$", ".pdf");
                    Files.write(caseDir.resolve(pdfFile), Base64.getDecoder().decode(pdf.getContent()));
                    c.pdfFile = pdfFile;
                    writeCaseFiles(caseDir, c); // re-escribe el HTML con el botón "PDF"
                } catch (Exception e) {
                    System.err.println("[Aviso] Error generando PDF de " + c.id + ": " + Report.cleanError(e));
                }
            }
        } finally {
            chrome.quit();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Utilidades
    // ═══════════════════════════════════════════════════════════════

    /** Abre un archivo en el navegador predeterminado (Windows, macOS o Linux). */
    public static void openInBrowser(Path file) {
        String path = file.toAbsolutePath().toString();
        String os = System.getProperty("os.name").toLowerCase();
        List<String> cmd = os.contains("win") ? List.of("cmd", "/c", "start", "\"\"", path)
                : os.contains("mac") ? List.of("open", path)
                : List.of("xdg-open", path);
        try {
            new ProcessBuilder(cmd).inheritIO().start();
        } catch (IOException e) {
            System.out.println("Abre a mano: " + path);
        }
    }

    private static void writeJson(Path file, Object value) {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(value, w);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir " + file, e);
        }
    }

    private static <T> T readJson(Path file, Class<T> type) throws IOException {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, type);
        }
    }

    private static void writeText(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir " + file, e);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }

    /**
     * Actualiza y abre reports/index.html (por ejemplo, después de borrar ejecuciones a mano).
     *   IntelliJ: clic derecho en ReportBuilder.java → Run 'ReportBuilder.main()'
     *   Consola:  mvn -q compile exec:java
     * Antes completa las ejecuciones que hayan quedado "EN CURSO".
     */
    public static void main(String[] args) {
        recoverUnfinishedRuns();
        Path index = buildIndex();
        int total = readAllRuns().size();
        System.out.println("[OK] index.html actualizado con " + total + " ejecución(es): " + index);
        if (total == 0) {
            System.out.println("[Info] Todavía no hay reportes. Ejecuta primero las pruebas.");
        } else if (!(args.length > 0 && args[0].equalsIgnoreCase("sin-abrir"))) {
            openInBrowser(index);
        }
    }
}
