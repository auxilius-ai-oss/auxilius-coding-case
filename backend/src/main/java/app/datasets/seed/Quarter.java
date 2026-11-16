package app.datasets.seed;

import java.time.LocalDate;

public record Quarter(int year, int quarter) {

    public LocalDate start() {
        return LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
    }

    public LocalDate end() {
        return start().plusMonths(3).minusDays(1);
    }

    public String label() {
        return "Q" + quarter + " " + year;
    }

    /** The month an extract for this quarter is named after, e.g. 2026-12. */
    public String period() {
        return "%d-%02d".formatted(year, quarter * 3);
    }

    public Quarter next() {
        return quarter == 4 ? new Quarter(year + 1, 1) : new Quarter(year, quarter + 1);
    }

    public boolean isBefore(Quarter other) {
        return year < other.year || (year == other.year && quarter < other.quarter);
    }
}
