package reports;

import static reports.Html.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import reports.ReportModels.CaseResult;
import reports.ReportModels.CaseSummary;
import reports.ReportModels.RunInfo;
import reports.ReportModels.StepLog;

/**
 * Plantillas HTML de los reportes (el CSS está aparte en reports/assets/).
 * Diseño de "panel lateral + área de trabajo":
 *  - casePage  → lista de pasos + visor grande de capturas
 *  - runPage   → resumen lateral + tarjetas de casos
 *  - indexPage → filtros laterales + mapa de actividad + mosaicos por día
 */
public final class ReportTemplates {

    private ReportTemplates() {
    }

    private static String generatedAt() {
        return "Generado el " + esc(fmtDateTime(java.time.OffsetDateTime.now().toString()));
    }

    private static String fact(String ic, String label, String valueHtml) {
        return "<div class=\"fact\"><dt>" + icon(ic) + esc(label) + "</dt><dd>"
                + (valueHtml == null || valueHtml.isEmpty() ? "—" : valueHtml) + "</dd></div>";
    }

    private static String stat(String label, String value, String tone) {
        return "<div class=\"stat" + (tone.isEmpty() ? "" : " stat-" + tone) + "\"><b>" + value + "</b><span>" + esc(label) + "</span></div>";
    }

    private static String firstLine(String text) {
        if (text == null) return "";
        int i = text.indexOf('\n');
        return i < 0 ? text : text.substring(0, i);
    }

    /** Barra segmentada de pasos: exitosos / fallidos / info. */
    private static String meter(ReportModels.Counts c) {
        int total = Math.max(1, c.total);
        return "<div class=\"meter\" title=\"" + c.passed + " exitosos · " + c.failed + " fallidos\">"
                + "<span class=\"m-ok\" style=\"flex:" + c.passed + "\"></span>"
                + "<span class=\"m-bad\" style=\"flex:" + c.failed + "\"></span>"
                + "<span class=\"m-info\" style=\"flex:" + c.info + "\"></span>"
                + (c.total == 0 ? "<span class=\"m-empty\" style=\"flex:" + total + "\"></span>" : "")
                + "</div>";
    }

    // ═══════════════════════════════════════════════════════════════
    // Reporte de un caso: lista de pasos + visor
    // ═══════════════════════════════════════════════════════════════

