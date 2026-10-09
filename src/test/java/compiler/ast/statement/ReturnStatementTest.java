package compiler.ast.statement;

import compiler.lexer.Lexer;
import compiler.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReturnStatementTest {

    @Test
    void bareReturnHasNullValue() {
        Parser p = new Parser(new Lexer("return"));
        ReturnStatement r = ReturnStatement.parse(p);

        assertNull(r.value);
    }

    @Test
    void wrapBuildsReturnDirectlyFromAnAlreadyParsedValue() {
        ReturnStatement r = ReturnStatement.wrap(3, 23, null);

        assertEquals(3, r.line);
        assertEquals(23, r.column);
        assertNull(r.value);
    }

    // "return <expr>" needs Expression.parse() (Stage 3, Arsen) — add once
    // that's in.
}
