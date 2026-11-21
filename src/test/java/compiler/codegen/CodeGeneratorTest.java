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
