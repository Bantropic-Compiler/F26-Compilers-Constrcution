package compiler.codegen;

import compiler.semantic.type.ArrayType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.RecordType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.type.StringType;
import compiler.semantic.type.VoidType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps language types and names to JVM descriptors, opcodes and Jasmin text. */
final class Jvm {

    static final String PROGRAM_CLASS = "Program";

    private final Map<RecordType, String> recordClasses = new LinkedHashMap<>();

    /** Descriptor of a type: integer, boolean and char are I, real is D. */
    String descriptor(SemanticType type) {
        if (type == PrimitiveType.REAL) {
            return "D";
        }
        if (type instanceof PrimitiveType) {
            return "I";
        }
        if (type == VoidType.INSTANCE) {
            return "V";
        }
        return referenceDescriptor(type);
    }

    String methodDescriptor(List<SemanticType> parameters, SemanticType result) {
        StringBuilder text = new StringBuilder("(");
        for (SemanticType parameter : parameters) {
            text.append(descriptor(parameter));
        }
        return text.append(')').append(descriptor(result)).toString();
    }

    /** Class name used by new, anewarray and checkcast for a reference type. */
    String className(SemanticType type) {
        if (type == StringType.INSTANCE) {
            return "java/lang/String";
        }
        if (type instanceof RecordType record) {
            return recordClass(record);
        }
        if (type instanceof ArrayType) {
            return descriptor(type);
        }
        throw new IllegalArgumentException("not a reference type: " + type);
    }

    /** Name of the class generated for a record type, assigned on first use. */
    String recordClass(RecordType record) {
        String name = recordClasses.get(record);
        if (name == null) {
            name = "Rec" + (recordClasses.size() + 1);
            recordClasses.put(record, name);
        }
        return name;
    }

    /** Record types met so far, in the order of their class names. */
    List<RecordType> records() {
        return new ArrayList<>(recordClasses.keySet());
    }

    /** Opcode prefix of a type: i, d or a. */
    String prefix(SemanticType type) {
        if (type == PrimitiveType.REAL) {
            return "d";
        }
        if (type instanceof PrimitiveType) {
            return "i";
        }
        return "a";
    }

    String globalName(String name) {
        return "g_" + name;
    }

    String fieldName(String name) {
        return "f_" + name;
    }

    String methodName(String name) {
        return "r_" + name;
    }

    /** Java string quoted for ldc, with every unsafe character written as \\uXXXX. */
    String quote(String text) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= ' ' && c <= '~' && c != '"' && c != '\\') {
                result.append(c);
            } else {
                result.append(String.format("\\u%04x", (int) c));
            }
        }
        return result.append('"').toString();
    }

    private String referenceDescriptor(SemanticType type) {
        if (type == StringType.INSTANCE) {
            return "Ljava/lang/String;";
        }
        if (type instanceof ArrayType array) {
            return "[" + descriptor(array.elementType());
        }
        if (type instanceof RecordType record) {
            return "L" + recordClass(record) + ";";
        }
        throw new IllegalArgumentException("no JVM type for: " + type);
    }
}
