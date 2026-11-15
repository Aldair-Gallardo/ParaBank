package reports;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

/**
 * Extensión de JUnit que cierra el reporte de cada test, pase o falle.
 * Se ejecuta justo después del test y ANTES de @AfterEach, así el
 * navegador sigue abierto para tomar la captura final si hubo error.
 *
 * Se activa con @ExtendWith(ReportExtension.class) en BaseTest.
 */
public class ReportExtension implements AfterTestExecutionCallback {

    /** Lo implementa BaseTest para que la extensión encuentre el reporte del test. */
    public interface HasReport {
        Report report();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Object instance = context.getRequiredTestInstance();
        if (!(instance instanceof HasReport holder) || holder.report() == null) return;

        Report report = holder.report();
        Throwable error = context.getExecutionException().orElse(null);
        boolean skipped = error instanceof TestAbortedException;
        report.finish(skipped ? null : error, skipped);

        // Test "verde" con pasos fallidos registrados → se marca como fallido,
        // para que JUnit y el reporte digan lo mismo.
        if (error == null && report.failedCount() > 0) {
            throw new AssertionError("El reporte registró " + report.failedCount() + " paso(s) fallido(s).");
        }
    }
}
