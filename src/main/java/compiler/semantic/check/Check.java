package compiler.semantic.check;

import compiler.ast.Program;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;

public interface Check {

    void run(Program program, SemanticModel model, Diagnostics diagnostics);
}
