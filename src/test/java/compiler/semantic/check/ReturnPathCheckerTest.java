package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReturnPathCheckerTest {
    @Test
    void rejectsEmptyFunction() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics("""
                routine f(): integer is
                end
                """));
    }

    @Test
    void acceptsForwardProcedureAndExpressionBody() {
        assertEquals(List.of(), diagnostics("""
                routine f(): integer
                routine p() is
                end
                routine f(): integer => 1
                """));
    }

    @Test
    void rejectsMissingElseReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics("""
                routine f(b: boolean): integer is
                    if b then
                        return 1
                    end
                end
                """));
    }

    @Test
    void acceptsNestedReturningBranches() {
        assertEquals(List.of(), diagnostics("""
                routine f(b: boolean, c: boolean): integer is
                    if b then
                        if c then
                            return 1
                        else
                            return 2
                        end
                    else
                        return 3
                    end
                    print 0
                end
                """));
    }

    @Test
    void acceptsFallbackReturnAfterBranchAndLoops() {
        assertEquals(List.of(), diagnostics("""
                routine f(b: boolean): integer is
                    if b then
                        return 1
                    end
                    while b loop
                        break
                        return 2
                    end
                    for i in 1..3 loop
                        continue
                        return 3
                    end
                    return 4
                end
                """));
    }

    @Test
    void loopBodyReturnAloneDoesNotGuaranteeFunctionReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics("""
                routine f(b: boolean): integer is
                    while b loop
                        return 1
                    end
                end
                """));
    }

    @Test
    void emptyRangeCannotGuaranteeReturn() {
        assertEquals(List.of("1:1: function 'f' does not return a value on all paths"), diagnostics("""
                routine f(): integer is
                    for i in 10..1 loop
                        return 1
                    end
                end
                """));
    }

    @Test
    void leavesInvalidReturnFormToC1() {
        assertEquals(List.of("2:5: function must return a value"), diagnostics("""
                routine f(): integer is
                    return
                    return 1
                end
                """));
    }
}
