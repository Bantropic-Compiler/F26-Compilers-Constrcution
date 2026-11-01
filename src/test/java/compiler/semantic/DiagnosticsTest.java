package compiler.semantic;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Node;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticsTest {

    @Test
    void diagnosticFormatsAsLineColumnMessage() {
        assertEquals("3:7: something is wrong", new Diagnostic(3, 7, "something is wrong").toString());
    }

    @Test
    void reportByNodeUsesTheNodePosition() {
        Node node = SemanticTestSupport.parse("\n  var x is 1\n").declarations.get(0);
        Diagnostics diagnostics = new Diagnostics();

        diagnostics.report(node, "bad");

        assertEquals(List.of(new Diagnostic(2, 3, "bad")), diagnostics.all());
    }

    @Test
    void identicalDiagnosticsAreStoredOnce() {
        Diagnostics diagnostics = new Diagnostics();
        diagnostics.report(1, 1, "same");
        diagnostics.report(1, 1, "same");
        diagnostics.report(1, 1, "other");
        diagnostics.report(1, 2, "same");

        assertEquals(3, diagnostics.all().size());
    }

    @Test
    void allKeepsInsertionOrderAndSortedOrdersByPosition() {
        Diagnostics diagnostics = new Diagnostics();
        diagnostics.report(5, 1, "e");
        diagnostics.report(2, 9, "c");
        diagnostics.report(2, 3, "b");
        diagnostics.report(1, 7, "a");

        assertEquals(List.of("5:1: e", "2:9: c", "2:3: b", "1:7: a"),
                diagnostics.all().stream().map(Diagnostic::toString).toList());
        assertEquals(List.of("1:7: a", "2:3: b", "2:9: c", "5:1: e"),
                diagnostics.sorted().stream().map(Diagnostic::toString).toList());
    }

    @Test
    void sortingIsStableForEqualPositions() {
        Diagnostics diagnostics = new Diagnostics();
        diagnostics.report(1, 1, "second letter b");
        diagnostics.report(1, 1, "first letter a");

        assertEquals("second letter b", diagnostics.sorted().get(0).message());
    }

    @Test
    void emptyUntilSomethingIsReported() {
        Diagnostics diagnostics = new Diagnostics();
        assertTrue(diagnostics.isEmpty());
        diagnostics.report(1, 1, "x");
        assertFalse(diagnostics.isEmpty());
    }

    @Test
    void returnedListsAreSnapshots() {
        Diagnostics diagnostics = new Diagnostics();
        diagnostics.report(1, 1, "x");
        List<Diagnostic> snapshot = diagnostics.all();
        diagnostics.report(2, 2, "y");

        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new Diagnostic(3, 3, "z")));
    }
}
