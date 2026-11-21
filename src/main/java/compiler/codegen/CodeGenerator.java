package compiler.codegen;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.Declaration;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Accessor;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.expression.literal.CharLiteral;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.ast.expression.literal.StringLiteral;
import compiler.ast.statement.AssignmentStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.lexer.TokenType;
import compiler.semantic.symbol.VariableSymbol;
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
                emitInitialValue(code, variable, type);
                // store the initializer or default value in the global field
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

    private void emitBody(RoutineDeclaration routine, SemanticType returnType, Code code) {
        // emitBlock() is true when it reaches `return` statement
        // for void functions it returns false and then we add some defualt return
        if (emitBlock(code, routine.body, returnType)) {
            return;
        }

        // TODO: Add if/else generation and delete with branch
        if (returnType != VoidType.INSTANCE) {
            emitDefault(code, returnType);
        }

        // Return for void
        emitReturnInstruction(code, returnType);
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

    void emitExpression(Code code, Expression expression) {
        if (expression instanceof IntegerLiteral n) {
            // push the 32-bit integer literal
            code.emit("ldc " + (int) n.value);
        } else if (expression instanceof RealLiteral n) {
            // push the double literal
            code.emit("ldc2_w " + n.value);
        } else if (expression instanceof BooleanLiteral b) {
            // push 1 for true or 0 for false
            code.emit(b.value ? "iconst_1" : "iconst_0");
        } else if (expression instanceof CharLiteral c) {
            // push the Unicode code point as an integer
            code.emit("ldc " + c.codePoint);
        } else if (expression instanceof StringLiteral s) {
            // push the string literal
            code.emit("ldc " + jvm.quote(s.value));
        } else if (expression instanceof ModifiablePrimaryNode variable) {
            emitRead(code, variable, variable.accessors.size());
        } else if (expression instanceof UnaryExpression unary) {
            emitUnary(code, unary);
        } else if (expression instanceof BinaryExpression binary) {
            emitBinary(code, binary);
        } else if (expression instanceof RoutineCallExpression call) {
            emitCall(code, call);
        } else {
            throw new UnsupportedOperationException("Unsupported expression: " + expression.getClass().getSimpleName());
        }
    }

    private SemanticType emitRead(Code code, ModifiablePrimaryNode variable, int accessorCount) {
        VariableSymbol symbol = model.variableOf(variable);
        SemanticType type = symbol.type();
        if (symbol.kind() == VariableSymbol.Kind.GLOBAL) {
            // load the global variable from its static field
            code.emit("getstatic " + Jvm.PROGRAM_CLASS + "/" + jvm.globalName(symbol.name())
                    + " " + jvm.descriptor(type));
        } else {
            // load the local variable or parameter from its slot
            code.emit(jvm.prefix(type) + "load " + code.slotOf(symbol));
        }
        for (int i = 0; i < accessorCount; i++) {
            Accessor accessor = variable.accessors.get(i);
            if (accessor instanceof IndexAccess index) {
                emitIndex(code, index);
                if (type instanceof ArrayType array) {
                    type = array.elementType();
                    // load the array element using the opcode for its type
                    code.emit(jvm.prefix(type) + "aload");
                } else {
                    // load the UTF-16 code unit at the given index
                    code.emit("invokevirtual java/lang/String/charAt(I)C");
                    type = PrimitiveType.CHAR;
                }
            } else if (accessor instanceof FieldAccess field) {
                if (type instanceof RecordType record) {
                    type = record.fieldType(field.name);
                    // read the field from the record on the stack
                    code.emit("getfield " + jvm.recordClass(record) + "/" + jvm.fieldName(field.name)
                            + " " + jvm.descriptor(type));
                } else {
                    // get the array length or the number of UTF-16 code units in the string
                    code.emit(type instanceof ArrayType ? "arraylength" : "invokevirtual java/lang/String/length()I");
                    type = PrimitiveType.INTEGER;
                }
            }
        }
        return type;
    }

    private void emitIndex(Code code, IndexAccess index) {
        emitExpression(code, index.index);
        code.emit("iconst_1");
        code.emit("isub");
    }

    private void emitAssignment(Code code, AssignmentStatement assignment) {
        ModifiablePrimaryNode target = assignment.target;
        SemanticType type = model.typeOf(target);
        String store;
        if (target.accessors.isEmpty()) {
            VariableSymbol symbol = model.variableOf(target);
            if (symbol.kind() == VariableSymbol.Kind.GLOBAL) {
                store = "putstatic " + Jvm.PROGRAM_CLASS + "/" + jvm.globalName(symbol.name())
                        + " " + jvm.descriptor(type);
            } else {
                store = jvm.prefix(type) + "store " + code.slotOf(symbol);
            }
        } else {
            int last = target.accessors.size() - 1;
            SemanticType owner = emitRead(code, target, last);
            Accessor accessor = target.accessors.get(last);
            if (accessor instanceof FieldAccess field && owner instanceof RecordType record) {
                store = "putfield " + jvm.recordClass(record) + "/" + jvm.fieldName(field.name)
                        + " " + jvm.descriptor(type);
            } else if (accessor instanceof IndexAccess index && owner instanceof ArrayType) {
                emitIndex(code, index);
                store = jvm.prefix(type) + "astore";
            } else {
                throw new IllegalArgumentException("invalid assignment target");
            }
        }
        emitConvertedExpression(code, assignment.value, type);
        code.emit(store);
    }

    private void emitUnary(Code code, UnaryExpression unary) {
        emitExpression(code, unary.operand);
        switch (unary.op) {
            case PLUS -> { /* Unary plus leaves the operand unchanged. */ }
            case MINUS -> {
                // negate the integer or double operand
                code.emit(jvm.prefix(model.typeOf(unary.operand)) + "neg");
            }
            case NOT -> emitNot(code);
            default -> throw new IllegalArgumentException("not a unary operator: " + unary.op);
        }
    }

    private void emitNot(Code code) {
        // push the bit used to invert a normalized boolean
        code.emit("iconst_1");
        // flip 0 to 1 and 1 to 0
        code.emit("ixor");
    }

    private void emitBinary(Code code, BinaryExpression binary) {
        if (model.typeOf(binary) == StringType.INSTANCE) {
            emitConcatenation(code, binary);
            return;
        }
        boolean real = model.typeOf(binary.left) == PrimitiveType.REAL
                || model.typeOf(binary.right) == PrimitiveType.REAL;
        emitOperand(code, binary.left, real);
        emitOperand(code, binary.right, real);
        switch (binary.op) {
            case PLUS, MINUS, STAR, SLASH, PERCENT -> {
                String operation = switch (binary.op) {
                    case PLUS -> "add";
                    case MINUS -> "sub";
                    case STAR -> "mul";
                    case SLASH -> "div";
                    default -> "rem";
                };
                // consume both numeric operands and push the arithmetic result
                code.emit((real ? "d" : "i") + operation);
            }
            case AND, OR, XOR -> {
                String operation = switch (binary.op) {
                    case AND -> "iand";
                    case OR -> "ior";
                    default -> "ixor";
                };
                // combine both evaluated boolean operands without short-circuiting
                code.emit(operation);
            }
            case EQ, NEQ, LT, LE, GT, GE -> {
                if (model.typeOf(binary.left) == StringType.INSTANCE) {
                    // Semantic analyzer already check 
                    // that string type allows only = and /= binary expressions

                    // compare string contents and push a normalized boolean
                    code.emit("invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z");
                    if (binary.op == TokenType.NEQ) {
                        emitNot(code);
                    }
                } else {
                    emitComparison(code, binary.op, real);
                }
            }
            default -> throw new IllegalArgumentException("not a binary operator: " + binary.op);
        }
    }

    private void emitOperand(Code code, Expression operand, boolean real) {
        emitExpression(code, operand);
        if (real) {
            // widen this operand before evaluating the next one, preserving stack order
            emitConversion(model.typeOf(operand), PrimitiveType.REAL, code);
        }
    }

    /**
     * Converts the value on top of the stack; semantic analysis must have approved the conversion.
     * Boolean/integer conversions and compatible reference types need no JVM instructions.
     * In particular, integer-to-boolean is already restricted to constants 0 and 1.
     */
    private void emitConversion(SemanticType sourceType, SemanticType targetType, Code code) {
        if (targetType == PrimitiveType.REAL
                && (sourceType == PrimitiveType.INTEGER || sourceType == PrimitiveType.BOOLEAN)) {
            code.emit("i2d");
        } else if (targetType == PrimitiveType.INTEGER && sourceType == PrimitiveType.REAL) {
            // round to the nearest integer, choosing the even integer for ties
            code.emit("invokestatic java/lang/Math/rint(D)D");
            code.emit("d2i");
        }
    }

    private void emitComparison(Code code, TokenType operator, boolean real) {
        String condition = switch (operator) {
            case EQ -> "eq";
            case NEQ -> "ne";
            case LT -> "lt";
            case LE -> "le";
            case GT -> "gt";
            case GE -> "ge";
            default -> throw new IllegalArgumentException("not a comparison: " + operator);
        };
        // Оба операнда уже вычислены: стек [..., левый, правый], правый на вершине.
        // Нужно заменить их одним boolean: 0 или 1.
        if (real) {
            // JVM не умеет одним условным переходом сравнивать два double.
            // dcmpg/dcmpl снимают оба числа и кладут int: 
            // -1, если левое < правого,
            // 0, если равны, 
            // 1, если левое > правого. 
            // Стек станет [..., результат].
            // Разница только при NaN в любом операнде: dcmpg даёт 1, dcmpl даёт -1.
            // Для < и <= выбираем 1: последующая проверка результата < 0 или <= 0 ложна.
            // Для > и >= выбираем -1: проверка > 0 или >= 0 тоже ложна.
            // Поэтому сравнения порядка с NaN всегда ложны. Для = и /= подходят оба
            // варианта: ненулевой результат означает, что равенство ложно, а неравенство истинно.
            code.emit(operator == TokenType.LT || operator == TokenType.LE ? "dcmpg" : "dcmpl");
        }
        // Это имена меток в генерируемом коде.
        // yes — ветка истины; end — общая точка продолжения с готовым boolean на стеке.
        String yes = code.newLabel();
        String end = code.newLabel();
        // Для integer if_icmplt снимает два числа и переходит к yes, если левое < правого.
        // Для real инструкция iflt снимает один int от dcmpg/dcmpl и переходит, если он < 0.
        // Остальные condition аналогично задают =, /=, <=, > или >=.
        // Сам переход НЕ кладёт boolean на стек: после проверки остаётся [...].
        code.emit((real ? "if" : "if_icmp") + condition + " " + yes);
        // Если условие ложно, перехода нет: выполняем следующую инструкцию и кладём 0.
        code.emit("iconst_0");
        // Пропускаем ветку истины. 
        code.emit("goto " + end);
        // Сюда условный переход приводит только при истинном сравнении.
        code.place(yes);
        // Кладём 1 вместо пропущенного iconst_0.
        code.emit("iconst_1");
        // Обе ветки сходятся со стеком [..., boolean]: добавлено ровно одно значение, 0 или 1.
        code.place(end);
    }

    private void emitConcatenation(Code code, BinaryExpression binary) {
        emitStringOperand(code, binary.left);
        emitStringOperand(code, binary.right);
        // concatenate the right string onto the left string
        code.emit("invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;");
    }

    private void emitStringOperand(Code code, Expression operand) {
        emitExpression(code, operand);
        if (model.typeOf(operand) == PrimitiveType.CHAR) {
            // convert the entire Unicode code point to a string
            code.emit("invokestatic java/lang/Character/toString(I)Ljava/lang/String;");
        }
    }

    private void emitCall(Code code, RoutineCallExpression call) {
        RoutineSymbol routine = model.routineOf(call);
        for (int i = 0; i < call.args.size(); i++) {
            Expression argument = call.args.get(i);
            emitConvertedExpression(code, argument, routine.parameterTypes().get(i));
        }
        code.emit("invokestatic " + Jvm.PROGRAM_CLASS + "/" + jvm.methodName(routine.name())
                + jvm.methodDescriptor(routine.parameterTypes(), routine.returnType()));
    }

    private boolean emitBlock(Code code, BlockNode block, SemanticType returnType) {
        for (Node item : block.items) {
            if (item instanceof VariableDeclaration variable) {
                VariableSymbol symbol = model.variableOf(variable);
                int slot = code.declare(symbol);
                emitInitialValue(code, variable, symbol.type());
                code.emit(jvm.prefix(symbol.type()) + "store " + slot);
            } else if (item instanceof AssignmentStatement assignment) {
                emitAssignment(code, assignment);
            } else if (item instanceof ReturnStatement statement) {
                if (statement.value != null) {
                    emitConvertedExpression(code, statement.value, returnType);
                }
                emitReturnInstruction(code, returnType);
                return true;
            }
        }
        return false;
    }

    private void emitReturnInstruction(Code code, SemanticType type) {
        code.emit(type == VoidType.INSTANCE ? "return" : jvm.prefix(type) + "return");
    }

    private void emitConvertedExpression(Code code, Expression value, SemanticType type) {
        emitExpression(code, value);
        emitConversion(model.typeOf(value), type, code);
    }

    private void emitInitialValue(Code code, VariableDeclaration variable, SemanticType type) {
        if (variable.initializer == null) {
            emitDefault(code, type);
        } else {
            emitConvertedExpression(code, variable.initializer, type);
        }
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
