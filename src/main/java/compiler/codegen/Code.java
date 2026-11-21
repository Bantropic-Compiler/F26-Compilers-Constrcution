package compiler.codegen;

import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.PrimitiveType;
import java.util.HashMap;
import java.util.Map;

/** Instructions of one method together with its labels and local slots. */
final class Code {

    private final StringBuilder text = new StringBuilder();
    private final Map<VariableSymbol, Integer> slots = new HashMap<>();
    private int labelCount;
    private int nextSlot;

    void emit(String instruction) {
        text.append("    ").append(instruction).append('\n');
    }

    String newLabel() {
        labelCount++;
        return "L" + labelCount;
    }

    void place(String label) {
        text.append(label).append(":\n");
    }

    /** Reserves local slots and returns the number of the first one. */
    int newLocal(int size) {
        int slot = nextSlot;
        nextSlot += size;
        return slot;
    }

    /** Gives a variable the next free slots: two for a real, one for anything else. */
    int declare(VariableSymbol symbol) {
        int slot = newLocal(symbol.type() == PrimitiveType.REAL ? 2 : 1);
        slots.put(symbol, slot);
        return slot;
    }

    int slotOf(VariableSymbol symbol) {
        Integer slot = slots.get(symbol);
        if (slot == null) {
            throw new IllegalStateException("variable has no slot: " + symbol.name());
        }
        return slot;
    }

    int maxLocals() {
        return nextSlot;
    }

    /** The emitted instructions and labels, one per line. */
    String text() {
        return text.toString();
    }
}
