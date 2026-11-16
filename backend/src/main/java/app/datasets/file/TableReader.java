package app.datasets.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

/** Reads CSV and Excel files into header plus rows of strings. */
public final class TableReader {

    public record Table(String sheetName, List<String> header, List<List<String>> rows, List<String> mergedRanges) {}

    public record Contents(List<Table> tables, CsvSeparator separator) {}

    private TableReader() {}

    public static boolean isExcel(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    /** Reads at most {@code maxRows} data rows per table; pass {@link Integer#MAX_VALUE} for all. */
    public static Contents read(String fileName, Path path, int maxRows) {
        try {
            return isExcel(fileName) ? readExcel(path, maxRows) : readCsv(path, maxRows);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Contents readCsv(Path path, int maxRows) throws IOException {
        List<String> lines = new ArrayList<>();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && lines.size() <= maxRows) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        if (lines.isEmpty()) {
            return new Contents(List.of(new Table(null, List.of(), List.of(), List.of())), CsvSeparator.COMMA);
        }
        CsvSeparator separator = detectSeparator(lines.getFirst());
        List<String> header = splitCsvLine(lines.getFirst(), separator.character());
        List<List<String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            rows.add(splitCsvLine(line, separator.character()));
        }
        return new Contents(List.of(new Table(null, header, rows, List.of())), separator);
    }

    static CsvSeparator detectSeparator(String headerLine) {
        CsvSeparator best = CsvSeparator.COMMA;
        long bestCount = -1;
        for (CsvSeparator candidate : CsvSeparator.values()) {
            long count = headerLine.chars().filter(c -> c == candidate.character()).count();
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    static List<String> splitCsvLine(String line, char separator) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static Contents readExcel(Path path, int maxRows) throws IOException {
        List<Table> tables = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (InputStream in = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            for (Sheet sheet : workbook) {
                List<List<String>> all = new ArrayList<>();
                for (Row row : sheet) {
                    if (all.size() > maxRows) {
                        break;
                    }
                    List<String> values = new ArrayList<>();
                    for (int c = 0; c < Math.max(row.getLastCellNum(), 0); c++) {
                        Cell cell = row.getCell(c);
                        values.add(cell == null ? "" : formatter.formatCellValue(cell));
                    }
                    all.add(values);
                }
                List<String> merged = sheet.getMergedRegions().stream().map(CellRangeAddress::formatAsString).toList();
                if (all.isEmpty()) {
                    tables.add(new Table(sheet.getSheetName(), List.of(), List.of(), merged));
                } else {
                    tables.add(new Table(sheet.getSheetName(), all.getFirst(), all.subList(1, all.size()), merged));
                }
            }
        }
        return new Contents(tables, null);
    }
}
