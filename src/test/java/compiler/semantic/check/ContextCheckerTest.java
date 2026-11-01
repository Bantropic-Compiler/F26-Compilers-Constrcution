package compiler.semantic.check;

import static compiler.TestPrograms.read;
import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContextCheckerTest {
    @Test
    void rejectsLoopKeywordsOutsideLoopsIncludingInsideIf() {
        assertEquals(List.of(
                "3:9: break is only allowed inside a loop",
                "5:5: continue is only allowed inside a loop"), diagnostics(read("semantic/context/rejects-loop-keywords-outside-loops-including-inside-if.i")));
    }

    @Test
    void nestedLoopsRestoreOuterContextAndDoNotLeak() {
        assertEquals(List.of("10:5: break is only allowed inside a loop"), diagnostics(read("semantic/context/nested-loops-restore-outer-context-and-do-not-leak.i")));
    }

    @Test
    void checksReturnFormsAcrossRoutines() {
        assertEquals(List.of(
                "2:5: procedure cannot return a value",
                "5:5: function must return a value"), diagnostics(read("semantic/context/checks-return-forms-across-routines.i")));
    }

    @Test
    void leavesReturnTypeCompatibilityToTypeChecker() {
        assertEquals(List.of("2:12: cannot return 'string' from routine returning 'integer'"), diagnostics(read("semantic/context/leaves-return-type-compatibility-to-type-checker.i")));
    }

    @Test
    void stillChecksContextAfterNameErrors() {
        assertEquals(List.of(
                "2:8: 'missing' is not declared",
                "3:9: break is only allowed inside a loop"), diagnostics(read("semantic/context/still-checks-context-after-name-errors.i")));
    }
}
