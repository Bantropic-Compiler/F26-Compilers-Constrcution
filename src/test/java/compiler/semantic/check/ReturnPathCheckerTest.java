package compiler.semantic.check;

import static compiler.TestPrograms.read;
import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReturnPathCheckerTest {
    @Test
    void rejectsEmptyFunction() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics(read("semantic/return-path/rejects-empty-function.i")));
    }

    @Test
    void acceptsForwardProcedureAndExpressionBody() {
        assertEquals(List.of(), diagnostics(read("semantic/return-path/accepts-forward-procedure-and-expression-body.i")));
    }

    @Test
    void rejectsMissingElseReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics(read("semantic/return-path/rejects-missing-else-return.i")));
    }

    @Test
    void acceptsNestedReturningBranches() {
        assertEquals(List.of(), diagnostics(read("semantic/return-path/accepts-nested-returning-branches.i")));
    }

    @Test
    void acceptsFallbackReturnAfterBranchAndLoops() {
        assertEquals(List.of(), diagnostics(read("semantic/return-path/accepts-fallback-return-after-branch-and-loops.i")));
    }

    @Test
    void loopBodyReturnAloneDoesNotGuaranteeFunctionReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics(read("semantic/return-path/loop-body-return-alone-does-not-guarantee-function-return.i")));
    }

    @Test
    void emptyRangeCannotGuaranteeReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics(read("semantic/return-path/empty-range-cannot-guarantee-return.i")));
    }

    @Test
    void leavesInvalidReturnFormToC1() {
        assertEquals(List.of("2:5: function must return a value"), diagnostics(read("semantic/return-path/leaves-invalid-return-form-to-c1.i")));
    }
}
