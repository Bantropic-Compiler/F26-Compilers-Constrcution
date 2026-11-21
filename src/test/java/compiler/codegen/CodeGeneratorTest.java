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
        return generate(source).get("Program.j");
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
    void forwardDeclarationIsEmittedOnce() {
        String text = program("routine g(n: integer)\nroutine g(n: integer) is\nend\n");
        assertEquals(1, count(text, ".method public static r_g(I)V"));
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
