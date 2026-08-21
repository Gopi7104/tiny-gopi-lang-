import ast.Expression;
import ast.Function;
import ast.Program;
import java.util.HashMap; // function map - symbol table 
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SemanticAnalyzer {

  private final Map<String, Function> functions = new HashMap<>();

  public void analyze(Program program) {

    functions.put(
        "show",
        new Function(
            "show",
            java.util.List.of(new Function.Parameter("value")),
            new Expression.NumberLiteral(0)));
            
    // Pass 1: Register all functions

    for (Function function : program.functions()) {

      if (functions.containsKey(function.name())) {
        if (function.name().equals("main")) {
          throw new RuntimeException("Semantic error: duplicate 'main' function");
        }
        throw new RuntimeException("Semantic error: duplicate function '" + function.name() + "'");
      }

      functions.put(function.name(), function);
    }

    // Pass 2: Enforce main function **rules

    Function mainFunc = functions.get("main");
    if (mainFunc == null) {
      throw new RuntimeException("Semantic error: missing 'main' function");
    }
    if (!mainFunc.parameters().isEmpty()) {
      throw new RuntimeException("Semantic error: 'main' must not have parameters");
    }

    // Pass 3: Analyze every function

    for (Function function : program.functions()) {
      analyzeFunction(function);
    }
  }

  private void analyzeFunction(Function function) {

    Set<String> parameters = new HashSet<>();

    for (Function.Parameter parameter : function.parameters()) {

      if (!parameters.add(parameter.name())) {
        throw new RuntimeException(
            "Semantic error in function '"
                + function.name()
                + "': duplicate parameter '"
                + parameter.name()
                + "'");
      }
    }

    analyzeExpression(function.returnExpression(), parameters, function.name());
  }

  private void analyzeExpression(
      Expression expression, Set<String> parameters, String functionName) {

    if (expression instanceof Expression.NumberLiteral) {
      return;
    }

    if (expression instanceof Expression.Variable variable) {

      if (!parameters.contains(variable.name())) {

        throw new RuntimeException(
            "Semantic error in function '"
                + functionName
                + "': unknown variable '"
                + variable.name()
                + "'");
      }

      return;
    }

    if (expression instanceof Expression.BinaryAdd binaryAdd) {

      analyzeExpression(binaryAdd.left(), parameters, functionName);

      analyzeExpression(binaryAdd.right(), parameters, functionName);

      return;
    }

    if (expression instanceof Expression.FunctionCall functionCall) {

      Function target = functions.get(functionCall.functionName());

      // Function must exist

      if (target == null) {

        throw new RuntimeException(
            "Semantic error in function '"
                + functionName
                + "': unknown function '"
                + functionCall.functionName()
                + "'");
      }

      // Check argument count

      if (functionCall.arguments().size() != target.parameters().size()) {

        throw new RuntimeException(
            "Semantic error in function '"
                + functionName
                + "': function '"
                + functionCall.functionName()
                + "' expects "
                + target.parameters().size()
                + " arguments but got "
                + functionCall.arguments().size());
      }

      // Analyze every argument

      for (Expression argument : functionCall.arguments()) {
        analyzeExpression(argument, parameters, functionName);
      }
      return;
    }
    throw new RuntimeException(
        "Semantic error: unsupported expression " + expression.getClass().getSimpleName());
  }
}
