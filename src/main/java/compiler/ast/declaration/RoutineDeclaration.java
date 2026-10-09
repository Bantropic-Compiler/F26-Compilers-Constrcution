package compiler.ast.declaration;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.expression.Expression;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.ast.type.TypeNode;
import java.util.ArrayList;
import java.util.List;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class RoutineDeclaration extends Declaration {

    public final String name;
    public final List<Parameter> parameters;
    public final TypeNode returnType; // nullable
    public final BlockNode body;      // null = forward declaration

    private RoutineDeclaration(int line, int column, String name, List<Parameter> parameters,
                                TypeNode returnType, BlockNode body) {
        super(line, column);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }

    /**
     * RoutineDeclaration : RoutineHeader [ RoutineBody ]
     * RoutineHeader      : routine Identifier ( [ Parameters ] ) [ : Type ]
     * RoutineBody        : is Body end | => Expression
     *
     * "=> Expression" is desugared here into a one-statement BlockNode
     * wrapping a ReturnStatement, so later stages only ever see one
     * body shape. Parameters is treated as optional (the grammar as
     * given requires at least one, but empty parameter lists are used
     * throughout prog-examples/).
     */
    public static RoutineDeclaration parse(Parser p) {
        Token start = p.expect(TokenType.ROUTINE);
        Token nameToken = p.expect(TokenType.IDENTIFIER);
        p.expect(TokenType.LPAREN);

        List<Parameter> parameters = new ArrayList<>();
        if (!p.check(TokenType.RPAREN)) {
            parameters.add(Parameter.parse(p));
            while (p.check(TokenType.COMMA)) {
                p.advance();
                parameters.add(Parameter.parse(p));
            }
        }
        p.expect(TokenType.RPAREN);

        TypeNode returnType = null;
        if (p.check(TokenType.COLON)) {
            p.advance();
            returnType = TypeNode.parse(p);
        }

        BlockNode body;
        if (p.check(TokenType.IS)) {
            p.advance();
            body = BlockNode.parse(p);
            p.expect(TokenType.END);
        } else if (p.check(TokenType.ARROW)) {
            Token arrow = p.advance();
            Expression value = Expression.parse(p);
            List<Node> items = new ArrayList<>();
            items.add(ReturnStatement.wrap(arrow.line(), arrow.column(), value));
            body = BlockNode.wrap(arrow.line(), arrow.column(), items);
        } else {
            body = null; // forward declaration
        }

        return new RoutineDeclaration(start.line(), start.column(),
                (String) nameToken.value(), parameters, returnType, body);
    }
}
