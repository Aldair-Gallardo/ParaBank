# Selenium POM Base

Plantilla base para automatizar pruebas web con **Selenium + Java + JUnit**, usando **Page Object Model (POM)** y pensada para trabajar en **IntelliJ IDEA**.

Genera reportes propios en **HTML** (con CSS separado), PDF y Excel, y una página principal con todas las ejecuciones en orden cronológico. La navegación es la misma que en la plantilla de Playwright (principal → ejecución → caso), pero con otro diseño:

| Página | Diseño |
|---|---|
| **Principal** (`reports/index.html`) | Panel lateral con indicadores y filtros; mapa de actividad con un cuadro por ejecución; mosaicos de ejecuciones agrupados por día |
| **Consolidado** de la ejecución | Panel lateral con el resumen y los datos del entorno; los casos en tarjetas, con una barra de pasos y botones HTML/PDF |
| **Reporte del caso** | Panel lateral con estado, indicadores, motivo de la falla y entorno; lista de pasos y un visor grande que muestra la captura del paso seleccionado (se navega con las flechas ← →) |
| **PDF del caso** | Resumen arriba y todos los pasos con sus capturas, dos por página |

---

## 1. Qué instalar

### 1.1 Programas

| Programa | Versión | Para qué | Verificar |
|---|---|---|---|
| **JDK (Java)** | **21 LTS** (Eclipse Temurin 21.0.x) | Compilar y ejecutar el proyecto | `java -version` → `21.0.x` |
| **IntelliJ IDEA** | 2025.3 o más reciente (gratis) | Editor; ya trae Maven incluido | — |
| **Google Chrome** | Última | Navegador de las pruebas y generador de PDF | — |
| **Git** | 2.40 o más | Subir a GitHub | `git --version` |
| Maven (opcional) | 3.9.x | Solo si quieres ejecutar `mvn` desde CMD | `mvn -v` |

> **IntelliJ IDEA Community:** desde la versión 2025.3, JetBrains unió Community y Ultimate en **un solo instalador**. La parte gratuita es lo que antes era Community, sin licencia ni registro, y es todo lo que necesitas para este proyecto. Descárgalo en <https://www.jetbrains.com/idea/download/>. Si te ofrece una prueba de Ultimate, puedes ignorarla.

> **No necesitas instalar drivers** (chromedriver, geckodriver...). Selenium 4 incluye **Selenium Manager**, que descarga solo el driver correcto la primera vez.

### 1.2 Librerías del proyecto

Maven las descarga solo al abrir el proyecto. Ya están en `pom.xml`:

| Librería | Versión | Para qué |
|---|---|---|
| `selenium-java` | 4.51.0 | Controlar el navegador |
| `junit-jupiter` / `junit-platform-launcher` | 6.0.3 | Tests, tests con datos y ciclo de reportes |
| `poi-ooxml` (Apache POI) | 5.5.1 | Leer datos de Excel y generar el Excel de reportes |
| `gson` | 2.13.1 | Guardar los datos de los reportes (JSON) |
| `log4j-api` / `log4j-core` | 2.24.3 | Logs que pide Apache POI |
| `maven-compiler-plugin` | 3.14.0 | Compilar para Java 21 |
| `maven-surefire-plugin` | 3.5.3 | Ejecutar los tests con `mvn test` |

### 1.3 Compatibilidad con la plantilla de Playwright

Puedes tener los dos proyectos en la misma computadora sin conflictos. Cada uno usa su propio lenguaje y no comparten librerías:

| Herramienta | Playwright | Selenium | ¿Choca? |
|---|---|---|---|
| Git | 2.40+ (tienes 2.56) | 2.40+ (la misma) | No, se comparte |
| Google Chrome | No lo usa (Playwright trae su propio Chromium) | Sí lo usa | No |
| Node.js 24 | Sí | No | No |
| JDK 21 | No | Sí | No |
| Editor | VS Code | IntelliJ IDEA | No, puedes tener ambos |
| Reportes | `reports/` con el mismo formato | `reports/` con el mismo formato | No, cada proyecto tiene su carpeta |

---

## 2. Instalación paso a paso (Windows, en CMD)

### Paso 1: JDK 21
```cmd
winget install EclipseAdoptium.Temurin.21.JDK
```
Cierra y abre CMD. Luego verifica:
```cmd
java -version
```
Debe decir `openjdk version "21.0.x"`.

