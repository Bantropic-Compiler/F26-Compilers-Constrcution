package compiler.ast.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import compiler.lexer.Lexer;
import compiler.parser.Parser;

class RoutineDeclarationTest {

    @Test
    void forwardDeclarationHasNullBody() {
        Parser p = new Parser(new Lexer("routine isOdd(n: integer): boolean"));
        RoutineDeclaration r = RoutineDeclaration.parse(p);

        assertEquals("isOdd", r.name);
        assertEquals(1, r.parameters.size());
        assertEquals("n", r.parameters.get(0).name);
        assertNotNull(r.returnType);
        assertNull(r.body);
    }

    @Test
    void emptyParameterList() {
        Parser p = new Parser(new Lexer("routine sumArray() is\nend"));
        RoutineDeclaration r = RoutineDeclaration.parse(p);

        assertTrue(r.parameters.isEmpty());
        assertNotNull(r.body);
        assertTrue(r.body.items.isEmpty());
    }

    @Test
    void multipleParameters() {
        Parser p = new Parser(new Lexer("routine maximum(a: integer, b: integer)"));
        RoutineDeclaration r = RoutineDeclaration.parse(p);

        assertEquals(2, r.parameters.size());
        assertEquals("a", r.parameters.get(0).name);
        assertEquals("b", r.parameters.get(1).name);
    }

    @Test
    void noReturnTypeIsNull() {
        Parser p = new Parser(new Lexer("routine sumArray()"));
        RoutineDeclaration r = RoutineDeclaration.parse(p);

        assertNull(r.returnType);
    }

    @Test
    void bodyWithOnlyDeclarations() {
        Parser p = new Parser(new Lexer(
                "routine f() is\n    var x: integer\n    type T is integer\nend"));
        RoutineDeclaration r = RoutineDeclaration.parse(p);

        assertEquals(2, r.body.items.size());
        assertInstanceOf(VariableDeclaration.class, r.body.items.get(0));
        assertInstanceOf(TypeDeclaration.class, r.body.items.get(1));
    }

    // "=> Expression" form needs Expression.parse(). Any body containing an
    // actual statement also needs Statement.parse().
}
