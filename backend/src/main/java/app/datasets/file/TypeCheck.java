package app.datasets.file;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Whether a raw cell value can be loaded into a column of a given type. */
public final class TypeCheck {

    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private TypeCheck() {}

    public static boolean accepts(ColumnType type, String raw) {
        String value = raw.trim();
        if (value.isEmpty()) {
            return true;
        }
        try {
            switch (type) {
                case VARCHAR -> { }
                case BOOLEAN -> {
                    String lower = value.toLowerCase(Locale.ROOT);
                    if (!lower.equals("true") && !lower.equals("false")) {
                        return false;
                    }
                }
                case INTEGER -> Integer.parseInt(value);
                case BIGINT -> Long.parseLong(value);
                case DECIMAL -> new BigDecimal(value);
                case DATE -> parseDate(value);
                case TIMESTAMP -> LocalDateTime.parse(value.replace(' ', 'T'));
            }
            return true;
        } catch (NumberFormatException | DateTimeParseException e) {
            return false;
        }
    }

    private static void parseDate(String value) {
        if (value.contains(".")) {
            LocalDate.parse(value, GERMAN_DATE);
        } else {
            LocalDate.parse(value);
        }
    }
}
