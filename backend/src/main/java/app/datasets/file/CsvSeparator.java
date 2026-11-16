package app.datasets.file;

public enum CsvSeparator {
    COMMA(','), SEMICOLON(';'), TAB('\t'), PIPE('|');

    private final char character;

    CsvSeparator(char character) {
        this.character = character;
    }

    public char character() {
        return character;
    }
}
