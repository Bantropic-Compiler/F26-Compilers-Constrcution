package compiler.ast;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Base class for every AST node. Holds the position of the token the
 * node started at.
 */
public abstract class Node {

    public final int line;
    public final int column;

    protected Node(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /** Stable, indented representation of the AST structure for diagnostics and tests. */
    public final String describe() {
        StringBuilder out = new StringBuilder();
        appendNode(out, this, 0);
        return out.toString();
    }

    private static void appendNode(StringBuilder out, Node node, int depth) {
        indent(out, depth).append(node.getClass().getSimpleName()).append('\n');
        Field[] fields = node.getClass().getDeclaredFields();
        Arrays.sort(fields, Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!Modifier.isPublic(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                Object value = field.get(node);
                indent(out, depth + 1).append(field.getName()).append(':');
                if (value instanceof Node child) {
                    out.append('\n');
                    appendNode(out, child, depth + 2);
                } else if (value instanceof Iterable<?> elements) {
                    boolean empty = true;
                    for (Object element : elements) {
                        if (empty) out.append('\n');
                        empty = false;
                        if (element instanceof Node child) {
                            appendNode(out, child, depth + 2);
                        } else {
                            indent(out, depth + 2).append(formatScalar(element)).append('\n');
                        }
                    }
                    if (empty) out.append(" []\n");
                } else {
                    out.append(' ').append(formatScalar(value)).append('\n');
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot describe " + node.getClass().getName(), exception);
            }
        }
    }

    private static StringBuilder indent(StringBuilder out, int depth) {
        return out.append("  ".repeat(depth));
    }

    private static String formatScalar(Object value) {
        if (value instanceof String text) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        }
        return String.valueOf(value);
    }
}