    public static String casePage(CaseResult c, String runId, String runHtmlFile, String projectName) {
        int caseRate = rate(c.counts.passed, c.counts.passed + c.counts.failed);
        String toRun = "../" + href(runHtmlFile);
        String toIndex = "../../../index.html";
        String sc = statusClass(c.status);

        // Lista de pasos (izquierda) y versión imprimible (todas las capturas)
        StringBuilder list = new StringBuilder();
        StringBuilder print = new StringBuilder();
        StepLog firstSelected = c.steps.stream().filter(s -> s.status == ReportModels.StepStatus.FAILED).findFirst()
                .orElse(c.steps.stream().filter(s -> s.screenshot != null).findFirst().orElse(c.steps.isEmpty() ? null : c.steps.get(0)));

        for (StepLog s : c.steps) {
            String cls = statusClass(s.status.name());
            boolean selected = s == firstSelected;
            list.append("<li class=\"step-item si-").append(cls).append(selected ? " is-selected" : "").append("\" tabindex=\"0\" data-item data-status=\"").append(cls)
                    .append("\" data-index=\"").append(s.index)
                    .append("\" data-img=\"").append(s.screenshot != null ? href(s.screenshot) : "")
                    .append("\" data-title=\"").append(esc(s.message))
                    .append("\" data-error=\"").append(esc(s.error == null ? "" : s.error))
                    .append("\" data-time=\"").append(esc(fmtTime(s.time) + " · +" + fmtDuration(s.durationMs)))
                    .append("\" data-label=\"").append(esc(s.status == ReportModels.StepStatus.PASSED ? "EXITOSO" : s.status == ReportModels.StepStatus.FAILED ? "FALLIDO" : "INFO"))
                    .append("\">")
                    .append("<span class=\"si-num\">").append(s.index).append("</span>")
                    .append("<span class=\"si-body\"><span class=\"si-title\">").append(esc(s.message)).append("</span>")
                    .append("<span class=\"si-meta\">").append(esc(fmtTime(s.time))).append(" · +").append(esc(fmtDuration(s.durationMs)))
                    .append(s.screenshot != null ? " · " + icon("zoom") : "").append("</span></span>")
                    .append("<span class=\"si-dot\"></span></li>");

            print.append("<article class=\"print-step ps-").append(cls).append("\"><h3><span class=\"ps-num\">").append(s.index).append("</span>")
                    .append(esc(s.message)).append(" ").append(pill(s.status.name())).append("</h3>")
                    .append("<p class=\"ps-time\">").append(esc(fmtTime(s.time))).append(" · +").append(esc(fmtDuration(s.durationMs))).append("</p>");
            if (s.error != null) print.append("<pre class=\"error-box\">").append(esc(s.error)).append("</pre>");
            if (s.screenshot != null) print.append("<img src=\"").append(href(s.screenshot)).append("\" alt=\"Paso ").append(s.index).append("\">");
            print.append("</article>");
        }
        if (c.steps.isEmpty()) list.append("<li class=\"empty\">Este caso no registró pasos.</li>");

        String reason = c.reason == null || c.reason.isEmpty() ? ""
                : "<div class=\"side-alert\"><h3>" + icon("alert") + " Motivo de la falla</h3><pre>" + esc(c.reason) + "</pre></div>";
        String tags = c.tags.stream().map(t -> "<span class=\"chip\">" + esc(t) + "</span>").collect(Collectors.joining());
        String browser = c.browser + (c.browserVersion != null ? " " + c.browserVersion : "");

        String body = """
                <div class="app app-case">
                  <aside class="side side-%s">
                    <nav class="side-nav">
                      <a href="%s">%s Inicio</a>
                      <a href="%s">%s Ejecución %s</a>
                    </nav>
                    <div class="side-head">
                      <div class="status-banner">%s</div>
                      %s
                      <h1>%s</h1>
                      <p class="side-sub">%s</p>
                      <div class="chips">%s</div>
                    </div>
                    <div class="side-score">
                      %s
                      <div class="stats">%s%s%s%s</div>
                    </div>
                    %s
                    <dl class="facts">%s%s%s%s%s%s</dl>
                    <div class="side-actions">%s<a class="btn btn-outline" href="%s">%s Consolidado</a></div>
                    <p class="side-foot">%s<br>%s</p>
                  </aside>

                  <main class="work">
                    <header class="work-head">
                      <div>
                        <p class="eyebrow">Recorrido del caso</p>
                        <h2>%d pasos registrados</h2>
                      </div>
                      <div class="seg" role="group" aria-label="Filtrar pasos">
                        <button type="button" class="seg-btn is-active" data-filter="all">Todos <b>%d</b></button>
                        <button type="button" class="seg-btn" data-filter="ok">Exitosos <b>%d</b></button>
                        <button type="button" class="seg-btn" data-filter="bad">Fallidos <b>%d</b></button>
                      </div>
                    </header>

                    <div class="studio">
                      <ol class="step-list" id="stepList">%s</ol>
                      <section class="viewer" id="viewer" aria-live="polite">
                        <header class="viewer-head">
                          <span class="viewer-num" id="vNum"></span>
                          <div><h3 id="vTitle"></h3><p id="vTime"></p></div>
                          <span class="viewer-label" id="vLabel"></span>
                        </header>
                        <pre class="error-box" id="vError" hidden></pre>
                        <button type="button" class="viewer-stage" id="vStage" title="Ver en pantalla completa">
                          <img id="vImg" alt="">
                          <span class="viewer-empty" id="vEmpty">Este paso no tiene captura</span>
                        </button>
                        <footer class="viewer-nav">
                          <button type="button" class="btn btn-outline" id="vPrev">← Anterior</button>
                          <span>Usa las flechas ← → del teclado</span>
                          <button type="button" class="btn" id="vNext">Siguiente →</button>
                        </footer>
                      </section>
                    </div>

                    <section class="print-only">%s</section>
                  </main>
                </div>

                <div class="lightbox" id="lightbox" hidden>
                  <button type="button" class="lightbox-close" aria-label="Cerrar">%s</button>
                  <img alt="">
                  <p class="lightbox-caption"></p>
                </div>""".formatted(
                sc,
                toIndex, icon("home"), toRun, icon("layers"), esc(runId),
                pill(c.status, true),
                c.code.isEmpty() ? "" : "<span class=\"case-code\">" + icon("tag") + esc(c.code) + "</span>",
                esc(c.name),
                esc(projectName) + " · " + esc(c.testClass),
                tags,
                donut(caseRate, "éxito"),
                stat("Pasos", String.valueOf(c.counts.total), ""),
                stat("Exitosos", String.valueOf(c.counts.passed), "ok"),
                stat("Fallidos", String.valueOf(c.counts.failed), "bad"),
                stat("Duración", esc(fmtDuration(c.durationMs)), ""),
                reason,
                fact("server", "Ambiente", esc(c.environment)),
                fact("browser", "Navegador", esc(browser)),
                fact("link", "URL", esc(c.baseUrl)),
                fact("user", "Ejecutado por", esc(c.executor)),
                fact("calendar", "Inicio", esc(fmtDateTime(c.startedAt))),
                fact("clock", "Fin", esc(fmtDateTime(c.endedAt))),
                c.pdfFile != null ? "<a class=\"btn\" href=\"" + href(c.pdfFile) + "\">" + icon("pdf") + " Descargar PDF</a>" : "",
                toRun, icon("layers"),
                esc(c.id), generatedAt(),
                c.counts.total, c.counts.total, c.counts.passed, c.counts.failed,
                list,
                print,
                icon("x"));

        return layout((c.code.isEmpty() ? "" : c.code + " · ") + c.name,
                "../../../assets/report.css", "../../../assets/report.js", body, "page-case");
    }

