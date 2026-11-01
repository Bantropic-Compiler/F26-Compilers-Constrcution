package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static compiler.semantic.SemanticTestSupport.inRoutine;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TypeCheckerTest {

    private static List<String> body(String... lines) {
        return diagnostics(inRoutine(lines));
    }

    @Test
    void initializersMustConformToTheDeclaredType() {
        assertEquals(List.of(), body(
                "var a: integer is 1",
                "var b: real is 1",
                "var c: integer is 2.5",
                "var d: boolean is 1",
                "var e: boolean is 0",
                "var f: char is 'x'",
                "var g: string is \"s\"",
                "var h: integer is true"));
        assertEquals(List.of(
                "2:23: cannot initialize 'boolean' with 'integer'",
                "3:20: cannot initialize 'char' with 'string'",
                "4:22: cannot initialize 'string' with 'char'",
                "5:23: cannot initialize 'integer' with 'string'"), body(
                "var a: boolean is 2",
                "var b: char is \"s\"",
                "var c: string is 'x'",
                "var d: integer is \"s\""));
    }

    @Test
    void booleanAcceptsOnlyConstantZeroAndOne() {
        assertEquals(List.of("3:23: cannot initialize 'boolean' with 'integer'"), body(
                "var n is 1",
                "var a: boolean is n"));
        assertEquals(List.of(), body("var a: boolean is 1 - 1", "var b: boolean is 0 + 1"));
    }

    @Test
    void variableTypeIsInferredFromItsInitializer() {
        assertEquals(List.of("5:10: cannot assign 'string' to 'integer'"), body(
                "var a is 1",
                "var b is 1.5",
                "var c is \"s\"",
                "a := c"));
    }

    @Test
    void assignmentRules() {
        assertEquals(List.of(), body(
                "var i is 1",
                "var r is 1.5",
                "var b is true",
                "i := r",
                "r := i",
                "i := b",
                "b := true",
                "b := 1"));
        assertEquals(List.of(
                "5:10: cannot assign 'real' to 'boolean'",
                "6:10: cannot assign 'boolean' to 'string'"), body(
                "var b is true",
                "var s is \"x\"",
                "var r is 1.5",
                "b := r",
                "s := b"));
    }

    @Test
    void recordsAssignOnlyWithinTheSameDeclaration() {
        assertEquals(List.of("12:10: cannot assign 'B' to 'A'"), diagnostics("""
                type A is record
                    var x: integer
                end
                type B is record
                    var x: integer
                end
                routine t() is
                    var a: A
                    var a2: A
                    var b: B
                    a := a2
                    a := b
                end
                """));
    }

    @Test
    void arrayAssignmentRequiresMatchingElementTypeAndSizeWhenBothKnown() {
        assertEquals(List.of("7:10: cannot assign 'array[3] integer' to 'array[2] integer'",
                "8:10: cannot assign 'array[2] real' to 'array[2] integer'"), body(
                "var a: array [2] integer",
                "var b: array [2] integer",
                "var c: array [3] integer",
                "var d: array [2] real",
                "a := b",
                "a := c",
                "a := d"));
    }

    @Test
    void conditionsMustBeBoolean() {
        assertEquals(List.of(
                "2:8: condition of 'if' must be boolean, found 'integer'",
                "3:11: condition of 'while' must be boolean, found 'real'"), body(
                "if 2 then end",
                "while 1.5 loop end"));
        assertEquals(List.of(), body(
                "var b is true",
                "if b then end",
                "if 1 then end",
                "while 1 < 2 loop end"));
    }

    @Test
    void returnTypeIsCheckedAgainstTheRoutineSignature() {
        assertEquals(List.of(), diagnostics("""
                routine a(): integer => 1
                routine b(): real => 1
                routine c(): boolean => 1
                routine d(): integer is
                    return 2.5
                end
                """));
        assertEquals(List.of(
                "1:25: cannot return 'string' from routine returning 'integer'",
                "2:24: cannot return 'integer' from routine returning 'string'"), diagnostics("""
                routine a(): integer => "s"
                routine b(): string => 1
                """));
    }

    @Test
    void callArgumentsAreCheckedByCountAndType() {
        String callee = "routine f(a: integer, b: string): integer => a\n";
        assertEquals(List.of(), diagnostics(callee + inRoutine("print f(1, \"x\")", "print f(true, \"x\")")));
        assertEquals(List.of(
                "3:11: routine 'f' expects 2 argument(s) but 1 given",
                "4:11: routine 'f' expects 2 argument(s) but 3 given",
                "5:16: argument 2 of 'f': cannot pass 'integer' as 'string'"), diagnostics(callee + inRoutine(
                "print f(1)",
                "print f(1, \"x\", 3)",
                "print f(1, 2)")));
    }

    @Test
    void procedureCallsAreCheckedLikeFunctionCalls() {
        assertEquals(List.of(
                "4:5: routine 'p' expects 1 argument(s) but 0 given",
                "5:7: argument 1 of 'p': cannot pass 'string' as 'integer'"), diagnostics("""
                routine p(a: integer) is
                end
                routine t() is
                    p()
                    p("x")
                end
                """));
    }

    @Test
    void aProcedureHasNoValue() {
        assertEquals(List.of("4:14: routine 'p' does not return a value"), diagnostics("""
                routine p() is
                end
                routine t() is
                    var x is p()
                end
                """));
    }

    @Test
    void arithmeticOperatorTypes() {
        assertEquals(List.of(), body(
                "var i is 1",
                "var r is 1.5",
                "var a is i + r",
                "var b is i * i",
                "var c is i / 2",
                "var d is r / 2",
                "var e is i % 3",
                "var s is \"a\" + \"b\""));
        assertEquals(List.of(
                "2:14: operator '+' cannot be applied to 'boolean' and 'integer'",
                "3:14: operator '%' cannot be applied to 'real' and 'integer'"), body(
                "var a is true + 1",
                "var b is 1.5 % 2"));
    }

    @Test
    void comparisonAndLogicalOperatorTypes() {
        assertEquals(List.of(), body(
                "var a is 1 < 2.5",
                "var b is 1 = 1",
                "var c is true and false",
                "var d is true xor true",
                "var e is 'a' = 'b'",
                "var f is \"a\" = \"b\""));
        assertEquals(List.of(
                "2:14: operator 'and' cannot be applied to 'integer' and 'boolean'",
                "3:14: operator '<' cannot be applied to 'boolean' and 'boolean'"), body(
                "var a is 2 and true",
                "var b is true < false"));
    }

    @Test
    void indexingArraysAndStrings() {
        assertEquals(List.of(), body(
                "var a: array [3] integer",
                "var s is \"abc\"",
                "var i is a[1]",
                "a[2] := 5",
                "var c: char is s[1]"));
        assertEquals(List.of(
                "5:13: array index must be integer, found 'real'",
                "6:13: string index must be integer, found 'boolean'",
                "7:12: cannot index a value of type 'integer'"), body(
                "var a: array [3] integer",
                "var s is \"abc\"",
                "var n is 1",
                "print a[1.5]",
                "print s[true]",
                "print n[1]"));
    }

    @Test
    void stringElementsAreReadOnly() {
        assertEquals(List.of("3:5: cannot assign to a string element"), body(
                "var s is \"abc\"",
                "s[1] := 'x'"));
    }

    @Test
    void fieldAccess() {
        String record = """
                type P is record
                    var x: integer
                    var y: real
                end
                """;
        assertEquals(List.of(), diagnostics(record + inRoutine(
                "var p: P",
                "p.x := 1",
                "var v: real is p.y")));
        assertEquals(List.of(
                "8:12: type 'P' has no field 'z'",
                "9:12: cannot access field 'x' of type 'integer'"), diagnostics(record + inRoutine(
                "var p: P",
                "var n is 1",
                "print p.z",
                "print n.x")));
    }

    @Test
    void arraySizeIsReadOnly() {
        assertEquals(List.of(), body("var a: array [3] integer", "var n is a.size"));
        assertEquals(List.of("3:5: cannot assign to 'size'"), body(
                "var a: array [3] integer",
                "a.size := 1"));
    }

    @Test
    void printAcceptsAnyValueType() {
        assertEquals(List.of(), body("print 1, 2.5, true, 'c', \"s\""));
    }

    @Test
    void errorsDoNotCascade() {
        assertEquals(List.of("2:14: 'x' is not declared"), body("var a is x + 1 * 2"));
        assertEquals(List.of("2:5: 'x' is not declared", "2:10: operator '+' cannot be applied to 'integer' and 'boolean'"), body("x := 1 + true"));
    }

    @Test
    void constantExpressionsAreFoldedForBooleanConformance() {
        assertEquals(List.of(), body("var a: boolean is 2 - 1"));
    }
}
