package reports;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import reports.ReportModels.CaseResult;
import reports.ReportModels.RunInfo;
import reports.ReportModels.StepLog;

/** Excel consolidado de una ejecución (hojas Resultados, Pasos y Resumen). */
public final class ReportExcel {

    private ReportExcel() {
    }

    public static String write(Path runDir, RunInfo run, List<CaseResult> cases) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);
            CellStyle ok = statusStyle(wb, "D1FAE5", "065F46");
            CellStyle bad = statusStyle(wb, "FEE2E2", "991B1B");
            CellStyle skip = statusStyle(wb, "FEF3C7", "92400E");

            // ── Resultados ──
            Sheet res = wb.createSheet("Resultados");
            String[] cols = {"Código", "Caso", "Navegador", "Estado", "Pasos OK", "Pasos fallidos", "Inicio", "Duración (s)", "Ambiente", "URL", "Motivo de falla"};
            int[] widths = {14, 46, 12, 12, 10, 14, 24, 13, 12, 36, 60};
            headerRow(res, cols, widths, header);
            int r = 1;
            for (CaseResult c : cases) {
                Row row = res.createRow(r++);
                row.createCell(0).setCellValue(c.code);
                row.createCell(1).setCellValue(c.name);
                row.createCell(2).setCellValue(c.browser);
                Cell st = row.createCell(3);
                st.setCellValue(c.status);
                st.setCellStyle(pick(c.status, ok, bad, skip));
                row.createCell(4).setCellValue(c.counts.passed);
                row.createCell(5).setCellValue(c.counts.failed);
                row.createCell(6).setCellValue(Html.fmtDateTime(c.startedAt));
                row.createCell(7).setCellValue(Math.round(c.durationMs / 100.0) / 10.0);
                row.createCell(8).setCellValue(c.environment);
                row.createCell(9).setCellValue(c.baseUrl);
                row.createCell(10).setCellValue(c.reason == null ? "" : c.reason.lines().findFirst().orElse(""));
            }
            res.setAutoFilter(new CellRangeAddress(0, Math.max(0, r - 1), 0, cols.length - 1));

            // ── Pasos ──
            Sheet steps = wb.createSheet("Pasos");
            headerRow(steps, new String[]{"Código", "Caso", "#", "Paso", "Estado", "Hora", "Error"},
                    new int[]{14, 40, 5, 60, 12, 24, 60}, header);
            r = 1;
            for (CaseResult c : cases) {
                for (StepLog s : c.steps) {
                    String label = switch (s.status) {
                        case PASSED -> "EXITOSO";
                        case FAILED -> "FALLIDO";
                        case INFO -> "INFO";
                    };
                    Row row = steps.createRow(r++);
                    row.createCell(0).setCellValue(c.code);
                    row.createCell(1).setCellValue(c.name);
                    row.createCell(2).setCellValue(s.index);
                    row.createCell(3).setCellValue(s.message);
                    Cell st = row.createCell(4);
                    st.setCellValue(label);
                    st.setCellStyle(pick(label, ok, bad, skip));
                    row.createCell(5).setCellValue(Html.fmtDateTime(s.time));
                    row.createCell(6).setCellValue(s.error == null ? "" : s.error.lines().findFirst().orElse(""));
                }
            }

            // ── Resumen ──
            Sheet sum = wb.createSheet("Resumen");
            sum.setColumnWidth(0, 24 * 256);
            sum.setColumnWidth(1, 50 * 256);
            Object[][] data = {
                    {"Proyecto", run.projectName}, {"Ejecución", run.id}, {"Estado", run.status},
                    {"Ambiente", run.environment}, {"URL", run.baseUrl}, {"Navegadores", String.join(", ", run.browsers)},
                    {"Ejecutado por", run.executor}, {"Inicio", Html.fmtDateTime(run.startedAt)},
                    {"Fin", run.endedAt == null ? "" : Html.fmtDateTime(run.endedAt)}, {"", ""},
                    {"Total de casos", run.totals.cases}, {"Exitosos", run.totals.passed}, {"Fallidos", run.totals.failed},
                    {"Omitidos", run.totals.skipped}, {"% de éxito", run.successRate + "%"},
            };
            CellStyle bold = wb.createCellStyle();
            Font bf = wb.createFont();
            bf.setBold(true);
            bold.setFont(bf);
            for (int i = 0; i < data.length; i++) {
                Row row = sum.createRow(i);
                Cell k = row.createCell(0);
                k.setCellValue(String.valueOf(data[i][0]));
                k.setCellStyle(bold);
                Cell v = row.createCell(1);
                if (data[i][1] instanceof Integer n) v.setCellValue(n);
                else v.setCellValue(String.valueOf(data[i][1]));
                if (i == 2) v.setCellStyle(pick(run.status, ok, bad, skip));
            }

            String fileName = "Ejecucion_" + run.id + ".xlsx";
            try (OutputStream out = Files.newOutputStream(runDir.resolve(fileName))) {
                wb.write(out);
            }
            return fileName;
        }
    }

    private static void headerRow(Sheet sheet, String[] cols, int[] widths, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(22);
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static CellStyle pick(String status, CellStyle ok, CellStyle bad, CellStyle skip) {
        return switch (status) {
            case "EXITOSO" -> ok;
            case "FALLIDO" -> bad;
            default -> skip;
        };
    }

    private static XSSFColor color(String hex) {
        return new XSSFColor(new byte[]{
                (byte) Integer.parseInt(hex.substring(0, 2), 16),
                (byte) Integer.parseInt(hex.substring(2, 4), 16),
                (byte) Integer.parseInt(hex.substring(4, 6), 16)}, null);
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color("0B3B3C"));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(color("FFFFFF"));
        s.setFont(f);
        return s;
    }

    private static CellStyle statusStyle(XSSFWorkbook wb, String bg, String fg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(color(bg));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(color(fg));
        s.setFont(f);
        return s;
    }
}
