package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class StatementTest {

    @Test
    void dispatchesSimpleStatements() {
        assertInstanceOf(PrintStatement.class, Statement.parse(new Parser(new Lexer("print 1"))));
        assertInstanceOf(ReturnStatement.class, Statement.parse(new Parser(new Lexer("return"))));
        assertInstanceOf(BreakStatement.class, Statement.parse(new Parser(new Lexer("break"))));
        assertInstanceOf(ContinueStatement.class, Statement.parse(new Parser(new Lexer("continue"))));
    }

    @Test
    void printParsesOneOrMoreExpressions() {
        Parser p = new Parser(new Lexer("print 1, x + 2"));
        PrintStatement statement = assertInstanceOf(PrintStatement.class, Statement.parse(p));

        assertEquals(2, statement.args.size());
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, statement.args.get(0)).value);
        assertInstanceOf(BinaryExpression.class, statement.args.get(1));
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void returnParsesOptionalValue() {
        ReturnStatement withValue = assertInstanceOf(ReturnStatement.class,
                Statement.parse(new Parser(new Lexer("return 1 + 2"))));
        assertInstanceOf(BinaryExpression.class, withValue.value);

        ReturnStatement bare = assertInstanceOf(ReturnStatement.class,
                Statement.parse(new Parser(new Lexer("return"))));
        assertNull(bare.value);
    }

    @Test
    void blockUsesStatementDispatcher() {
        RoutineDeclaration routine = RoutineDeclaration.parse(new Parser(new Lexer(
                "routine show() is\n    print 1, 2\n    return\nend")));

        assertEquals(2, routine.body.items.size());
        assertInstanceOf(PrintStatement.class, routine.body.items.get(0));
        assertInstanceOf(ReturnStatement.class, routine.body.items.get(1));
    }

    @Test
    void printRequiresAnExpression() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("print"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("print 1,"))));
    }
}
