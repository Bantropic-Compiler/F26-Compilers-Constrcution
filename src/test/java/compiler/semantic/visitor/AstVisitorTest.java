package compiler.semantic.visitor;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.statement.IfStatement;
import compiler.semantic.SemanticTestSupport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AstVisitorTest {

    private static final class Recorder extends AstVisitor {
        final List<String> names = new ArrayList<>();

        @Override
        public void visit(Node node) {
            if (node != null) {
                names.add(node.getClass().getSimpleName());
            }
            super.visit(node);
        }
    }

    private static List<String> visited(String source) {
        Recorder recorder = new Recorder();
        recorder.visit(SemanticTestSupport.parse(source));
        return recorder.names;
    }

    @Test
    void visitsStatementsAndExpressionsInSourceOrder() {
        List<String> names = visited("""
                routine f(a: integer): integer is
                    var x is a + 1
                    if x > 0 then
                        print x
                    else
                        return 0
                    end
                    return x
                end
                """);

        assertEquals(List.of("Program", "RoutineDeclaration", "Parameter", "PrimitiveTypeNode",
                "PrimitiveTypeNode", "BlockNode",
                "VariableDeclaration", "BinaryExpression", "ModifiablePrimaryNode", "IntegerLiteral",
                "IfStatement", "BinaryExpression", "ModifiablePrimaryNode", "IntegerLiteral",
                "BlockNode", "PrintStatement", "ModifiablePrimaryNode",
                "BlockNode", "ReturnStatement", "IntegerLiteral",
                "ReturnStatement", "ModifiablePrimaryNode"), names);
    }

    @Test
    void visitsLoopsCallsAccessorsAndTypes() {
        List<String> names = visited("""
                type R is record
                    var f: array[2] integer
                end
                routine g(r: R) is
                    for i in 1..2 reverse loop
                        r.f[i] := h(i)
                    end
                    while true loop
                        break
                    end
                    h(1)
                end
                """);

        assertEquals(List.of("Program",
                "TypeDeclaration", "RecordTypeNode", "VariableDeclaration", "ArrayTypeNode",
                "IntegerLiteral", "PrimitiveTypeNode",
                "RoutineDeclaration", "Parameter", "TypeReferenceNode", "BlockNode",
                "ForStatement", "IntegerLiteral", "IntegerLiteral", "BlockNode",
                "AssignmentStatement", "ModifiablePrimaryNode", "FieldAccess", "IndexAccess",
                "ModifiablePrimaryNode", "RoutineCallExpression", "ModifiablePrimaryNode",
                "WhileStatement", "BooleanLiteral", "BlockNode", "BreakStatement",
                "RoutineCallStatement", "IntegerLiteral"), names);
    }

    @Test
    void visitsEveryLiteralKindAndStringAndFlowStatements() {
        List<String> names = visited("""
                routine f() is
                    print 1.5, 'c', "s", true
                    for v in a loop
                        continue
                    end
                    var s: string
                end
                """);

        assertTrue(names.containsAll(List.of("RealLiteral", "CharLiteral", "StringLiteral",
                "BooleanLiteral", "ContinueStatement", "StringTypeNode", "PrintStatement")));
    }

    @Test
    void visitingNullDoesNothing() {
        Recorder recorder = new Recorder();
        recorder.visit(null);

        assertTrue(recorder.names.isEmpty());
    }

    @Test
    void overridingWithoutCallingSuperStopsDescent() {
        List<String> seen = new ArrayList<>();
        AstVisitor visitor = new AstVisitor() {
            @Override
            public void visit(Node node) {
                if (node != null) {
                    seen.add(node.getClass().getSimpleName());
                }
                super.visit(node);
            }

            @Override
            protected void visitIf(IfStatement node) {
            }
        };

        visitor.visit(SemanticTestSupport.parse("""
                routine f() is
                    if true then
                        print 1
                    end
                    print 2
                end
                """));

        assertFalse(seen.contains("BooleanLiteral"));
        assertEquals(1, seen.stream().filter(name -> name.equals("PrintStatement")).count());
    }

    @Test
    void subclassCanCollectSpecificNodesWhileStillDescending() {
        List<String> operators = new ArrayList<>();
        AstVisitor visitor = new AstVisitor() {
            @Override
            protected void visitBinary(BinaryExpression node) {
                operators.add(node.op.name());
                super.visitBinary(node);
            }
        };

        visitor.visit(SemanticTestSupport.parse("var x is 1 + 2 * 3 - 4\n"));

        assertEquals(List.of("MINUS", "PLUS", "STAR"), operators);
    }

    @Test
    void visitingSiblingsToleratesStructuralChangesDuringTraversal() {
        BlockNode block = ((compiler.ast.declaration.RoutineDeclaration) SemanticTestSupport.parse("""
                routine f() is
                    print 1
                    print 2
                end
                """).declarations.get(0)).body;
        AstVisitor visitor = new AstVisitor() {
            @Override
            protected void visitPrint(compiler.ast.statement.PrintStatement node) {
                block.items.clear();
            }
        };

        assertDoesNotThrow(() -> visitor.visit(block));
    }

    @Test
    void unknownNodeKindsAreRejected() {
        Node unknown = new Node(1, 1) {
        };

        assertThrows(IllegalArgumentException.class, () -> new Recorder().visit(unknown));
    }
}
