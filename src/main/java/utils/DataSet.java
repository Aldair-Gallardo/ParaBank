package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lectura de datos de prueba desde Excel (src/main/resources/Datos/data.xlsx).
 *
 * Formato de la hoja: la PRIMERA fila son los nombres de columna y cada fila
 * siguiente es un caso. Ejemplo (hoja "Login"):
 *
 *   caseCode | nombreTest          | usuario | password  | esperado
 *   TC-001   | Login satisfactorio | alda    | AldaGa_12 | ok
 *
 * Uso más común:
 *   Map<String, String> datos = DataSet.fila("Login", "TC-001");
 *   datos.get("usuario");   // "alda"
 */
public final class DataSet {

    /** Ruta del Excel de datos. */
    public static final String EXCEL_FILE = "src/main/resources/Datos/data.xlsx";

    private DataSet() {
    }

    /** Lee todas las filas de una hoja. Cada fila es un mapa columna → valor (texto). */
    public static List<Map<String, String>> leerHoja(String hoja) throws IOException {
        return leerHoja(EXCEL_FILE, hoja);
    }

    /** Lee todas las filas de una hoja de otro archivo Excel. */
    public static List<Map<String, String>> leerHoja(String archivo, String hoja) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<Map<String, String>> filas = new ArrayList<>();

        try (InputStream in = Files.newInputStream(Path.of(archivo));
             Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet(hoja);
            if (sheet == null) {
                throw new IllegalArgumentException("La hoja \"" + hoja + "\" no existe en " + archivo);
            }

            // Fila 1: nombres de columna
            Row encabezado = sheet.getRow(sheet.getFirstRowNum());
            List<String> columnas = new ArrayList<>();
            for (int c = 0; c < encabezado.getLastCellNum(); c++) {
                columnas.add(formatter.formatCellValue(encabezado.getCell(c)).trim());
            }

            // Filas siguientes: un caso por fila
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> fila = new LinkedHashMap<>();
                boolean vacia = true;
                for (int c = 0; c < columnas.size(); c++) {
                    if (columnas.get(c).isEmpty()) continue;
                    Cell cell = row.getCell(c);
                    String valor = formatter.formatCellValue(cell).trim();
                    if (!valor.isEmpty()) vacia = false;
                    fila.put(columnas.get(c), valor);
                }
                if (!vacia) filas.add(fila);
            }
        }
        return filas;
    }

    /** Filtra las filas de un código de caso (columna caseCode). */
    public static List<Map<String, String>> porCaso(String hoja, String caseCode) throws IOException {
        List<Map<String, String>> encontradas = leerHoja(hoja).stream()
                .filter(f -> caseCode.equals(f.get("caseCode")))
                .toList();
        if (encontradas.isEmpty()) {
            throw new IllegalArgumentException("No hay datos para el caso " + caseCode + " en la hoja " + hoja);
        }
        return encontradas;
    }

    /** Devuelve la fila de un caso. Uso: DataSet.fila("Login", "TC-001") */
    public static Map<String, String> fila(String hoja, String caseCode) throws IOException {
        return porCaso(hoja, caseCode).get(0);
    }
}