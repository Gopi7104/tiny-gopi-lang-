package ast;

import java.util.List;

public class Program {

  private final List<Function> functions;

  public Program(List<Function> functions) {
    this.functions = functions;
  }

  public List<Function> functions() {
    return functions;
  }

  @Override
  public String toString() {
    return "Program{" + "functions=" + functions + '}';
  }
}
