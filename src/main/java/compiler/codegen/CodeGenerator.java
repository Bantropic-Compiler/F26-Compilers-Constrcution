package compiler.codegen;

import compiler.ast.Program;
import compiler.ast.declaration.Declaration;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.type.ArrayType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.RecordType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.type.StringType;
import compiler.semantic.type.VoidType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns an analyzed program into Jasmin source: one class for the program and one per record type. */
public final class CodeGenerator {

    private static final int MAX_STACK = 64;

    private final SemanticModel model;
    private final Jvm jvm = new Jvm();

    public CodeGenerator(SemanticModel model) {
        this.model = model;
    }

    /** Returns Jasmin text by file name: Program.j first, then one file per record class. */
    public Map<String, String> generate(Program program) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(Jvm.PROGRAM_CLASS + ".j", programClass(program));
        List<RecordType> records = jvm.records();
        for (int i = 0; i < records.size(); i++) {
            files.put(jvm.recordClass(records.get(i)) + ".j", recordClass(records.get(i)));
            records = jvm.records();
        }
        return files;
    }

    private String programClass(Program program) {
        StringBuilder text = new StringBuilder();
        text.append(".bytecode 49.0\n");
        text.append(".class public ").append(Jvm.PROGRAM_CLASS).append('\n');
        text.append(".super java/lang/Object\n\n");
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable) {
                text.append(".field public static ").append(jvm.globalName(variable.name))
                        .append(' ').append(jvm.descriptor(globalType(variable))).append('\n');
            }
        }
        text.append('\n').append(staticInitializer(program));
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof RoutineDeclaration routine && routine.body != null) {
                text.append('\n').append(routineMethod(routine));
            }
        }
        return text.toString();
    }

    private String staticInitializer(Program program) {
        Code code = new Code();
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable) {
                SemanticType type = globalType(variable);
                emitDefault(code, type);
                code.emit("putstatic " + Jvm.PROGRAM_CLASS + "/" + jvm.globalName(variable.name)
                        + " " + jvm.descriptor(type));
            }
        }
        code.emit("return");
        return method(".method static <clinit>()V", code);
    }

    private String routineMethod(RoutineDeclaration routine) {
        RoutineSymbol symbol = model.routineOf(routine);
        Code code = new Code();
        for (Parameter parameter : routine.parameters) {
            code.declare(model.variableOf(parameter));
        }
        emitBody(routine, symbol.returnType(), code);
        String header = ".method public static " + jvm.methodName(routine.name)
                + jvm.methodDescriptor(symbol.parameterTypes(), symbol.returnType());
        return method(header, code);
    }

    /** Fills the method body: for now only the default return value. */
    private void emitBody(RoutineDeclaration routine, SemanticType returnType, Code code) {
        if (returnType != VoidType.INSTANCE) {
            emitDefault(code, returnType);
        }
        code.emit(returnType == VoidType.INSTANCE ? "return" : jvm.prefix(returnType) + "return");
    }

    private String recordClass(RecordType record) {
        String name = jvm.recordClass(record);
        Code code = new Code();
        code.newLocal(1);
        code.emit("aload 0");
        code.emit("invokespecial java/lang/Object/<init>()V");
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, SemanticType> field : record.fields().entrySet()) {
            String fieldName = jvm.fieldName(field.getKey());
            String descriptor = jvm.descriptor(field.getValue());
            fields.append(".field public ").append(fieldName).append(' ').append(descriptor).append('\n');
            code.emit("aload 0");
            emitDefault(code, field.getValue());
            code.emit("putfield " + name + "/" + fieldName + " " + descriptor);
        }
        code.emit("return");
        StringBuilder text = new StringBuilder();
        text.append(".bytecode 49.0\n");
        text.append(".class public ").append(name).append('\n');
        text.append(".super java/lang/Object\n\n");
        text.append(fields).append('\n');
        text.append(method(".method public <init>()V", code));
        return text.toString();
    }

    /** Pushes the default value of a type: zero, empty string, new record or filled array. */
    private void emitDefault(Code code, SemanticType type) {
        if (type == PrimitiveType.REAL) {
            code.emit("dconst_0");
        } else if (type instanceof PrimitiveType) {
            code.emit("iconst_0");
        } else if (type == StringType.INSTANCE) {
            code.emit("ldc \"\"");
        } else if (type instanceof RecordType record) {
            String name = jvm.recordClass(record);
            code.emit("new " + name);
            code.emit("dup");
            code.emit("invokespecial " + name + "/<init>()V");
        } else if (type instanceof ArrayType array) {
            emitDefaultArray(code, array);
        } else {
            throw new IllegalArgumentException("no default value for: " + type);
        }
    }

    private void emitDefaultArray(Code code, ArrayType array) {
        SemanticType element = array.elementType();
        int size = (int) array.size();
        code.emit("ldc " + size);
        if (element instanceof PrimitiveType) {
            code.emit("newarray " + (element == PrimitiveType.REAL ? "double" : "int"));
            return;
        }
        code.emit("anewarray " + jvm.className(element));
        int index = code.newLocal(1);
        String start = code.newLabel();
        String end = code.newLabel();
        code.emit("iconst_0");
        code.emit("istore " + index);
        code.place(start);
        code.emit("iload " + index);
        code.emit("ldc " + size);
        code.emit("if_icmpge " + end);
        code.emit("dup");
        code.emit("iload " + index);
        emitDefault(code, element);
        code.emit("aastore");
        code.emit("iinc " + index + " 1");
        code.emit("goto " + start);
        code.place(end);
    }

    private SemanticType globalType(VariableDeclaration variable) {
        return model.variableOf(variable).type();
    }

    private String method(String header, Code code) {
        return header + "\n"
                + ".limit stack " + MAX_STACK + "\n"
                + ".limit locals " + code.maxLocals() + "\n"
                + code.text()
                + ".end method\n";
    }
}