Revisa también `JAVA_HOME`, que lo necesita Maven por consola:
```cmd
echo %JAVA_HOME%
```
Si sale vacío o `%JAVA_HOME%`, configúralo. Ajusta la carpeta a la que tengas en `C:\Program Files\Eclipse Adoptium\`:
```cmd
dir "C:\Program Files\Eclipse Adoptium"
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
```
Cierra y abre CMD otra vez.

### Paso 2: IntelliJ IDEA
Descárgalo de <https://www.jetbrains.com/idea/download/> (Windows → `.exe`) e instálalo con las opciones por defecto.

### Paso 3: Google Chrome y Git (si no los tienes)
```cmd
winget install Google.Chrome
winget install Git.Git
```

### Paso 4 (opcional): Maven por consola
IntelliJ ya trae Maven, así que este paso solo hace falta si quieres usar `mvn test` en CMD.
1. Descarga el **Binary zip archive** de Maven 3.9.x en <https://maven.apache.org/download.cgi>.
2. Descomprímelo en `C:\Program Files\Maven`.
3. Agrégalo al PATH: tecla Windows → escribe **"variables de entorno"** → **Editar las variables de entorno del sistema** → **Variables de entorno** → en *Variables de usuario* selecciona **Path** → **Editar** → **Nuevo** → pega `C:\Program Files\Maven\apache-maven-3.9.11\bin` → **Aceptar**. No uses `setx PATH`, porque puede recortar tu PATH.
4. Cierra y abre CMD, y verifica con `mvn -v`.

---

## 3. Abrir el proyecto en IntelliJ IDEA

1. **File → Open** y elige la carpeta `selenium-pom-base`. Si te pregunta, ábrelo como **Maven project**.
2. Si aparece **"Trust project?"**, elige **Trust Project**.
3. Ve a **File → Project Structure → Project** y en **SDK** elige **21** (Temurin). Si no aparece, usa **Add SDK → Download JDK → versión 21 → Eclipse Temurin**.
4. Espera a que Maven descargue las dependencias (barra de progreso abajo a la derecha). Si no arranca solo, abre el panel **Maven** (a la derecha) y presiona **Reload All Maven Projects** (ícono de flechas circulares).
5. (Opcional) Copia `config.local.properties.example` como `config.local.properties` para tus datos personales.

### Ejecutar las pruebas
- **Todas:** clic derecho en la carpeta `src/test/java` → **Run 'All Tests'**.
- **Una clase:** abre `LoginTest.java` y presiona el ▶ verde junto a `class LoginTest`.
- **Un solo test:** el ▶ verde junto al método.
- **Con Maven:** en el panel **Maven** → `selenium-pom-base` → `Lifecycle` → doble clic en **test**.

Al terminar, `reports/index.html` se abre solo en tu navegador. También puedes abrirlo en IntelliJ: clic derecho en `reports/index.html` → **Open In → Browser**.

**Actualizar y abrir los reportes** (por ejemplo, después de borrar carpetas de `reports/ejecuciones/` a mano): clic derecho en `src/main/java/reports/ReportBuilder.java` → **Run 'ReportBuilder.main()'**. Por consola: `mvn -q compile exec:java`.

> Si detienes las pruebas con el botón **Stop** (■) de IntelliJ, esa ejecución queda pendiente y no aparece en el índice. Se completa sola en la siguiente ejecución o al correr `ReportBuilder.main()`.

### Por consola (opcional, con Maven instalado)
```cmd
cd ruta\selenium-pom-base
mvn test
mvn test -Dtest=LoginTest
mvn test -Dbrowser=firefox -Dheadless=true
```

---

## 4. Estructura

```
selenium-pom-base/
├── .github/workflows/maven.yml          ← Ejecuta las pruebas en GitHub Actions
├── WebSite/                             ← AQUÍ VA LA PÁGINA WEB DE PRÁCTICA
├── reports/                             ← REPORTES
│   ├── index.html                       ← Página principal (se genera sola)
│   ├── assets/                          ← CSS y JS de los reportes (se suben a Git)
│   │   ├── index.css · index.js         ← Estilos de la página principal
│   │   └── report.css · report.js       ← Estilos del consolidado y de cada caso
│   └── ejecuciones/                     ← Una carpeta por ejecución
│       └── Ejecucion_2026-11-14_215412/
│           ├── Ejecucion_2026-11-14_215412.html   ← Consolidado
│           ├── Ejecucion_2026-11-14_215412.xlsx
│           └── TC-010 Login admin desde Excel - chrome-a1b2c3/
│               ├── reporte_TC-010 Login admin desde Excel - chrome.html   ← Reporte del caso
│               ├── reporte_TC-010 Login admin desde Excel - chrome.pdf
│               └── screenshots/
├── src/main/java/
│   ├── pages/                           ← PAGE OBJECTS
│   │   ├── BasePage.java                ← step(), click(), type(), expectText()...
│   │   ├── login/LoginPage.java
│   │   └── home/HomePage.java
│   ├── reports/                         ← Motor de reportes (normalmente no se toca)
│   │   ├── Report.java                  ← Registra los pasos de cada caso
│   │   ├── ReportBuilder.java           ← Consolidado, PDF, Excel e index.html
│   │   ├── ReportTemplates.java · Html.java   ← HTML de las páginas
│   │   ├── ReportExcel.java · ReportModels.java
│   │   ├── ReportListener.java          ← Inicio y fin de la ejecución
│   │   └── ReportExtension.java         ← Cierra el reporte de cada test
│   └── utils/
│       ├── Config.java                  ← Lee config.properties
│       ├── DriverFactory.java           ← Crea Chrome / Firefox / Edge
│       ├── DataSet.java                 ← Lee el Excel de datos
│       └── WebServer.java               ← Servidor local para WebSite/
├── src/main/resources/
│   ├── Datos/data.xlsx                  ← Datos de prueba (hoja "Login")
│   └── log4j2.xml
├── src/test/java/tests/                 ← PRUEBAS
│   ├── BaseTest.java                    ← Abre/cierra navegador, crea reporte y páginas
│   ├── LoginTest.java
│   └── LoginExcelTest.java              ← Un test por fila del Excel
├── config.properties                    ← Configuración (se sube a Git)
├── config.local.properties.example      ← Copiar como config.local.properties
├── .gitignore
└── pom.xml                              ← Dependencias y versiones
```

> En el zip viene una ejecución de ejemplo con 4 casos aprobados en `reports/ejecuciones/`. Abre `reports/index.html` para ver las páginas. Está en `.gitignore`, así que no se sube a GitHub; bórrala cuando quieras.

---

## 5. Configuración (`config.properties`)

| Clave | Ejemplo | Qué hace |
|---|---|---|
| `project.name` | `Banca en Línea` | Título de los reportes |
| `test.env` | `qa` | Ambiente que aparece en los reportes |
| `base.url` | `https://qa.miapp.com` | URL a probar. Vacía = usa `WebSite/` en `localhost:3000` |
| `browser` | `chrome` / `firefox` / `edge` | Navegador |
| `headless` | `true` / `false` | Sin ventana |
| `timeout.seconds` | `10` | Espera máxima por elemento |
| `user` / `password` | | Credenciales de prueba |
| `report.pdf` | `true` | PDF por caso (usa Google Chrome) |
| `report.screenshots` | `all` / `failed` | Captura en cada paso o solo en fallos |
| `report.open` | `true` | Abrir `index.html` al terminar |
| `blocked.hosts` | `googlesyndication.com, doubleclick.net` | Dominios que no se cargan (anuncios que tapan botones). Solo Chrome y Edge |

