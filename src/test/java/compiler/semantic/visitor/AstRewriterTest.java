package compiler.semantic.visitor;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.PrintStatement;
import compiler.semantic.SemanticTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class AstRewriterTest {

    private static String printed(Program program) {
        return SourcePrinter.print(program);
    }

    private static BlockNode body(Program program, int routine) {
        return ((RoutineDeclaration) program.declarations.get(routine)).body;
    }

    private static final class Scale extends AstRewriter {
        @Override
        protected Expression visitIntegerLiteral(IntegerLiteral node) {
            markChanged();
            return new IntegerLiteral(node.line, node.column, node.value * 10);
        }
    }

    @Test
    void defaultRewriterReturnsTheSameTreeUnchanged() {
        Program program = SemanticTestSupport.parse("""
                type R is record
                    var f: integer
                end
                routine f(a: integer): integer is
                    var x is a + 1
                    for i in 1..3 loop
                        x := x + i
                    end
                    return x
                end
                """);
        String before = program.describe();
        AstRewriter rewriter = new AstRewriter() {
        };

        Program result = rewriter.rewrite(program);

        assertSame(program, result);
        assertEquals(before, result.describe());
        assertFalse(rewriter.changed());
    }

    @Test
    void replacesExpressionsInEveryChildPosition() {
        Program program = SemanticTestSupport.parse("""
                routine f() is
                    var a: array[1 + 1] integer
                    var x is 1
                    x := 2
                    if 3 > 4 then
                        print 5, g(6)
                    end
                    while 7 < 8 loop
                        return 9
                    end
                    for i in 10..11 loop
                        a[12] := 13
                    end
                    g(14)
                end
                """);
        Scale rewriter = new Scale();

        rewriter.rewrite(program);

        assertEquals("""
                routine f() is
                    var a: array[10 + 10] integer
                    var x is 10
                    x := 20
                    if 30 > 40 then
                        print 50, g(60)
                    end
                    while 70 < 80 loop
                        return 90
                    end
                    for i in 100..110 loop
                        a[120] := 130
                    end
                    g(140)
                end
                """, printed(program));
        assertTrue(rewriter.changed());
    }

    @Test
    void changedFlagCanBeReset() {
        Scale rewriter = new Scale();
        rewriter.rewrite(SemanticTestSupport.parse("var x is 1\n"));
        assertTrue(rewriter.changed());

        rewriter.resetChanged();

        assertFalse(rewriter.changed());
    }

    @Test
    void replacesReferencesInsideConditionsAndArguments() {
        Program program = SemanticTestSupport.parse("""
                routine f() is
                    if n > 0 then
                        print n + 1, n
                    end
                end
                """);
        AstRewriter rewriter = new AstRewriter() {
            @Override
            protected Expression visitModifiablePrimary(ModifiablePrimaryNode node) {
                return new IntegerLiteral(node.line, node.column, 5);
            }
        };

        rewriter.rewrite(program);

        assertEquals("""
                routine f() is
                    if 5 > 0 then
                        print 5 + 1, 5
                    end
                end
                """, printed(program));
    }

    @Test
    void rewriteItemCanReplaceOneStatementWithSeveral() {
        Program program = SemanticTestSupport.parse("""
                routine f() is
                    print 1
                    return
                end
                """);
        AstRewriter rewriter = new AstRewriter() {
            @Override
            protected List<Node> rewriteItem(Node item) {
                if (item instanceof PrintStatement print) {
                    markChanged();
                    return List.of(print, print);
                }
                return super.rewriteItem(item);
            }
        };

        rewriter.rewrite(program);

        assertEquals(3, body(program, 0).items.size());
        assertTrue(rewriter.changed());
    }

    @Test
    void returningNullFromAStatementVisitRemovesItFromTheBlock() {
        Program program = SemanticTestSupport.parse("""
                routine f() is
                    while true loop
                        print 1
                        break
                        print 2
                    end
                end
                """);
        AstRewriter rewriter = new AstRewriter() {
            @Override
            protected Node visitBreak(BreakStatement node) {
                return null;
            }
        };

        rewriter.rewrite(program);

        assertEquals("""
                routine f() is
                    while true loop
                        print 1
                        print 2
                    end
                end
                """, printed(program));
    }

    @Test
    void returningNullFromADeclarationRemovesItFromTheProgram() {
        Program program = SemanticTestSupport.parse("type A is integer\nvar x is 1\ntype B is real\n");
        AstRewriter rewriter = new AstRewriter() {
            @Override
            protected Node visitTypeDeclaration(TypeDeclaration node) {
                return null;
            }
        };

        rewriter.rewrite(program);

        assertEquals(1, program.declarations.size());
        assertEquals("var x is 1\n", printed(program));
    }

    @Test
    void replacingANodeThatHasNoReplaceableSlotFailsLoudly() {
        Program program = SemanticTestSupport.parse("routine f() is\n    x := 1\nend\n");
        AstRewriter rewriter = new AstRewriter() {
            @Override
            protected Expression visitModifiablePrimary(ModifiablePrimaryNode node) {
                return new IntegerLiteral(node.line, node.column, 1);
            }
        };

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> rewriter.rewrite(program));

        assertTrue(error.getMessage().contains("cannot be replaced"));
    }

    @Test
    void rewritesNestedBlocksInPlace() {
        Program program = SemanticTestSupport.parse("""
                routine f() is
                    if true then
                        if false then
                            print 1
                        end
                    end
                end
                """);
        Scale rewriter = new Scale();

        rewriter.rewrite(program);

        assertTrue(printed(program).contains("print 10"));
    }
}
