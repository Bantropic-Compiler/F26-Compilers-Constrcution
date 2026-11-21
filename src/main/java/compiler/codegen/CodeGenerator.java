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
        text.append(".bytecode 49.0\n"); // class file version 49: the old verifier, no stack map frames needed
        text.append(".class public ").append(Jvm.PROGRAM_CLASS).append('\n'); // class declaration
        text.append(".super java/lang/Object\n\n"); // every class extends Object
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

                // Program.global = default value (pops the value)
                code.emit("putstatic " + Jvm.PROGRAM_CLASS + "/" + jvm.globalName(variable.name)
                        + " " + jvm.descriptor(type));
            }
        }
        code.emit("return"); // end of the static initializer
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
        // return the default value: return for void, ireturn, dreturn or areturn otherwise
        code.emit(returnType == VoidType.INSTANCE ? "return" : jvm.prefix(returnType) + "return");
    }

    private String recordClass(RecordType record) {
        String name = jvm.recordClass(record);
        Code code = new Code();
        code.newLocal(1);
        code.emit("aload 0"); // push this
        // call the constructor of Object, required in every constructor
        code.emit("invokespecial java/lang/Object/<init>()V");
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, SemanticType> field : record.fields().entrySet()) {
            String fieldName = jvm.fieldName(field.getKey());
            String descriptor = jvm.descriptor(field.getValue());
            fields.append(".field public ").append(fieldName).append(' ').append(descriptor).append('\n');
            code.emit("aload 0"); // push this, the object whose field is assigned
            emitDefault(code, field.getValue());
            code.emit("putfield " + name + "/" + fieldName + " " + descriptor); // this.field = default value (pops this and the value)
        }
        code.emit("return"); // end of the constructor
        StringBuilder text = new StringBuilder();
        text.append(".bytecode 49.0\n"); // class file version 49: the old verifier, no stack map frames needed
        text.append(".class public ").append(name).append('\n'); // class declaration
        text.append(".super java/lang/Object\n\n"); // every class extends Object
        text.append(fields).append('\n');
        text.append(method(".method public <init>()V", code));
        return text.toString();
    }

    /** Pushes the default value of a type: zero, empty string, new record or filled array. */
    private void emitDefault(Code code, SemanticType type) {
        if (type == PrimitiveType.REAL) {
            code.emit("dconst_0"); // push 0.0
        } else if (type instanceof PrimitiveType) {
            code.emit("iconst_0"); // push 0, also false and char 0
        } else if (type == StringType.INSTANCE) {
            code.emit("ldc \"\""); // push the empty string
        } else if (type instanceof RecordType record) {
            String name = jvm.recordClass(record);
            // create an uninitialized record object
            code.emit("new " + name);
            // duplicate the reference, one copy is consumed by the constructor call
            code.emit("dup");
            // call the record constructor, which fills all fields with defaults
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
        code.emit("ldc " + size); // push the array size
        if (element instanceof PrimitiveType) {
            // numeric array is created already filled with zeros by the JVM
            code.emit("newarray " + (element == PrimitiveType.REAL ? "double" : "int"));
            return;
        }
        // array of references (strings, records, arrays), all elements are null for now
        code.emit("anewarray " + jvm.className(element));
        int index = code.newLocal(1);
        String start = code.newLabel();
        String end = code.newLabel();
        code.emit("iconst_0"); // i = 0
        code.emit("istore " + index); // store i into a local slot
        code.place(start);
        code.emit("iload " + index); // loop start: if i >= size jump to the end
        code.emit("ldc " + size);
        code.emit("if_icmpge " + end);
        code.emit("dup"); // duplicate the array reference, so one copy stays on the stack after aastore
        code.emit("iload " + index); // push index i
        emitDefault(code, element);
        code.emit("aastore"); // array[i] = default value (pops array, index and value)
        code.emit("iinc " + index + " 1"); // i = i + 1
        code.emit("goto " + start); // jump back to the loop start
        code.place(end);
    }

    private SemanticType globalType(VariableDeclaration variable) {
        return model.variableOf(variable).type();
    }

    private String method(String header, Code code) {
        return header + "\n"
                + ".limit stack " + MAX_STACK + "\n" // stack limit of the method, a fixed value
                + ".limit locals " + code.maxLocals() + "\n" // number of local variable slots: this or parameters plus temporaries
                + code.text()
                + ".end method\n"; // end of the method
    }
}
