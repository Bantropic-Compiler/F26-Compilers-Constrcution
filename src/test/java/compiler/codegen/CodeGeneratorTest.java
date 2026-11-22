package compiler.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compiler.ast.Program;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticResult;
import compiler.semantic.SemanticTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CodeGeneratorTest {

    private static Map<String, String> generate(String source) {
        Program program = SemanticTestSupport.parse(source);
        SemanticResult result = new SemanticAnalyzer().analyze(program);
        assertEquals(List.of(), result.diagnostics());
        return new CodeGenerator(result.model()).generate(program);
    }

    private static String program(String source) {
        // These assertions inspect source routines independently of launcher dispatch.
        String text = generate(source).get("Program.j");
        return text.substring(0, text.indexOf("\n.method public static main("));
    }

    private static void assertHasLines(String text, String... lines) {
        for (String line : lines) {
            assertTrue(text.contains(line + "\n"), () -> "missing line: " + line + "\nin:\n" + text);
        }
    }

    @Test
    void emptyProgramHasHeaderAndEmptyInitializer() {
        String expected = ".bytecode 49.0\n"
                + ".class public Program\n"
                + ".super java/lang/Object\n"
                + "\n"
                + "\n"
                + ".method static <clinit>()V\n"
                + ".limit stack 64\n"
                + ".limit locals 0\n"
                + "    return\n"
                + ".end method\n";
        assertEquals(expected, program(""));
    }

    @Test
    void scalarGlobalsBecomeStaticFields() {
        String text = program("var a: integer\nvar b: real\nvar c: boolean\nvar d: char\nvar e: string\n");
        assertHasLines(text,
                ".field public static g_a I",
                ".field public static g_b D",
                ".field public static g_c I",
                ".field public static g_d I",
                ".field public static g_e Ljava/lang/String;");
    }

    @Test
    void stringArrayIsFilledWithEmptyStrings() {
        String text = program("var a: array[2] string\n");
        assertHasLines(text,
                "    anewarray java/lang/String",
                "    istore 0",
                "L1:",
                "    if_icmpge L2",
                "    ldc \"\"",
                "    aastore",
                "    iinc 0 1",
                "    goto L1",
                "L2:",
                "    putstatic Program/g_a [Ljava/lang/String;");
        assertHasLines(text, ".limit locals 1");
    }

    @Test
    void nestedArraysGetOneLoopPerLevel() {
        String text = program("var m: array[2] array[3] string\n");
        assertHasLines(text,
                "    anewarray [Ljava/lang/String;",
                "    istore 0",
                "    istore 1",
                "    ldc 3",
                "    anewarray java/lang/String",
                "L4:",
                "    putstatic Program/g_m [[Ljava/lang/String;");
        assertHasLines(text, ".limit locals 2");
    }

    @Test
    void recordGetsClassWithDefaultingConstructor() {
        Map<String, String> files = generate(
                "type P is record\n    var x: integer\n    var y: real\n    var s: string\nend\nvar p: P\n");
        assertEquals(List.of("Program.j", "Rec1.j"), List.copyOf(files.keySet()));
        assertHasLines(files.get("Program.j"),
                ".field public static g_p LRec1;",
                "    new Rec1",
                "    dup",
                "    invokespecial Rec1/<init>()V",
                "    putstatic Program/g_p LRec1;");
        String expected = ".bytecode 49.0\n"
                + ".class public Rec1\n"
                + ".super java/lang/Object\n"
                + "\n"
                + ".field public f_x I\n"
                + ".field public f_y D\n"
                + ".field public f_s Ljava/lang/String;\n"
                + "\n"
                + ".method public <init>()V\n"
                + ".limit stack 64\n"
                + ".limit locals 1\n"
                + "    aload 0\n"
                + "    invokespecial java/lang/Object/<init>()V\n"
                + "    aload 0\n"
                + "    iconst_0\n"
                + "    putfield Rec1/f_x I\n"
                + "    aload 0\n"
                + "    dconst_0\n"
                + "    putfield Rec1/f_y D\n"
                + "    aload 0\n"
                + "    ldc \"\"\n"
                + "    putfield Rec1/f_s Ljava/lang/String;\n"
                + "    return\n"
                + ".end method\n";
        assertEquals(expected, files.get("Rec1.j"));
    }

    @Test
    void recordArrayIsFilledWithNewRecords() {
        String text = program("type P is record\n    var x: integer\nend\nvar a: array[2] P\n");
        assertHasLines(text,
                ".field public static g_a [LRec1;",
                "    anewarray Rec1",
                "    new Rec1",
                "    invokespecial Rec1/<init>()V",
                "    aastore",
                "    putstatic Program/g_a [LRec1;");
    }

    @Test
    void routineBecomesPublicStaticMethod() {
        String text = program("routine f(a: integer, b: real, s: string): real is\n    return b\nend\n");
        assertHasLines(text,
                ".method public static r_f(IDLjava/lang/String;)D",
                ".limit locals 4",
                ".end method");
    }

    @Test
    void localDeclarationsInitializeSlotsInSourceOrder() {
        String text = program("""
                routine f(seed: integer) is
                    var count is seed
                    type Measure is real
                    var value: Measure is count
                    var label: string
                end
                """);
        assertTrue(text.contains("""
                .limit locals 5
                    iload 0
                    istore 1
                    iload 1
                    i2d
                    dstore 2
                    ldc ""
                    astore 4
                    return
                """));
    }

    @Test
    void ifWithoutElseFallsThroughOnFalse() {
        String text = program("""
                routine f(flag: boolean): integer is
                    if flag then return 1 end
                    return 2
                end
                """);
        assertTrue(text.contains("""
                    iload 0
                    ifeq L1
                    ldc 1
                    ireturn
                L1:
                    ldc 2
                    ireturn
                .end method
                """));
    }

    @Test
    void returningBranchesEndRoutine() {
        String text = program("""
                routine f(flag: boolean): integer is
                    if flag then return 1 else return 2 end
                    return 3
                end
                """);
        assertTrue(text.contains("""
                    iload 0
                    ifeq L1
                    ldc 1
                    ireturn
                L1:
                    ldc 2
                    ireturn
                .end method
                """));
    }

    @Test
    void whileChecksConditionBeforeEachIterationAndThenBranchSkipsElse() {
        String text = program("""
                var n: integer
                routine f(flag: boolean) is
                    while n > 0 loop
                        if flag then n := n - 1 else n := 0 end
                    end
                end
                """);
        assertTrue(text.contains("""
                L1:
                    getstatic Program/g_n I
                    ldc 0
                """));
        assertTrue(text.contains("""
                    ifeq L2
                    iload 0
                    ifeq L5
                    getstatic Program/g_n I
                    ldc 1
                    isub
                    putstatic Program/g_n I
                    goto L6
                L5:
                    ldc 0
                    putstatic Program/g_n I
                L6:
                    goto L1
                L2:
                    return
                .end method
                """));
    }

    @Test
    void breakAndContinueTargetTheNearestWhile() {
        String text = program("""
                routine f(outer: boolean, inner: boolean, stop: boolean) is
                    while outer loop
                        while inner loop
                            if stop then break else continue end
                        end
                        if stop then continue else break end
                    end
                end
                """);
        assertTrue(text.contains("""
                L1:
                    iload 0
                    ifeq L2
                L3:
                    iload 1
                    ifeq L4
                    iload 2
                    ifeq L5
                    goto L4
                L5:
                    goto L3
                L4:
                    iload 2
                    ifeq L6
                    goto L1
                L6:
                    goto L2
                L2:
                    return
                .end method
                """));
    }

    @ParameterizedTest
    @CsvSource({"false, 0, 1, 1", "true, 1, 0, -1"})
    void rangeForEvaluatesBoundsOnceAndChecksEndpointBeforeStepping(
            boolean reverse, int lower, int upper, int step) {
        String text = program("""
                routine lo(): integer => 1
                routine hi(): integer => 3
                routine f() is
                    for i in lo()..hi() %s loop
                        var seen is i
                        continue
                    end
                end
                """.formatted(reverse ? "reverse" : ""));
        assertEquals(1, count(text, "invokestatic Program/r_lo()I"));
        assertEquals(1, count(text, "invokestatic Program/r_hi()I"));
        assertTrue(text.contains("""
                    invokestatic Program/r_lo()I
                    istore %d
                    invokestatic Program/r_hi()I
                    istore %d
                    iload %d
                    iload %d
                    if_icmpgt L3
                L1:
                    iload 0
                    istore 2
                    goto L2
                L2:
                    iload 0
                    iload 1
                    if_icmpeq L3
                    iinc 0 %d
                    goto L1
                L3:
                    return
                """.formatted(lower, upper, lower, upper, step)));
    }

    @ParameterizedTest
    @CsvSource({"false, integer, i", "true, real, d", "false, string, a"})
    void arrayForCapturesSourceAndLoadsElementsAtEachIndex(
            boolean reverse, String type, String prefix) {
        String text = program("""
                var values: array[3] %1$s
                routine source(): array[3] %1$s => values
                routine f() is
                    for item in source() %2$s loop
                        var seen is item
                        continue
                    end
                end
                """.formatted(type, reverse ? "reverse" : ""));
        assertEquals(1, count(text, "invokestatic Program/r_source()"));
        String initialIndex = reverse
                ? "    aload 0\n    arraylength\n    iconst_1\n    isub\n"
                : "    iconst_0\n";
        String check = reverse ? "    iflt L3\n" : "    aload 0\n    arraylength\n    if_icmpge L3\n";
        assertTrue(text.contains("    astore 0\n" + initialIndex + "    istore 1\nL1:\n    iload 1\n" + check
                + "    aload 0\n    iload 1\n    " + prefix + "aload\n    " + prefix + "store 2\n"
                + "    " + prefix + "load 2\n"));
        assertTrue(text.contains("    goto L2\nL2:\n    iinc 1 " + (reverse ? -1 : 1)
                + "\n    goto L1\nL3:\n    return\n"));
    }

    @Test
    void evaluatesNestedAssignmentTargetOnceBeforeValue() {
        String text = program("""
                type Row is record
                    var values: array[2] real
                end
                var rows: array[2] Row
                routine index(): integer => 1
                routine value(): integer => 7
                routine update() is
                    rows[index()].values[index()] := value()
                    return
                end
                """);
        assertTrue(text.contains("""
                    getstatic Program/g_rows [LRec1;
                    invokestatic Program/r_index()I
                    iconst_1
                    isub
                    aaload
                    getfield Rec1/f_values [D
                    invokestatic Program/r_index()I
                    iconst_1
                    isub
                    invokestatic Program/r_value()I
                    i2d
                    dastore
                    return
                .end method
                """));
    }

    @Test
    void storesLocalsGlobalsAndFieldsWithConversions() {
        String text = program("""
                type Box is record
                    var n: integer
                end
                var total: real
                routine update(box: Box): Box is
                    var n: integer
                    n := 2.5
                    total := n
                    box.n := n
                    return box
                end
                """);
        assertTrue(text.contains("""
                    ldc2_w 2.5
                    invokestatic java/lang/Math/rint(D)D
                    d2i
                    istore 1
                    iload 1
                    i2d
                    putstatic Program/g_total D
                    aload 0
                    iload 1
                    putfield Rec1/f_n I
                    aload 0
                    areturn
                """));
    }

    @ParameterizedTest
    @CsvSource({"integer, real, iload, i2d, dreturn",
                "real, integer, dload, invokestatic java/lang/Math/rint(D)D, ireturn"})
    void convertsReturnValueToDeclaredType(String source, String target, String load,
                                           String conversion, String instruction) {
        String text = program("routine f(x: " + source + "): " + target + " => x");
        assertHasLines(text, "    " + load + " 0", "    " + conversion, "    " + instruction);
    }

    @Test
    void forwardDeclarationIsEmittedOnce() {
        String text = program("routine g(n: integer)\nroutine g(n: integer) is\nend\n");
        assertEquals(1, count(text, ".method public static r_g(I)V"));
    }

    @Test
    void printsScalarArgumentsWithSpacesAndOneNewline() {
        String text = program("""
                routine show() is
                    print 7, 2.5, true, '😀', "end"
                end
                """);
        String print = "invokevirtual java/io/PrintStream/print";
        assertHasLines(text, "    " + print + "(I)V", "    " + print + "(D)V",
                "    " + print + "(Z)V", "    ldc 128512",
                "    invokestatic java/lang/Character/toString(I)Ljava/lang/String;");
        assertEquals(2, count(text, print + "(Ljava/lang/String;)V"));
        assertEquals(4, count(text, "ldc 32\n    " + print + "(C)V"));
        assertEquals(1, count(text, print + "ln()V"));
        assertTrue(text.contains("ldc \"end\"\n    " + print + "(Ljava/lang/String;)V\n"
                + "    getstatic java/lang/System/out Ljava/io/PrintStream;\n    " + print + "ln()V"));
    }

    @Test
    void discardsOnlyResultsOfStatementCalls() {
        String text = program("""
                routine number(x: real): real is
                    return x
                end
                routine label(): string is
                    return "ok"
                end
                routine noop() is
                end
                routine run() is
                    number(1)
                    label()
                    noop
                end
                """);
        assertTrue(text.contains("""
                    i2d
                    invokestatic Program/r_number(D)D
                    pop2
                    invokestatic Program/r_label()Ljava/lang/String;
                    pop
                    invokestatic Program/r_noop()V
                    return
                """));
    }

    @Test
    void convertsCallArgumentsInEvaluationOrder() {
        String text = program("""
                routine f(a: real, b: integer, c: real, d: integer, e: boolean): real is
                    return a
                end
                var result is f(3, 2.5, true, false, 1)
                """);
        assertTrue(text.contains("""
                    ldc 3
                    i2d
                    ldc2_w 2.5
                    invokestatic java/lang/Math/rint(D)D
                    d2i
                    iconst_1
                    i2d
                    iconst_0
                    ldc 1
                    invokestatic Program/r_f(DIDII)D
                    putstatic Program/g_result D
                """));
    }

    @Test
    void nestedCallsConvertResultsBeforeEvaluatingNextArgument() {
        String text = program("""
                routine first(): integer is
                    return 3
                end
                routine second(): real is
                    return 2.5
                end
                routine combine(a: real, b: integer): integer is
                    return b
                end
                var result: real is combine(first(), second())
                """);
        assertTrue(text.contains("""
                    invokestatic Program/r_first()I
                    i2d
                    invokestatic Program/r_second()D
                    invokestatic java/lang/Math/rint(D)D
                    d2i
                    invokestatic Program/r_combine(DI)I
                    i2d
                    putstatic Program/g_result D
                """));
    }

    @Test
    void passesReferenceArgumentsWithoutConversion() {
        String text = program("""
                type P is record
                    var x: integer
                end
                var p: P
                var values: array[2] integer
                routine label(p: P, values: array[2] integer, s: string): string is
                    return s
                end
                var result is label(p, values, "hello")
                """);
        assertTrue(text.contains("""
                    getstatic Program/g_p LRec1;
                    getstatic Program/g_values [I
                    ldc "hello"
                    invokestatic Program/r_label(LRec1;[ILjava/lang/String;)Ljava/lang/String;
                    putstatic Program/g_result Ljava/lang/String;
                """));
    }

    @Test
    void callUsesForwardDeclaredSignature() {
        String text = program("""
                routine f(n: real): real
                var result is f(7)
                routine f(n: real): real is
                    return n
                end
                """);
        assertTrue(text.contains("""
                    ldc 7
                    i2d
                    invokestatic Program/r_f(D)D
                    putstatic Program/g_result D
                """));
        assertEquals(1, count(text, ".method public static r_f(D)D"));
    }

    @Test
    void readsStringFromRecordArrayUsingOneBasedIndex() {
        String text = program("""
                type P is record
                    var label: string
                end
                var items: array[2] P
                var i is 2
                var length is items[i].label.size
                """);
        assertTrue(text.contains("""
                    getstatic Program/g_items [LRec1;
                    getstatic Program/g_i I
                    iconst_1
                    isub
                    aaload
                    getfield Rec1/f_label Ljava/lang/String;
                    invokevirtual java/lang/String/length()I
                    putstatic Program/g_length I
                """));
    }

    @ParameterizedTest
    @CsvSource({
        "+7, ldc 7", "-7, ineg", "-2.5, dneg", "not 0, ixor",
        "7 + 2, iadd", "7 - 2, isub", "7 * 2, imul", "7 / 2, idiv", "7 % 2, irem",
        "7.0 + 2, dadd", "7 - 2.0, dsub", "7.0 * 2.0, dmul", "7.0 / 2, ddiv",
        "true and false, iand", "true or false, ior", "true xor false, ixor",
        "1 and 0, iand"
    })
    void emitsUnaryArithmeticAndLogicalOperations(String expression, String instruction) {
        assertHasLines(program("var result is " + expression), "    " + instruction);
    }

    @Test
    void widensOperandsInEvaluationOrder() {
        String text = program("var a is 3\nvar b is 2.5\nvar result is (a + b) / (b - a)");
        assertTrue(text.contains("""
                    getstatic Program/g_a I
                    i2d
                    getstatic Program/g_b D
                    dadd
                    getstatic Program/g_b D
                    getstatic Program/g_a I
                    i2d
                    dsub
                    ddiv
                    putstatic Program/g_result D
                """));
    }

    @ParameterizedTest
    @CsvSource({
        "integer, real, 3, D, i2d",
        "boolean, real, true, D, i2d",
        "real, integer, 2.5, I, round",
        "boolean, integer, true, I, none",
        "integer, integer, 3, I, none",
        "real, real, 2.5, D, none"
    })
    void convertsGlobalInitializers(String sourceType, String targetType, String value,
            String descriptor, String conversion) {
        String text = program("var source: " + sourceType + " is " + value
                + "\nvar result: " + targetType + " is source");
        String instructions = switch (conversion) {
            case "i2d" -> "    i2d\n";
            case "round" -> "    invokestatic java/lang/Math/rint(D)D\n    d2i\n";
            default -> "";
        };
        String sourceDescriptor = sourceType.equals("real") ? "D" : "I";
        assertTrue(text.contains("    getstatic Program/g_source " + sourceDescriptor + "\n"
                + instructions + "    putstatic Program/g_result " + descriptor + "\n"));
    }

    @ParameterizedTest
    @CsvSource({"0", "1"})
    void booleanConstantsNeedNoConversion(int value) {
        assertTrue(program("var result: boolean is " + value).contains(
                "    ldc " + value + "\n    putstatic Program/g_result I\n"));
    }

    @ParameterizedTest
    @CsvSource({"=, eq, dcmpl", "/=, ne, dcmpl", "<, lt, dcmpg",
            "<=, le, dcmpg", ">, gt, dcmpl", ">=, ge, dcmpl"})
    void comparisonsProduceBooleanValues(String operator, String condition, String compare) {
        String integers = program("var result is 1 " + operator + " 2");
        assertTrue(integers.contains("    if_icmp" + condition + " L1\n"
                + "    iconst_0\n    goto L2\nL1:\n    iconst_1\nL2:\n"
                + "    putstatic Program/g_result I\n"));
        String reals = program("var result is 1 " + operator + " 2.0");
        assertHasLines(reals, "    i2d", "    " + compare, "    if" + condition + " L1");
    }

    @Test
    void comparesCharactersAndBooleansAsIntegers() {
        assertHasLines(program("var result is 'a' < 'b'"), "    if_icmplt L1");
        assertHasLines(program("var result is true /= false"), "    if_icmpne L1");
    }

    @Test
    void comparesStringsByContentsAndInvertsInequality() {
        String text = program("var a is \"a\"\nvar b is \"b\"\nvar eq is a = b\nvar ne is a /= b");
        assertEquals(2, count(text, "invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z"));
        assertTrue(text.contains("""
                    invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z
                    iconst_1
                    ixor
                    putstatic Program/g_ne I
                """));
    }

    private static int count(String text, String part) {
        int count = 0;
        int from = text.indexOf(part);
        while (from >= 0) {
            count++;
            from = text.indexOf(part, from + part.length());
        }
        return count;
    }
}
