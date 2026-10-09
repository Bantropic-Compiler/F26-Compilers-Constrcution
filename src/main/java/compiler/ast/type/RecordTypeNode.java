package compiler.ast.type;

import compiler.ast.declaration.VariableDeclaration;
import java.util.ArrayList;
import java.util.List;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class RecordTypeNode extends TypeNode {

    public final List<VariableDeclaration> members;

    private RecordTypeNode(int line, int column, List<VariableDeclaration> members) {
        super(line, column);
        this.members = members;
    }

    public static RecordTypeNode parse(Parser p) {
        Token start = p.expect(TokenType.RECORD);
        p.skipSeparators();
        List<VariableDeclaration> members = new ArrayList<>();
        while (!p.check(TokenType.END)) {
            members.add(VariableDeclaration.parse(p));
            p.skipSeparators();
        }
        p.expect(TokenType.END);
        return new RecordTypeNode(start.line(), start.column(), members);
    }
}
