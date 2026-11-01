package compiler.semantic;

public record Diagnostic(int line, int column, String message) {

    @Override
    public String toString() {
        return line + ":" + column + ": " + message;
    }
}
