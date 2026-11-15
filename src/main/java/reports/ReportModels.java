package reports;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos que guardan los reportes en JSON (case.json y run.json).
 * Son clases simples con campos públicos para leerlas y escribirlas con Gson.
 */
public final class ReportModels {

    private ReportModels() {
    }

    public enum StepStatus { PASSED, FAILED, INFO }

    /** Un paso registrado dentro de un caso. */
    public static class StepLog {
        public int index;
        public StepStatus status;
        public String message;
        public String error;
        /** Relativa a la carpeta del caso: screenshots/paso-01-exitoso.png */
        public String screenshot;
        /** Fecha y hora ISO-8601. */
        public String time;
        public long durationMs;
    }

    public static class Counts {
        public int passed;
        public int failed;
        public int info;
        public int total;
    }

    /** Resultado completo de un caso (case.json). */
    public static class CaseResult {
        public String id;
        public String code;
        public String name;
        public String title;
        /** EXITOSO | FALLIDO | OMITIDO */
        public String status;
        public String reason;
        public String browser;
        public String browserVersion;
        public String testClass;
        public List<String> tags = new ArrayList<>();
        public String startedAt;
        public String endedAt;
        public long durationMs;
        public String environment;
        public String baseUrl;
        public String executor;
        public Counts counts = new Counts();
        public List<StepLog> steps = new ArrayList<>();
        public String htmlFile;
        public String pdfFile;
    }

    /** Resumen de un caso dentro de run.json. */
    public static class CaseSummary {
        public String id;
        public String code;
        public String name;
        public String browser;
        public String status;
        public String reason;
        public long durationMs;
        public String startedAt;
        public Counts counts;
        /** Relativas a la carpeta de la ejecución. */
        public String htmlPath;
        public String pdfPath;
    }

    public static class Totals {
        public int cases;
        public int passed;
        public int failed;
        public int skipped;
        public int steps;
    }

    public static class SystemInfo {
        public String java;
        public String os;
        public String selenium;
    }

    /** Información de una ejecución completa (run.json). */
    public static class RunInfo {
        public String id;
        public String folder;
        public String projectName;
        public String environment;
        public String baseUrl;
        public String executor;
        public List<String> browsers = new ArrayList<>();
        public String startedAt;
        public String endedAt;
        public long durationMs;
        /** EN CURSO | EXITOSO | FALLIDO | SIN CASOS */
        public String status;
        public Totals totals = new Totals();
        public int successRate;
        public SystemInfo system = new SystemInfo();
        public String htmlFile;
        public String excelFile;
        public List<CaseSummary> cases = new ArrayList<>();
    }
}