**Prioridad:** variable de entorno > `-Dclave=valor` > `config.local.properties` > `config.properties`.

---

## 6. ¿Dónde va la página web?

### A) Sitio de práctica local
Copia los archivos del sitio en `WebSite/`. La página de inicio debe llamarse `index.html`. Deja `base.url=` vacío. Al ejecutar las pruebas, el proyecto levanta solo el sitio en `http://localhost:3000` con el servidor que trae Java, así que no tienes que instalar nada. Para verlo tú mismo: clic derecho en `WebServer.java` → **Run 'WebServer.main()'**.

### B) Una página real en internet

| # | Dónde | Qué cambiar |
|---|---|---|
| 1 | `config.local.properties` | `base.url=https://qa.tu-aplicacion.com` (sin `/` al final) |
| 2 | `config.local.properties` | `user=` y `password=` de un usuario de prueba real |
| 3 | `config.local.properties` | `test.env=qa` y `project.name=...` |
| 4 | `pages/**` | En cada Page Object cambia `path()` (`"/"`, `"/login"`...) y los localizadores `By` |
| 5 | `tests/` y `data.xlsx` | Ajusta los textos esperados y los datos |
| 6 | GitHub | En el repositorio, ve a **Settings → Secrets and variables → Actions** y crea `BASE_URL`, `TEST_USER` y `TEST_PASSWORD`. Luego descomenta esas líneas en `.github/workflows/maven.yml` |

En `open()` siempre se usa `base.url + path()`, así que el mismo test corre en QA o en producción cambiando solo la configuración.

---

## 7. Crear una página y un test

