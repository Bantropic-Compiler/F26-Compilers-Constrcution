package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SignatureCheckerTest {

    @Test
    void identicalSignaturesMatch() {
        assertEquals(List.of(), diagnostics("""
                routine f(a: integer, b: real): boolean
                routine f(a: integer, b: real): boolean => true
                """));
    }

    @Test
    void parameterNamesMayDiffer() {
        assertEquals(List.of(), diagnostics("""
                routine f(a: integer)
                routine f(z: integer) is
                end
                """));
    }

    @Test
    void differentParameterCountIsReportedAtTheDefinition() {
        assertEquals(List.of("2:1: routine 'f' does not match its forward declaration at 1:1"), diagnostics("""
                routine f(a: integer)
                routine f(a: integer, b: integer) is
                end
                """));
    }

    @Test
    void differentParameterTypeIsReported() {
        assertEquals(List.of("2:1: routine 'f' does not match its forward declaration at 1:1"), diagnostics("""
                routine f(a: integer)
                routine f(a: real) is
                end
                """));
    }

    @Test
    void differentReturnTypeIsReported() {
        assertEquals(List.of("2:1: routine 'f' does not match its forward declaration at 1:1"), diagnostics("""
                routine f(): integer
                routine f(): real => 1.0
                """));
    }

    @Test
    void procedureAndFunctionDoNotMatch() {
        assertEquals(List.of("2:1: routine 'f' does not match its forward declaration at 1:1"), diagnostics("""
                routine f()
                routine f(): integer => 1
                """));
    }

    @Test
    void structurallyEqualArrayTypesMatch() {
        assertEquals(List.of(), diagnostics("""
                routine f(a: array [3] integer)
                routine f(a: array [3] integer) is
                end
                """));
    }

    @Test
    void arrayTypesOfDifferentElementTypeDoNotMatch() {
        assertEquals(List.of("2:1: routine 'f' does not match its forward declaration at 1:1"), diagnostics("""
                routine f(a: array [3] integer)
                routine f(a: array [3] real) is
                end
                """));
    }

    @Test
    void namedRecordTypesMatchByDeclaration() {
        assertEquals(List.of(), diagnostics("""
                type R is record
                    var a: integer
                end
                routine f(r: R)
                routine f(r: R) is
                end
                """));
    }

    @Test
    void unresolvedTypesDoNotProduceAnAdditionalMismatch() {
        assertEquals(List.of("2:14: 'Missing' is not declared"), diagnostics("""
                routine f(a: integer)
                routine f(a: Missing) is
                end
                """));
    }

    @Test
    void plainRoutinesAreNotChecked() {
        assertEquals(List.of(), diagnostics("routine f(a: integer): integer => a\n"));
    }
}
