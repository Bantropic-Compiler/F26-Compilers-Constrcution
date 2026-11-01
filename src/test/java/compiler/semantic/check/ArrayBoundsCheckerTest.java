package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArrayBoundsCheckerTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "true", "n", "2147483647 + 1"})
    void rejectsInvalidSizes(String size) {
        var errors = diagnostics("var n is 3\nvar a: array [" + size + "] integer\n");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).endsWith("array size must be a positive constant integer"));
    }

    @Test
    void permitsOpenParameterButNotVariableOrAlias() {
        assertEquals(List.of(
                "1:8: array size may be omitted only in a routine parameter",
                "2:11: array size may be omitted only in a routine parameter"), diagnostics("""
                var a: array [] integer
                type A is array [] integer
                routine p(a: array [] integer) is
                end
                """));
    }

    @Test
    void openSizesAreRejectedInReturnTypesAndRecordFields() {
        var errors = diagnostics("""
                routine f(): array [] integer
                routine f(): array [] integer is
                    var a: array [2] integer
                    return a
                end
                routine p(r: record
                    var a: array [] integer
                end) is
                end
                """);
        assertEquals(3, errors.size());
        assertTrue(errors.stream().allMatch(error -> error.endsWith(
                "array size may be omitted only in a routine parameter")));
    }

    @Test
    void checksBothBoundariesAndConstantExpressions() {
        assertEquals(List.of(
                "4:13: array index 0 is outside bounds 1..5",
                "5:13: array index 6 is outside bounds 1..5"), diagnostics("""
                routine p(a: array [2 + 3] integer) is
                    print a[1]
                    print a[5]
                    print a[0]
                    print a[3 + 3]
                end
                """));
    }

    @Test
    void checksLoopOffsetsAndReverse() {
        var errors = diagnostics("""
                routine p(a: array [5] integer) is
                    for i in 1..5 reverse loop
                        print a[i]
                        print a[i + 1]
                        print a[i - 1]
                    end
                end
                """);
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).endsWith("array index 2..6 is outside bounds 1..5"));
        assertTrue(errors.get(1).endsWith("array index 0..4 is outside bounds 1..5"));
    }

    @Test
    void checksNestedArraysAndRecordFields() {
        var errors = diagnostics("""
                type R is record
                    var a: array [2] array [3] integer
                end
                routine p(r: R) is
                    print r.a[3][4]
                end
                """);
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).endsWith("array index 3 is outside bounds 1..2"));
        assertTrue(errors.get(1).endsWith("array index 4 is outside bounds 1..3"));
    }

    @Test
    void unknownIndicesOpenArraysAndShadowingDoNotProduceFalseErrors() {
        assertEquals(List.of(), diagnostics("""
                routine p(a: array [5] integer, b: array [] integer, n: integer) is
                    print a[n], b[100]
                    for i in 6..10 loop
                        var i is 1
                        print a[i]
                    end
                    for j in 10..1 loop
                        print a[j]
                    end
                end
                """));
    }

    @Test
    void nestedLoopsKeepIndependentSymbolRanges() {
        var errors = diagnostics("""
                routine p(a: array [5] integer) is
                    for i in 1..5 loop
                        for i in 6..7 loop
                            print a[i]
                        end
                        print a[i]
                    end
                end
                """);
        assertEquals(List.of("4:21: array index 6..7 is outside bounds 1..5"), errors);
    }

    @Test
    void unresolvedSizeAndWrongIndexTypeDoNotCascade() {
        assertEquals(List.of(
                "1:15: 'missing' is not declared",
                "3:13: array index must be integer, found 'boolean'"), diagnostics("""
                var a: array [missing] integer
                routine p(b: array [5] integer) is
                    print b[true]
                end
                """));
    }
}