    // ═══════════════════════════════════════════════════════════════
    // Consolidado: resumen lateral + tarjetas de casos
    // ═══════════════════════════════════════════════════════════════

    public static String runPage(RunInfo run) {
        var t = run.totals;
        StringBuilder cards = new StringBuilder();
        int i = 1;
        for (CaseSummary c : run.cases) {
            String cls = statusClass(c.status);
            cards.append("<article class=\"case-card cc-").append(cls).append("\" data-item data-status=\"").append(cls)
                    .append("\" data-text=\"").append(esc((c.code + " " + c.name + " " + c.browser).toLowerCase())).append("\">")
                    .append("<div class=\"cc-top\"><span class=\"cc-num\">").append(String.format("%02d", i++)).append("</span>")
                    .append(c.code.isEmpty() ? "" : "<span class=\"case-code\">" + esc(c.code) + "</span>")
                    .append(pill(c.status)).append("</div>")
                    .append("<h3><a href=\"").append(href(c.htmlPath)).append("\">").append(esc(c.name)).append("</a></h3>");
            if (c.reason != null) cards.append("<p class=\"cc-reason\">").append(icon("alert")).append(esc(firstLine(c.reason))).append("</p>");
            cards.append(meter(c.counts))
                    .append("<ul class=\"cc-meta\"><li>").append(icon("check")).append(c.counts.passed).append(" ok</li>")
                    .append("<li>").append(icon("x")).append(c.counts.failed).append(" fallidos</li>")
                    .append("<li>").append(icon("clock")).append(esc(fmtDuration(c.durationMs))).append("</li>")
                    .append("<li>").append(icon("browser")).append(esc(c.browser)).append("</li></ul>")
                    .append("<div class=\"cc-actions\"><a class=\"btn\" href=\"").append(href(c.htmlPath)).append("\">").append(icon("file")).append(" Ver reporte</a>");
            if (c.pdfPath != null) {
                cards.append("<a class=\"btn btn-outline\" href=\"").append(href(c.pdfPath)).append("\">").append(icon("pdf")).append(" PDF</a>");
            }
            cards.append("</div></article>");
        }
        if (run.cases.isEmpty()) cards.append("<p class=\"empty\">Sin casos registrados.</p>");

        String browsers = run.browsers.stream().map(b -> "<span class=\"chip\">" + icon("browser") + esc(b) + "</span>").collect(Collectors.joining());

        String body = """
                <div class="app app-run">
                  <aside class="side side-%s">
                    <nav class="side-nav"><a href="../../index.html">%s Inicio</a></nav>
                    <div class="side-head">
                      <div class="status-banner">%s</div>
                      <p class="eyebrow">Ejecución / regresión</p>
                      <h1>%s</h1>
                      <p class="side-sub">%s</p>
                      <div class="chips"><span class="chip">%s%s</span>%s</div>
                    </div>
                    <div class="side-score">
                      %s
                      <div class="stats">%s%s%s%s</div>
                    </div>
                    <dl class="facts">%s%s%s%s%s%s%s%s%s</dl>
                    <div class="side-actions">%s</div>
                    <p class="side-foot">%s<br>%s</p>
                  </aside>

                  <main class="work">
                    <header class="work-head">
                      <div>
                        <p class="eyebrow">Casos ejecutados</p>
                        <h2>%d casos · %d%% de éxito</h2>
                      </div>
                      <div class="tools">
                        <label class="search">%s<input type="search" id="search" placeholder="Buscar caso, código o navegador"></label>
                        <div class="seg" role="group" aria-label="Filtrar casos">
                          <button type="button" class="seg-btn is-active" data-filter="all">Todos <b>%d</b></button>
                          <button type="button" class="seg-btn" data-filter="ok">Exitosos <b>%d</b></button>
                          <button type="button" class="seg-btn" data-filter="bad">Fallidos <b>%d</b></button>
                        </div>
                      </div>
                    </header>
                    <div class="case-grid">%s</div>
                  </main>
                </div>""".formatted(
                statusClass(run.status),
                icon("home"),
                pill(run.status, true),
                esc(fmtDateTime(run.startedAt)),
                esc(run.projectName),
                icon("server"), esc(run.environment), browsers,
                donut(run.successRate, "éxito"),
                stat("Casos", String.valueOf(t.cases), ""),
                stat("Exitosos", String.valueOf(t.passed), "ok"),
                stat("Fallidos", String.valueOf(t.failed), "bad"),
                stat("Omitidos", String.valueOf(t.skipped), "skip"),
                fact("tag", "ID", esc(run.id)),
                fact("link", "URL", esc(run.baseUrl)),
                fact("user", "Ejecutado por", esc(run.executor)),
                fact("calendar", "Inicio", esc(fmtDateTime(run.startedAt))),
                fact("clock", "Duración", esc(fmtDuration(run.durationMs))),
                fact("layers", "Pasos totales", String.valueOf(t.steps)),
                fact("cpu", "Java", esc(run.system.java)),
                fact("play", "Selenium", esc(run.system.selenium)),
                fact("server", "Sistema", esc(run.system.os)),
                run.excelFile != null ? "<a class=\"btn\" href=\"" + href(run.excelFile) + "\">" + icon("sheet") + " Descargar Excel</a>" : "",
                esc(run.folder), generatedAt(),
                t.cases, run.successRate,
                icon("search"), t.cases, t.passed, t.failed,
                cards);

        return layout("Ejecución " + run.id + " · " + run.projectName,
                "../../assets/report.css", "../../assets/report.js", body, "page-run");
    }

