package ast;

import java.util.List;

public abstract class Expression {

  // Number literal
  public static class NumberLiteral extends Expression {

    private final int value;

    public NumberLiteral(int value) {
      this.value = value;
    }

    public int value() {
      return value;
    }

    @Override
    public String toString() {
      return "NumberLiteral(" + value + ")";
    }
  }

  // Variable reference
  public static class Variable extends Expression {

    private final String name;

    public Variable(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    @Override
    public String toString() {
      return "Variable(" + name + ")";
    }
  }

  // Binary addition
  public static class BinaryAdd extends Expression {

    private final Expression left;
    private final Expression right;

    public BinaryAdd(Expression left, Expression right) {

      this.left = left;
      this.right = right;
    }

    public Expression left() {
      return left;
    }

    public Expression right() {
      return right;
    }

    @Override
    public String toString() {
      return "BinaryAdd(" + left + ", " + right + ")";
    }
  }

  // Function call
  //
  // add(10, 20)
  // foo(a, b)

  public static class FunctionCall extends Expression {

    private final String functionName;
    private final List<Expression> arguments;

    public FunctionCall(String functionName, List<Expression> arguments) {

      this.functionName = functionName;
      this.arguments = List.copyOf(arguments);
    }

    public String functionName() {
      return functionName;
    }

    public List<Expression> arguments() {
      return arguments;
    }

    @Override
    public String toString() {
      return "FunctionCall(" + functionName + ", " + arguments + ")";
    }
  }
}
