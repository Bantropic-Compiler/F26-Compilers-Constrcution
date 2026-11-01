package compiler.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.statement.AssignmentStatement;
import compiler.ast.statement.PrintStatement;
import compiler.ast.statement.IfStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.semantic.SemanticTestSupport;
import org.junit.jupiter.api.Test;

class NodeMutabilityTest {

    private static IntegerLiteral literal(long value) {
        return (IntegerLiteral) SemanticTestSupport.expression(Long.toString(value));
    }

    @Test
    void expressionChildrenCanBeReplaced() {
        BinaryExpression sum = (BinaryExpression) SemanticTestSupport.expression("1 + 2");
        IntegerLiteral replacement = literal(9);
        sum.left = replacement;
        sum.right = replacement;

        assertSame(replacement, sum.left);
        assertSame(replacement, sum.right);
    }

    @Test
    void declarationAndStatementSlotsCanBeReplaced() {
        Program program = SemanticTestSupport.parse("""
                var v is 1
                routine t() is
                    var a is 1
                    a := 2
                    if true then end
                    while true loop end
                end
                """);
        VariableDeclaration global = (VariableDeclaration) program.declarations.get(0);
        RoutineDeclaration routine = (RoutineDeclaration) program.declarations.get(1);
        AssignmentStatement assignment = (AssignmentStatement) routine.body.items.get(1);
        IfStatement conditional = (IfStatement) routine.body.items.get(2);
        WhileStatement loop = (WhileStatement) routine.body.items.get(3);
        Expression replacement = literal(7);

        global.initializer = replacement;
        assignment.value = replacement;
        conditional.condition = replacement;
        loop.condition = replacement;

        assertSame(replacement, global.initializer);
        assertSame(replacement, assignment.value);
        assertSame(replacement, conditional.condition);
        assertSame(replacement, loop.condition);
    }

    @Test
    void argumentListsAreMutable() {
        Program program = SemanticTestSupport.parse("""
                routine f(a: integer) is
                end
                routine g(a: integer): integer => a
                routine t() is
                    f(1)
                    print 1
                    var x is g(1)
                end
                """);
        RoutineDeclaration routine = (RoutineDeclaration) program.declarations.get(2);
        PrintStatement print = (PrintStatement) routine.body.items.get(1);
        RoutineCallExpression call = (RoutineCallExpression) ((VariableDeclaration) routine.body.items.get(2)).initializer;

        print.args.add(literal(2));
        call.args.set(0, literal(3));

        assertEquals(2, print.args.size());
        assertEquals(3, ((IntegerLiteral) call.args.get(0)).value);
    }

    @Test
    void reparsingProducesIndependentTrees() {
        Program first = SemanticTestSupport.parse("var x is 1\n");
        Program second = SemanticTestSupport.parse("var x is 1\n");

        assertNotSame(first.declarations.get(0), second.declarations.get(0));
        assertEquals(first.describe(), second.describe());
    }
}