    // ═══════════════════════════════════════════════════════════════
    // Página principal: filtros laterales + actividad + mosaicos
    // ═══════════════════════════════════════════════════════════════

    public static String indexPage(List<RunInfo> runs, String projectName) {
        List<RunInfo> sorted = new ArrayList<>(runs);
        sorted.sort(Comparator.comparing((RunInfo r) -> r.startedAt).reversed());

        int totalCases = sorted.stream().mapToInt(r -> r.totals.cases).sum();
        int totalPassed = sorted.stream().mapToInt(r -> r.totals.passed).sum();
        int totalFailed = sorted.stream().mapToInt(r -> r.totals.failed).sum();
        RunInfo last = sorted.isEmpty() ? null : sorted.get(0);
        LinkedHashSet<String> environments = sorted.stream().map(r -> r.environment).collect(Collectors.toCollection(LinkedHashSet::new));

        // Mapa de actividad: últimas 30 ejecuciones, de la más antigua a la más reciente.
        List<RunInfo> recent = new ArrayList<>(sorted.subList(0, Math.min(30, sorted.size())));
        java.util.Collections.reverse(recent);
        StringBuilder heat = new StringBuilder();
        for (RunInfo r : recent) {
            String tone = r.totals.failed == 0 ? "ok" : r.successRate >= 70 ? "warn" : "bad";
            heat.append("<a class=\"cell cell-").append(tone).append("\" href=\"").append(href("ejecuciones/" + r.folder + "/" + r.htmlFile))
                    .append("\" title=\"").append(esc(fmtDateTime(r.startedAt) + " — " + r.totals.passed + " ✓ / " + r.totals.failed + " ✗ (" + r.successRate + "%)"))
                    .append("\"><span>").append(r.successRate).append("</span></a>");
        }
        for (int k = recent.size(); k < 30; k++) heat.append("<span class=\"cell cell-empty\"></span>");

        Map<String, List<RunInfo>> groups = new LinkedHashMap<>();
        for (RunInfo r : sorted) groups.computeIfAbsent(dayKey(r.startedAt), k -> new ArrayList<>()).add(r);

        StringBuilder timeline = new StringBuilder();
        int counter = sorted.size();
        for (var entry : groups.entrySet()) {
            List<RunInfo> list = entry.getValue();
            int dayOk = list.stream().mapToInt(r -> r.totals.passed).sum();
            int dayBad = list.stream().mapToInt(r -> r.totals.failed).sum();
            timeline.append("<section class=\"day\" data-day=\"").append(entry.getKey()).append("\">")
                    .append("<header class=\"day-head\"><h2>").append(esc(fmtLongDay(list.get(0).startedAt))).append("</h2>")
                    .append("<span>").append(list.size()).append(list.size() == 1 ? " ejecución" : " ejecuciones")
                    .append(" · ").append(dayOk).append(" ✓ · ").append(dayBad).append(" ✗</span></header>")
                    .append("<div class=\"day-runs\">");
            for (RunInfo r : list) timeline.append(runTile(r, counter--));
            timeline.append("</div></section>");
        }
        if (sorted.isEmpty()) {
            timeline.append("<div class=\"empty-state\">").append(icon("info"))
                    .append("<h2>Aún no hay ejecuciones</h2><p>Ejecuta las pruebas y vuelve a abrir esta página.</p></div>");
        }

        String lastRun = last == null ? "" : """
                <a class="last-run lr-%s" href="%s">
                  <span>Última ejecución</span>
                  <strong>%s</strong>
                  %s
                </a>""".formatted(statusClass(last.status), href("ejecuciones/" + last.folder + "/" + last.htmlFile),
                esc(fmtDateTime(last.startedAt)), pill(last.status));

        String envFilter = environments.size() <= 1 ? "" : "<label class=\"side-label\">Ambiente</label><select id=\"envFilter\" class=\"select\"><option value=\"\">Todos</option>"
                + environments.stream().map(e -> "<option>" + esc(e) + "</option>").collect(Collectors.joining()) + "</select>";

        String body = """
                <div class="app app-index">
                  <aside class="side side-brand">
                    <div class="side-head">
                      <p class="eyebrow">%s Selenium · Reportes</p>
                      <h1>%s</h1>
                      <p class="side-sub">Historial de regresiones y ejecuciones</p>
                    </div>
                    <div class="side-score">
                      %s
                      <div class="stats">%s%s%s%s</div>
                    </div>
                    %s
                    <div class="side-filters">
                      <label class="side-label" for="runSearch">Buscar</label>
                      <label class="search">%s<input type="search" id="runSearch" placeholder="Fecha, caso, código..."></label>
                      <label class="side-label">Estado</label>
                      <div class="seg seg-col" role="group" aria-label="Filtrar por estado">
                        <button type="button" class="seg-btn is-active" data-filter="all">Todas</button>
                        <button type="button" class="seg-btn" data-filter="ok">Exitosas</button>
                        <button type="button" class="seg-btn" data-filter="bad">Con fallos</button>
                      </div>
                      %s
                      <label class="side-label">Orden</label>
                      <button type="button" class="btn btn-outline btn-block" id="sortToggle" data-order="desc">%s <span>Más recientes primero</span></button>
                    </div>
                    <p class="side-foot">%s</p>
                  </aside>

                  <main class="work">
                    <section class="activity">
                      <header class="work-head">
                        <div>
                          <p class="eyebrow">Actividad</p>
                          <h2>Últimas %d ejecuciones</h2>
                        </div>
                        <div class="legend"><span><i class="cell-ok"></i>Sin fallos</span><span><i class="cell-warn"></i>≥ 70%%</span><span><i class="cell-bad"></i>&lt; 70%%</span></div>
                      </header>
                      <div class="heat">%s</div>
                      <p class="heat-note">Cada cuadro es una ejecución (de la más antigua a la más reciente). El número es su %% de éxito.</p>
                    </section>
                    <div id="timeline">%s</div>
                    <p class="no-results" id="noResults" hidden>No hay ejecuciones que coincidan con el filtro.</p>
                  </main>
                </div>""".formatted(
                icon("layers"), esc(projectName),
                donut(rate(totalPassed, totalCases), "global"),
                stat("Ejecuciones", String.valueOf(sorted.size()), ""),
                stat("Casos", String.valueOf(totalCases), ""),
                stat("Exitosos", String.valueOf(totalPassed), "ok"),
                stat("Fallidos", String.valueOf(totalFailed), "bad"),
                lastRun,
                icon("search"), envFilter, icon("sort"),
                generatedAt(),
                recent.size(), heat,
                timeline);

        return layout("Reportes · " + projectName, "assets/index.css", "assets/index.js", body, "page-index");
    }