**1. Page Object** (`src/main/java/pages/transferencias/TransferenciasPage.java`):
```java
package pages.transferencias;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.BasePage;
import reports.Report;

public class TransferenciasPage extends BasePage {

    private final By monto = By.id("monto");
    private final By botonTransferir = By.cssSelector("button[type='submit']");
    private final By mensaje = By.cssSelector(".mensaje-exito");

    public TransferenciasPage(WebDriver driver, Report report) { super(driver, report); }

    @Override
    protected String path() { return "/transferencias.html"; }

    public void transferir(String valor) {
        type(monto, valor, "Ingresar monto");
        click(botonTransferir, "Hacer clic en \"Transferir\"");
    }

    public void validarExito() {
        expectText(mensaje, "Transferencia realizada", "Validar mensaje de éxito");
    }
}
```
Cada `click`, `type`, `expectText` o `step` registra el paso con su captura. Si algo falla, registra el error y lanza la excepción, que le llega al `catch` del test.

**2. Regístrala en `BaseTest.java`:**
```java
protected TransferenciasPage transferenciasPage;               // campo
transferenciasPage = new TransferenciasPage(driver, report);    // dentro de setUp()
```

**3. Test con try/catch** (`src/test/java/tests/TransferenciasTest.java`):
```java
package tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utils.Config;

class TransferenciasTest extends BaseTest {

    @Test
    @DisplayName("BAC-002 Transferencia entre cuentas propias")
    void transferenciaPropia() {
        try {
            loginPage.open();
            loginPage.login(Config.user(), Config.password());
            transferenciasPage.open();
            transferenciasPage.transferir("100.00");
            transferenciasPage.validarExito();
        } catch (Throwable e) {
            report.testFailed("Falló la transferencia entre cuentas propias", e);
            throw e;   // ← obligatorio: así JUnit marca el test como FALLIDO
        }
    }
}
```

- **try:** van los pasos del caso.
- **catch:** registra un mensaje claro, que aparece como **motivo de la falla**, y relanza el error. El paso fallido no se duplica.
- **Sin `finally`:** no hace falta cerrar el reporte ni el navegador; lo hacen `ReportExtension` y `BaseTest`.
- El nombre de la clase debe terminar en `Test` para que `mvn test` la encuentre.
- El código al inicio de `@DisplayName` (`TC-001`, `BAC-002`) aparece en los reportes.
- Para agregar notas al reporte: `report.info("...")`.

---

## 8. Subirlo a GitHub como plantilla

```cmd
cd ruta\selenium-pom-base
git init
git add .
git status
git commit -m "Plantilla base Selenium POM"
git branch -M main
git remote add origin https://github.com/TU-USUARIO/selenium-pom-base.git
git push -u origin main
```
En `git status` **no** deben aparecer `target/`, `.idea/`, `config.local.properties` ni nada dentro de `reports/ejecuciones/`, salvo `.gitkeep`. El `.gitignore` los excluye.

En GitHub ve a **Settings → General** y marca **Template repository**. Para cada proyecto nuevo usa **Use this template**.

| No se sube | Por qué |
|---|---|
| `target/` | Compilación; Maven la recrea |
| `.idea/`, `*.iml` | Configuración local de IntelliJ |
| `config.local.properties` | Tus credenciales |
| `reports/index.html`, `reports/ejecuciones/*` | Se generan al ejecutar |
| Dependencias (`.m2`) y drivers | Viven fuera del proyecto, en tu usuario |

---

## 9. Problemas comunes

| Mensaje | Solución |
|---|---|
| `release version 21 not supported` | El SDK del proyecto no es 21. Revisa **File → Project Structure → SDK** |
| Las clases de Selenium aparecen en rojo | Panel **Maven** → **Reload All Maven Projects** |
| `SessionNotCreatedException` | Actualiza Google Chrome. Selenium Manager descarga el driver para tu versión |
| El puerto 3000 ya está en uso | Cambia `web.port` en `config.properties` |
| No se generan los PDF | Necesitan Google Chrome instalado; o pon `report.pdf=false` |
| `ERR_FILE_NOT_FOUND` al abrir un reporte | Ruta de más de 260 caracteres en Windows. Mueve el proyecto a una carpeta corta (ej. `C:\Proyectos\mi-proyecto`) y usa nombres de test cortos |
| El índice muestra una ejecución que borraste | Corre `ReportBuilder.main()` para regenerarlo |
| Un anuncio tapa un botón (`ElementClickInterceptedException`) | Agrega sus dominios en `blocked.hosts` |
| `mvn` no se reconoce | Usa Maven desde IntelliJ o instala Maven (paso 4) |
