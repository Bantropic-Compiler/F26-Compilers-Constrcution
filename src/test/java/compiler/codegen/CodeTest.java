package compiler.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.PrimitiveType;
import org.junit.jupiter.api.Test;

class CodeTest {

    private final Code code = new Code();

    @Test
    void instructionsAndLabelsAreWrittenOnePerLine() {
        String label = code.newLabel();
        code.emit("iconst_1");
        code.place(label);
        assertEquals("    iconst_1\nL1:\n", code.text());
        assertEquals("L2", code.newLabel());
    }

    @Test
    void localSlotsAreAllocatedBySize() {
        VariableSymbol a = new VariableSymbol("a", null, VariableSymbol.Kind.LOCAL);
        a.setType(PrimitiveType.REAL);
        VariableSymbol b = new VariableSymbol("b", null, VariableSymbol.Kind.LOCAL);
        b.setType(PrimitiveType.INTEGER);
        assertEquals(0, code.declare(a));
        assertEquals(2, code.declare(b));
        assertEquals(2, code.slotOf(b));
        assertEquals(3, code.maxLocals());
    }
}
