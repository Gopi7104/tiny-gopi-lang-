package ast;

import java.util.List;

public class Function {

  private final String name;
  private final List<Parameter> parameters;
  private final Expression returnExpression;

  public Function(String name, List<Parameter> parameters, Expression returnExpression) {

    this.name = name;
    this.parameters = parameters;
    this.returnExpression = returnExpression;
  }

  public String name() {
    return name;
  }

  public List<Parameter> parameters() {
    return parameters;
  }

  public Expression returnExpression() {
    return returnExpression;
  }

  @Override
  public String toString() {
    return "Function{"
        + "name='"
        + name
        + '\''
        + ", parameters="
        + parameters
        + ", returnExpression="
        + returnExpression
        + '}';
  }

  // Function parameter

  public static class Parameter {

    private final String name;

    public Parameter(String name) {
      this.name = name;
    }

    public String name() {
      return name;
    }

    @Override
    public String toString() {
      return "Parameter{" + "name='" + name + '\'' + '}';
    }
  }
}
