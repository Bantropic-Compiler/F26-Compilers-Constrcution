package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ContextCheckerTest {
    @Test
    void rejectsLoopKeywordsOutsideLoopsIncludingInsideIf() {
        assertEquals(List.of(
                "3:9: break is only allowed inside a loop",
                "5:5: continue is only allowed inside a loop"), diagnostics("""
                routine p() is
                    if true then
                        break
                    end
                    continue
                end
                """));
    }

    @Test
    void nestedLoopsRestoreOuterContextAndDoNotLeak() {
        assertEquals(List.of("10:5: break is only allowed inside a loop"), diagnostics("""
                routine p() is
                    while true loop
                        for i in 1..3 loop
                            if true then
                                continue
                            end
                        end
                        break
                    end
                    break
                end
                """));
    }

    @Test
    void checksReturnFormsAcrossRoutines() {
        assertEquals(List.of(
                "2:5: procedure cannot return a value",
                "5:5: function must return a value"), diagnostics("""
                routine p() is
                    return 1
                end
                routine f(): integer is
                    return
                end
                routine q() is
                    return
                end
                routine g(): integer => 1
                """));
    }

    @Test
    void leavesReturnTypeCompatibilityToTypeChecker() {
        assertEquals(List.of("2:12: cannot return 'string' from routine returning 'integer'"), diagnostics("""
                routine f(): integer is
                    return "wrong"
                end
                """));
    }

    @Test
    void stillChecksContextAfterNameErrors() {
        assertEquals(List.of(
                "2:8: 'missing' is not declared",
                "3:9: break is only allowed inside a loop"), diagnostics("""
                routine p() is
                    if missing then
                        break
                    end
                end
                """));
    }
}
