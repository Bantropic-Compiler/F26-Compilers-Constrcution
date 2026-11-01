package compiler.semantic.check;

import static compiler.TestPrograms.read;
import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArrayBoundsCheckerTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-size-zero.i",
            "invalid-size-negative.i",
            "invalid-size-real.i",
            "invalid-size-boolean.i",
            "invalid-size-variable.i",
            "invalid-size-overflow.i"})
    void rejectsInvalidSizes(String testCase) {
        var errors = diagnostics(read("semantic/array-bounds/" + testCase));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).endsWith("array size must be a positive constant integer"));
    }

    @Test
    void permitsOpenParameterButNotVariableOrAlias() {
        assertEquals(List.of(
                "1:8: array size may be omitted only in a routine parameter",
                "2:11: array size may be omitted only in a routine parameter"), diagnostics(read("semantic/array-bounds/permits-open-parameter-but-not-variable-or-alias.i")));
    }

    @Test
    void openSizesAreRejectedInReturnTypesAndRecordFields() {
        var errors = diagnostics(read("semantic/array-bounds/open-sizes-are-rejected-in-return-types-and-record-fields.i"));
        assertEquals(3, errors.size());
        assertTrue(errors.stream().allMatch(error -> error.endsWith(
                "array size may be omitted only in a routine parameter")));
    }

    @Test
    void checksBothBoundariesAndConstantExpressions() {
        assertEquals(List.of(
                "4:13: array index 0 is outside bounds 1..5",
                "5:13: array index 6 is outside bounds 1..5"), diagnostics(read("semantic/array-bounds/checks-both-boundaries-and-constant-expressions.i")));
    }

    @Test
    void checksLoopOffsetsAndReverse() {
        var errors = diagnostics(read("semantic/array-bounds/checks-loop-offsets-and-reverse.i"));
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).endsWith("array index 2..6 is outside bounds 1..5"));
        assertTrue(errors.get(1).endsWith("array index 0..4 is outside bounds 1..5"));
    }

    @Test
    void checksNestedArraysAndRecordFields() {
        var errors = diagnostics(read("semantic/array-bounds/checks-nested-arrays-and-record-fields.i"));
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).endsWith("array index 3 is outside bounds 1..2"));
        assertTrue(errors.get(1).endsWith("array index 4 is outside bounds 1..3"));
    }

    @Test
    void unknownIndicesOpenArraysAndShadowingDoNotProduceFalseErrors() {
        assertEquals(List.of(), diagnostics(read("semantic/array-bounds/unknown-indices-open-arrays-and-shadowing-do-not-produce-false-errors.i")));
    }

    @Test
    void nestedLoopsKeepIndependentSymbolRanges() {
        var errors = diagnostics(read("semantic/array-bounds/nested-loops-keep-independent-symbol-ranges.i"));
        assertEquals(List.of("4:21: array index 6..7 is outside bounds 1..5"), errors);
    }

    @Test
    void unresolvedSizeAndWrongIndexTypeDoNotCascade() {
        assertEquals(List.of(
                "1:15: 'missing' is not declared",
                "3:13: array index must be integer, found 'boolean'"), diagnostics(read("semantic/array-bounds/unresolved-size-and-wrong-index-type-do-not-cascade.i")));
    }
}
