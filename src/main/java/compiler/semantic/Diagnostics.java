package compiler.semantic;

import compiler.ast.Node;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Diagnostics {

    private final List<Diagnostic> items = new ArrayList<>();

    public void report(int line, int column, String message) {
        Diagnostic diagnostic = new Diagnostic(line, column, message);
        if (!items.contains(diagnostic)) {
            items.add(diagnostic);
        }
    }

    public void report(Node node, String message) {
        report(node.line, node.column, message);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<Diagnostic> all() {
        return List.copyOf(items);
    }

    public List<Diagnostic> sorted() {
        List<Diagnostic> result = new ArrayList<>(items);
        result.sort(Comparator.comparingInt(Diagnostic::line).thenComparingInt(Diagnostic::column));
        return List.copyOf(result);
    }
}
