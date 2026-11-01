package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LoopCheckerTest {
    @Test
    void acceptsIntegerExpressionsReverseEmptyRangesAndArrays() {
        assertEquals(List.of(), diagnostics("""
                routine p(n: integer, a: array [] integer) is
                    for i in n + 1..n reverse loop
                        print i
                    end
                    for x in a loop
                        print x
                    end
                end
                """));
    }

    @Test
    void rejectsBothNonIntegerBounds() {
        assertEquals(List.of(
                "2:14: for range bound must be integer, found 'real'",
                "2:19: for range bound must be integer, found 'boolean'"), diagnostics("""
                routine p() is
                    for i in 1.5..true loop
                    end
                end
                """));
    }

    @Test
    void requiresArrayWithoutRange() {
        assertEquals(List.of("2:14: for iteration requires an array, found 'string'"), diagnostics("""
                routine p() is
                    for x in "text" loop
                    end
                end
                """));
    }

    @Test
    void rejectsAssignmentsToBothNestedLoopVariables() {
        assertEquals(List.of(
                "4:13: cannot assign to loop variable 'i'",
                "5:13: cannot assign to loop variable 'j'"), diagnostics("""
                routine p() is
                    for i in 1..3 loop
                        for j in 1..2 loop
                            i := 2
                            j := 1
                        end
                    end
                end
                """));
    }

    @Test
    void permitsAssignmentsToShadowingLocalAndOuterVariableAfterLoop() {
        assertEquals(List.of(), diagnostics("""
                routine p() is
                    var i is 0
                    for i in 1..3 loop
                        var i is 5
                        i := 6
                    end
                    i := 1
                end
                """));
    }

    @Test
    void arrayIterationVariableIsReadOnlyIncludingItsElements() {
        assertEquals(List.of("3:9: cannot assign to loop variable 'row'"), diagnostics("""
                routine p(a: array [] array [2] integer) is
                    for row in a loop
                        row[1] := 0
                    end
                end
                """));
    }

    @Test
    void doesNotCascadeFromUnresolvedSourcesOrTargets() {
        assertEquals(List.of(
                "2:14: 'missing' is not declared",
                "3:9: 'unknown' is not declared",
                "5:14: 'missing' is not declared"), diagnostics("""
                routine p() is
                    for i in missing..3 loop
                        unknown := 1
                    end
                    for x in missing loop
                    end
                end
                """));
    }
}
