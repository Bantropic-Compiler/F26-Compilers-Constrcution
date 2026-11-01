package compiler.semantic.check;

import static compiler.TestPrograms.read;
import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class LoopCheckerTest {
    @Test
    void acceptsIntegerExpressionsReverseEmptyRangesAndArrays() {
        assertEquals(List.of(), diagnostics(read("semantic/loop/accepts-integer-expressions-reverse-empty-ranges-and-arrays.i")));
    }

    @Test
    void rejectsBothNonIntegerBounds() {
        assertEquals(List.of(
                "2:14: for range bound must be integer, found 'real'",
                "2:19: for range bound must be integer, found 'boolean'"), diagnostics(read("semantic/loop/rejects-both-non-integer-bounds.i")));
    }

    @Test
    void requiresArrayWithoutRange() {
        assertEquals(List.of("2:14: for iteration requires an array, found 'string'"), diagnostics(read("semantic/loop/requires-array-without-range.i")));
    }

    @Test
    void rejectsAssignmentsToBothNestedLoopVariables() {
        assertEquals(List.of(
                "4:13: cannot assign to loop variable 'i'",
                "5:13: cannot assign to loop variable 'j'"), diagnostics(read("semantic/loop/rejects-assignments-to-both-nested-loop-variables.i")));
    }

    @Test
    void permitsAssignmentsToShadowingLocalAndOuterVariableAfterLoop() {
        assertEquals(List.of(), diagnostics(read("semantic/loop/permits-assignments-to-shadowing-local-and-outer-variable-after-loop.i")));
    }

    @Test
    void arrayIterationVariableIsReadOnlyIncludingItsElements() {
        assertEquals(List.of("3:9: cannot assign to loop variable 'row'"), diagnostics(read("semantic/loop/array-iteration-variable-is-read-only-including-its-elements.i")));
    }

    @Test
    void doesNotCascadeFromUnresolvedSourcesOrTargets() {
        assertEquals(List.of(
                "2:14: 'missing' is not declared",
                "3:9: 'unknown' is not declared",
                "5:14: 'missing' is not declared"), diagnostics(read("semantic/loop/does-not-cascade-from-unresolved-sources-or-targets.i")));
    }
}
