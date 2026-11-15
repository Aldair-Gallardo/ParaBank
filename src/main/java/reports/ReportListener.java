package reports;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Se ejecuta UNA vez al iniciar y UNA vez al terminar todas las pruebas
 * (desde IntelliJ o con "mvn test"). JUnit lo encuentra solo gracias al archivo
 * src/test/resources/META-INF/services/org.junit.platform.launcher.LauncherSessionListener
 */
public class ReportListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        ReportBuilder.start();
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        ReportBuilder.finish();
    }
}
