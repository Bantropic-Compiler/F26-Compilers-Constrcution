package compiler.ast.statement;

import compiler.ast.Node;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.ast.statement.routine.RoutineCallStatement;
import compiler.lexer.Token;
import compiler.parser.Parser;
import compiler.parser.ParserException;

/** Common base and dispatcher for statements in a block. */
public abstract class Statement extends Node {

    protected Statement(int line, int column) {
        super(line, column);
    }

    public static Statement parse(Parser p) {
        return switch (p.getCurrent().type()) {
            case PRINT -> PrintStatement.parse(p);
            case RETURN -> ReturnStatement.parse(p);
            case BREAK -> BreakStatement.parse(p);
            case CONTINUE -> ContinueStatement.parse(p);
            case IF -> IfStatement.parse(p);
            case WHILE -> WhileStatement.parse(p);
            case FOR -> ForStatement.parse(p);
            case IDENTIFIER -> {
                Token name = p.advance();
                yield RoutineCallStatement.isCallAfterName(p.getCurrent().type())
                        ? RoutineCallStatement.parse(p, name)
                        : AssignmentStatement.parse(p, name);
            }
            default -> throw new ParserException(
                    "expected a statement but found " + p.getCurrent().type(),
                    p.getCurrent().line(), p.getCurrent().column());
        };
    }
}
