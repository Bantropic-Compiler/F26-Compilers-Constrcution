package compiler.semantic.visitor;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.semantic.SemanticTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AstCopierTest {

    private static final String RICH = """
            type Item is record
                var price: integer
                var tags: array[2] string
            end
            var counter: integer is 0
            routine fwd(a: array[] real): boolean
            routine work(n: integer, items: array[3] Item): integer is
                var total is 0
                for i in 1..n reverse loop
                    total := total + items[i].price * -2
                end
                for it in items loop
                    if it.price >= 10.5 and not 1 then
                        print "big", 'c', true
                        continue
                    else
                        break
                    end
                end
                while total /= 0 loop
                    total := total - 1
                end
                fwd(total)
                return total
            end
            """;

    static List<Path> examples() throws Exception {
        try (Stream<Path> paths = Files.list(Path.of("prog-examples"))) {
            return paths.filter(path -> path.toString().endsWith(".i")).sorted().toList();
        }
    }

    private static final class Collector extends AstVisitor {
        final List<Node> nodes = new ArrayList<>();

        @Override
        public void visit(Node node) {
            if (node != null) {
                nodes.add(node);
            }
            super.visit(node);
        }
    }

    private static List<Node> nodesOf(Node root) {
        Collector collector = new Collector();
        collector.visit(root);
        return collector.nodes;
    }

    private static void assertDeepCopy(Program original) {
        Program copy = AstCopier.copy(original);

        assertNotSame(original, copy);
        assertEquals(original.describe(), copy.describe());

        List<Node> originalNodes = nodesOf(original);
        List<Node> copyNodes = nodesOf(copy);
        assertEquals(originalNodes.size(), copyNodes.size());
        Set<Node> originals = Collections.newSetFromMap(new IdentityHashMap<>());
        originals.addAll(originalNodes);
        for (int i = 0; i < copyNodes.size(); i++) {
            Node left = originalNodes.get(i);
            Node right = copyNodes.get(i);
            assertFalse(originals.contains(right), "shared node: " + right.getClass().getSimpleName());
            assertSame(left.getClass(), right.getClass());
            assertEquals(left.line, right.line);
            assertEquals(left.column, right.column);
        }
    }

    @Test
    void copiesARichProgramDeeplyWithPositions() {
        assertDeepCopy(SemanticTestSupport.parse(RICH));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void copiesEveryExampleProgram(Path source) throws Exception {
        assertDeepCopy(SemanticTestSupport.parse(Files.readString(source)));
    }

    @Test
    void mutatingTheCopyLeavesTheOriginalUntouched() {
        Program original = SemanticTestSupport.parse("var x is 1 + 2\nroutine f() is\n    print 1\nend\n");
        Program copy = AstCopier.copy(original);

        BinaryExpression sum = (BinaryExpression) ((VariableDeclaration) copy.declarations.get(0)).initializer;
        sum.left = null;
        ((VariableDeclaration) copy.declarations.get(0)).name = "renamed";
        ((RoutineDeclaration) copy.declarations.get(1)).body.items.clear();
        copy.declarations.remove(0);

        assertEquals(2, original.declarations.size());
        assertEquals("x", ((VariableDeclaration) original.declarations.get(0)).name);
        assertEquals(1, ((RoutineDeclaration) original.declarations.get(1)).body.items.size());
        assertNotNull(((BinaryExpression) ((VariableDeclaration) original.declarations.get(0)).initializer).left);
    }

    @Test
    void copyKeepsTheStaticTypeOfTheArgument() {
        Expression expression = SemanticTestSupport.expression("a + 1");

        BinaryExpression copy = AstCopier.copy((BinaryExpression) expression);

        assertNotSame(expression, copy);
        assertEquals(expression.describe(), copy.describe());
    }

    @Test
    void copyOfNullIsNull() {
        assertNull(AstCopier.copy((Node) null));
    }

    @Test
    void copiedListsAreIndependentAndMutable() {
        Program original = SemanticTestSupport.parse("routine f() is\n    print 1, 2\nend\n");
        Program copy = AstCopier.copy(original);
        compiler.ast.statement.PrintStatement print = (compiler.ast.statement.PrintStatement)
                ((RoutineDeclaration) copy.declarations.get(0)).body.items.get(0);

        print.args.add(SemanticTestSupport.expression("3"));

        compiler.ast.statement.PrintStatement originalPrint = (compiler.ast.statement.PrintStatement)
                ((RoutineDeclaration) original.declarations.get(0)).body.items.get(0);
        assertEquals(2, originalPrint.args.size());
        assertEquals(3, print.args.size());
    }

    @Test
    void unknownNodeKindsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> AstCopier.copy(new Node(1, 1) {
        }));
    }
}
