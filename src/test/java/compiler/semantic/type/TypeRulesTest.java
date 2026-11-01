package compiler.semantic.type;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.expression.Expression;
import compiler.semantic.SemanticTestSupport;
import org.junit.jupiter.api.Test;

class TypeRulesTest {

    private static final SemanticType INT = PrimitiveType.INTEGER;
    private static final SemanticType REAL = PrimitiveType.REAL;
    private static final SemanticType BOOL = PrimitiveType.BOOLEAN;
    private static final SemanticType CHAR = PrimitiveType.CHAR;
    private static final SemanticType STR = StringType.INSTANCE;

    private static Expression expr(String source) {
        return SemanticTestSupport.expression(source);
    }

    private static boolean ok(SemanticType target, SemanticType source) {
        return TypeRules.assignable(target, source, expr("x"));
    }

    private static ArrayType array(SemanticType element, long size) {
        return new ArrayType(element, size);
    }

    @Test
    void integerAcceptsIntegerRealAndBoolean() {
        assertTrue(ok(INT, INT));
        assertTrue(ok(INT, REAL));
        assertTrue(ok(INT, BOOL));
        assertFalse(ok(INT, CHAR));
        assertFalse(ok(INT, STR));
    }

    @Test
    void realAcceptsRealIntegerAndBoolean() {
        assertTrue(ok(REAL, REAL));
        assertTrue(ok(REAL, INT));
        assertTrue(ok(REAL, BOOL));
        assertFalse(ok(REAL, CHAR));
        assertFalse(ok(REAL, STR));
    }

    @Test
    void booleanAcceptsBooleanAndOnlyConstantZeroOrOne() {
        assertTrue(ok(BOOL, BOOL));
        assertFalse(ok(BOOL, INT));
        assertFalse(ok(BOOL, REAL));
        assertTrue(TypeRules.assignable(BOOL, INT, expr("0")));
        assertTrue(TypeRules.assignable(BOOL, INT, expr("1")));
        assertTrue(TypeRules.assignable(BOOL, INT, expr("+1")));
        assertTrue(TypeRules.assignable(BOOL, INT, expr("3 - 2")));
        assertFalse(TypeRules.assignable(BOOL, INT, expr("2")));
        assertFalse(TypeRules.assignable(BOOL, INT, expr("-1")));
        assertFalse(TypeRules.assignable(BOOL, INT, expr("n")));
        assertFalse(TypeRules.assignable(BOOL, INT, null));
        assertFalse(TypeRules.assignable(BOOL, REAL, expr("1.0")));
    }

    @Test
    void charAndStringAcceptOnlyThemselves() {
        assertTrue(ok(CHAR, CHAR));
        assertFalse(ok(CHAR, STR));
        assertFalse(ok(CHAR, INT));
        assertTrue(ok(STR, STR));
        assertFalse(ok(STR, CHAR));
        assertFalse(ok(STR, INT));
    }

    @Test
    void recordsRequireTheSameDeclaration() {
        RecordType first = new RecordType("A");
        RecordType second = new RecordType("A");

        assertTrue(ok(first, first));
        assertFalse(ok(first, second));
        assertFalse(ok(first, INT));
        assertFalse(ok(INT, first));
    }

    @Test
    void arraysNeedMatchingElementsAndCompatibleSizes() {
        assertTrue(ok(array(INT, 3), array(INT, 3)));
        assertFalse(ok(array(INT, 3), array(INT, 4)));
        assertFalse(ok(array(INT, 3), array(REAL, 3)));
        assertFalse(ok(array(INT, 3), INT));
        assertFalse(ok(INT, array(INT, 3)));
    }

    @Test
    void unsizedReceiverAcceptsAnySizeButUnsizedSourceFitsOnlyUnsizedReceiver() {
        assertTrue(ok(array(INT, ArrayType.NO_SIZE), array(INT, 3)));
        assertTrue(ok(array(INT, ArrayType.NO_SIZE), array(INT, ArrayType.NO_SIZE)));
        assertFalse(ok(array(INT, 3), array(INT, ArrayType.NO_SIZE)));
        assertFalse(ok(array(INT, ArrayType.NO_SIZE), array(REAL, 3)));
    }

    @Test
    void nestedArraysApplyTheSizeRuleAtEveryLevel() {
        assertTrue(ok(array(array(INT, 3), 2), array(array(INT, 3), 2)));
        assertFalse(ok(array(array(INT, 3), 2), array(array(INT, 4), 2)));
        assertTrue(ok(array(array(INT, ArrayType.NO_SIZE), ArrayType.NO_SIZE), array(array(INT, 4), 2)));
        assertFalse(ok(array(array(INT, 3), 2), array(INT, 2)));
    }

    @Test
    void arraysOfRecordsNeedTheSameRecordDeclaration() {
        RecordType record = new RecordType("R");
        assertTrue(ok(array(record, 2), array(record, 2)));
        assertFalse(ok(array(record, 2), array(new RecordType("R"), 2)));
    }

    @Test
    void errorTypesAreAcceptedInBothDirectionsToAvoidCascades() {
        assertTrue(ok(ErrorType.INSTANCE, INT));
        assertTrue(ok(INT, ErrorType.INSTANCE));
        assertTrue(ok(ErrorType.INSTANCE, ErrorType.INSTANCE));
        assertTrue(ok(array(INT, 3), ErrorType.INSTANCE));
        assertTrue(ok(array(ErrorType.INSTANCE, 3), array(INT, 3)));
    }

    @Test
    void voidIsNeverAssignable() {
        assertFalse(ok(INT, VoidType.INSTANCE));
        assertFalse(ok(VoidType.INSTANCE, INT));
        assertFalse(ok(VoidType.INSTANCE, VoidType.INSTANCE));
    }

    @Test
    void conformsToBooleanAcceptsBooleanAndConstantZeroOrOne() {
        assertTrue(TypeRules.conformsToBoolean(BOOL, expr("b")));
        assertTrue(TypeRules.conformsToBoolean(INT, expr("0")));
        assertTrue(TypeRules.conformsToBoolean(INT, expr("1")));
        assertFalse(TypeRules.conformsToBoolean(INT, expr("2")));
        assertFalse(TypeRules.conformsToBoolean(INT, expr("n")));
        assertFalse(TypeRules.conformsToBoolean(REAL, expr("1.0")));
        assertFalse(TypeRules.conformsToBoolean(CHAR, expr("'a'")));
        assertFalse(TypeRules.conformsToBoolean(STR, expr("\"s\"")));
    }
}
