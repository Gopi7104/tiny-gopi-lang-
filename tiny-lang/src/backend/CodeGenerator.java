package backend;

import ast.Expression;
import ast.Function;
import ast.Program;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {

  /*
   * Final program machine code.
   */
  private final MachineCodeBuffer code = new MachineCodeBuffer();

  /*
   * Function name -> function starting byte offset.
   *
   * Example:
   *
   * add  -> 0
   * main -> 8
   */
  private final Map<String, Integer> functionOffsets = new HashMap<>();

  /*
   * Function name -> generated machine code for that function.
   */
  private final Map<String, MachineCodeBuffer> functionCode = new HashMap<>();

  /*
   * Function name -> list of unresolved CALL sites.
   */
  private final Map<String, List<CallSite>> callSites = new HashMap<>();

  /*
   * Parameter name -> ARM64 argument register.
   *
   * a -> W0
   * b -> W1
   * c -> W2
   */
  private final Map<String, Integer> parameterRegisters = new HashMap<>();

  // ============================================================
  // Call site
  // ============================================================

  private static class CallSite {

    private final String caller;
    private final String target;
    private final int byteOffset;

    private CallSite(String caller, String target, int byteOffset) {

      this.caller = caller;
      this.target = target;
      this.byteOffset = byteOffset;
    }
  }

  public MachineCodeBuffer generate(Program program) {

    functionOffsets.clear();
    functionCode.clear();
    callSites.clear();
    code.clear();

    for (Function function : program.functions()) {

      if (functionCode.containsKey(function.name())) {

        throw new RuntimeException(
            "Code generation error: duplicate function '" + function.name() + "'");
      }

      functionCode.put(function.name(), new MachineCodeBuffer());
    }

    MachineCodeBuffer startBuffer = new MachineCodeBuffer();
    functionCode.put("_start", startBuffer);

    int callByteOffset = startBuffer.size();
    startBuffer.emit(Arm64.bl(0));
    callSites
        .computeIfAbsent("_start", ignored -> new java.util.ArrayList<>())
        .add(new CallSite("_start", "main", callByteOffset));

    startBuffer.emit(Arm64.movWImmediate(16, 1));
    startBuffer.emit(Arm64.svc(0x80));

    MachineCodeBuffer showBuffer = new MachineCodeBuffer();
    functionCode.put("show", showBuffer);
    generateShowFunction(showBuffer);

    for (Function function : program.functions()) {
      generateFunction(function);
    }

    int offset = 0;

    functionOffsets.put("_start", offset);
    offset += functionCode.get("_start").size();

    functionOffsets.put("show", offset);
    offset += functionCode.get("show").size();

    for (Function function : program.functions()) {
      functionOffsets.put(function.name(), offset);
      offset += functionCode.get(function.name()).size();
    }

    resolveCalls();

    appendFunction(functionCode.get("_start"));
    appendFunction(functionCode.get("show"));

    for (Function function : program.functions()) {
      appendFunction(functionCode.get(function.name()));
    }

    return code;
  }

  private void generateShowFunction(MachineCodeBuffer showBuffer) {
    int[] instructions = {
      0xd10043ff, 0x52800148, 0x39003fe8, 0x340001e0,
      0x528001c9, 0x529999aa, 0x72b9998a, 0x910003eb,
      0x9baa7c0c, 0xd363fd8c, 0x1b08818d, 0x321c05ad,
      0x3829696d, 0xd1000529, 0x7100241f, 0xaa0c03e0,
      0x54ffff08, 0x14000004, 0x52800608, 0x39003be8,
      0x528001a9, 0x910003e8, 0x8b29c108, 0x528001ea,
      0x4b090149, 0x93407d22, 0x91000501, 0x52800090,
      0x72a04010, 0x52800020, 0xd4001001, 0x910043ff,
      0x52800000, 0xd65f03c0
    };
    for (int instruction : instructions) {
      showBuffer.emit(instruction);
    }
  }

  private void generateFunction(Function function) {

    MachineCodeBuffer functionBuffer = functionCode.get(function.name());

    parameterRegisters.clear();

    functionBuffer.emit(Arm64.pushX(30)); 

    int numParams = function.parameters().size();
    if (numParams > 8) {
      throw new RuntimeException("Too many parameters (max 8 supported).");
    }

    for (int i = 0; i < numParams; i++) {
      functionBuffer.emit(Arm64.pushX(19 + i));
      functionBuffer.emit(Arm64.movW(19 + i, i));

      String parameterName = function.parameters().get(i).name();
      parameterRegisters.put(parameterName, 19 + i);
    }

    generateExpression(function.returnExpression(), function.name(), functionBuffer);

    for (int i = numParams - 1; i >= 0; i--) {
      functionBuffer.emit(Arm64.popX(19 + i));
    }

    functionBuffer.emit(Arm64.popX(30));
    functionBuffer.emit(Arm64.ret());
  }

  private void generateExpression(
      Expression expression, String currentFunction, MachineCodeBuffer functionBuffer) {

    if (expression instanceof Expression.NumberLiteral number) {

      generateNumberLiteral(number.value(), functionBuffer);

      return;
    }
    if (expression instanceof Expression.Variable variable) {

      generateVariable(variable.name(), functionBuffer);

      return;
    }

    if (expression instanceof Expression.BinaryAdd add) {

      generateAddition(add, currentFunction, functionBuffer);

      return;
    }

    if (expression instanceof Expression.FunctionCall call) {

      generateFunctionCall(call, currentFunction, functionBuffer);

      return;
    }

    throw new RuntimeException(
        "Code generation error: unsupported expression " + expression.getClass().getSimpleName());
  }

  // Number literal

  private void generateNumberLiteral(int value, MachineCodeBuffer functionBuffer) {

    generateConstant(0, value, functionBuffer);
  }

  private void generateConstant(int targetRegister, int value, MachineCodeBuffer functionBuffer) {

    int lower16 = value & 0xFFFF;
    int upper16 = (value >>> 16) & 0xFFFF;

    functionBuffer.emit(Arm64.movWImmediate(targetRegister, lower16));

    if (upper16 != 0) {
      functionBuffer.emit(Arm64.movkWImmediateLSL16(targetRegister, upper16));
    }
  }

  // Variable

  private void generateVariable(String name, MachineCodeBuffer functionBuffer) {

    Integer register = parameterRegisters.get(name);

    if (register == null) {

      throw new RuntimeException("Code generation error: unknown variable '" + name + "'");
    }

    /*
     * Expression results must be in W0.
     */
    if (register != 0) {

      functionBuffer.emit(Arm64.movW(0, register));
    }
  }

  // Addition

  private void generateAddition(
      Expression.BinaryAdd expression, String currentFunction, MachineCodeBuffer functionBuffer) {

    /*
     * 1. Evaluate left -> W0
     * 2. Push W0 to stack
     * 3. Evaluate right -> W0
     * 4. Pop left -> W1
     * 5. Add W0 = W1 + W0
     */
    generateExpression(expression.left(), currentFunction, functionBuffer);

    functionBuffer.emit(Arm64.pushX(0));

    generateExpression(expression.right(), currentFunction, functionBuffer);

    functionBuffer.emit(Arm64.popX(1));

    functionBuffer.emit(Arm64.addW(0, 1, 0));
  }

  private void generateFunctionCall(
      Expression.FunctionCall call, String currentFunction, MachineCodeBuffer functionBuffer) {

    String functionName = call.functionName();

    if (!functionCode.containsKey(functionName)) {
      throw new RuntimeException("Code generation error: unknown function '" + functionName + "'");
    }

    List<Expression> arguments = call.arguments();

    if (arguments.size() > 8) {
      throw new RuntimeException(
          "Code generation error: function '"
              + functionName
              + "' has "
              + arguments.size()
              + " arguments. Maximum is 8.");
    }

    /*
     * Evaluate all arguments and push them to the stack.
     * This ensures that earlier arguments (evaluated into W0) are not clobbered
     * by subsequent arguments that might also be function calls.
     */
    for (int i = 0; i < arguments.size(); i++) {
      generateExpression(arguments.get(i), currentFunction, functionBuffer);
      functionBuffer.emit(Arm64.pushX(0));
    }

    /*
     * Pop the arguments into W_i in reverse order.
     */
    for (int i = arguments.size() - 1; i >= 0; i--) {
      functionBuffer.emit(Arm64.popX(i));
    }

    int callByteOffset = functionBuffer.size();

    functionBuffer.emit(Arm64.bl(0));

    callSites
        .computeIfAbsent(currentFunction, ignored -> new java.util.ArrayList<>())
        .add(new CallSite(currentFunction, functionName, callByteOffset));
  }

  private void resolveCalls() {

    for (List<CallSite> sites : callSites.values()) {

      for (CallSite site : sites) {

        Integer targetOffset = functionOffsets.get(site.target);

        if (targetOffset == null) {

          throw new RuntimeException(
              "Code generation error: unknown " + "function target '" + site.target + "'");
        }

        Integer callerOffset = functionOffsets.get(site.caller);

        if (callerOffset == null) {

          throw new RuntimeException(
              "Code generation error: unknown " + "caller '" + site.caller + "'");
        }

        /*
         * BL's PC is the address of the BL instruction.
         *
         * Calculate:
         *
         *     target - currentInstruction
         *
         * The call site is relative to the beginning
         * of its function.
         */
        int currentInstructionOffset = callerOffset + site.byteOffset;

        int byteOffset = targetOffset - currentInstructionOffset;

        MachineCodeBuffer callerCode = functionCode.get(site.caller);

        callerCode.patch(site.byteOffset, Arm64.bl(byteOffset));
      }
    }
  }

  private void appendFunction(MachineCodeBuffer functionBuffer) {

    byte[] bytes = functionBuffer.toByteArray();

    /*
     * MachineCodeBuffer deliberately exposes only
     * instruction emission, so copy each instruction
     * back into the final buffer.
     */
    for (int i = 0; i < bytes.length; i += 4) {

      int instruction =
          (bytes[i] & 0xFF)
              | ((bytes[i + 1] & 0xFF) << 8)
              | ((bytes[i + 2] & 0xFF) << 16)
              | ((bytes[i + 3] & 0xFF) << 24);

      code.emit(instruction);
    }
  }
}