    private static String runTile(RunInfo r, int number) {
        String cls = statusClass(r.status);
        String base = "ejecuciones/" + r.folder + "/";

        StringBuilder cases = new StringBuilder();
        for (CaseSummary c : r.cases) {
            cases.append("<li class=\"mini-case mc-").append(statusClass(c.status)).append("\">")
                    .append("<a href=\"").append(href(base + c.htmlPath)).append("\">")
                    .append(c.code.isEmpty() ? "" : "<b>" + esc(c.code) + "</b> ").append(esc(c.name)).append("</a>")
                    .append(c.pdfPath != null ? "<a class=\"mc-pdf\" href=\"" + href(base + c.pdfPath) + "\" title=\"PDF\">" + icon("pdf") + "</a>" : "")
                    .append("</li>");
        }

        String searchText = (r.id + " " + r.environment + " " + String.join(" ", r.browsers) + " " + r.executor + " "
                + r.cases.stream().map(c -> c.code + " " + c.name).collect(Collectors.joining(" "))).toLowerCase();

        return """
                <article class="run tile-%s" data-ts="%s" data-status="%s" data-env="%s" data-text="%s">
                  <header class="tile-head">
                    <div class="tile-time"><strong>%s</strong><span>Ejecución #%d</span></div>
                    %s
                  </header>
                  <div class="tile-body">
                    %s
                    <div class="tile-info">
                      <a class="tile-title" href="%s">%s</a>
                      <div class="tile-counts"><span class="t-ok">%s %d</span><span class="t-bad">%s %d</span>%s</div>
                      <div class="chips"><span class="chip">%s%s</span><span class="chip">%s%s</span><span class="chip">%s%s</span><span class="chip">%s%s</span></div>
                    </div>
                  </div>
                  %s
                  <footer class="tile-actions">
                    <a class="btn" href="%s">%s Consolidado</a>%s
                  </footer>
                </article>""".formatted(
                cls, esc(r.startedAt), cls, esc(r.environment), esc(searchText),
                esc(fmtTime(r.startedAt)), number, pill(r.status),
                donut(r.successRate, "éxito"),
                href(base + r.htmlFile), esc(r.id),
                icon("check"), r.totals.passed, icon("x"), r.totals.failed,
                r.totals.skipped > 0 ? "<span class=\"t-skip\">" + icon("minus") + " " + r.totals.skipped + "</span>" : "",
                icon("server"), esc(r.environment), icon("browser"), esc(String.join(", ", r.browsers)),
                icon("user"), esc(r.executor), icon("clock"), esc(fmtDuration(r.durationMs)),
                r.cases.isEmpty() ? "" : "<details class=\"tile-cases\"><summary>" + r.cases.size() + (r.cases.size() == 1 ? " caso" : " casos")
                        + "</summary><ul>" + cases + "</ul></details>",
                href(base + r.htmlFile), icon("layers"),
                r.excelFile != null ? "<a class=\"btn btn-outline\" href=\"" + href(base + r.excelFile) + "\">" + icon("sheet") + " Excel</a>" : "");
    }
}
