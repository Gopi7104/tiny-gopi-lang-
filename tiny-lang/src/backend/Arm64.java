package backend;

public final class Arm64 {

  private Arm64() {}

  // Register helpers

  /**
   * ARM64 uses:
   *
   * <p>X0-X30 for 64-bit registers W0-W30 for the lower 32 bits
   *
   * <p>Our first TinyGopiLang version works with 32-bit num values, so the arithmetic instructions
   * below use W registers.
   */
  public static int w(int register) {

    if (register < 0 || register > 31) {
      throw new IllegalArgumentException("Invalid ARM64 register: " + register);
    }

    return register;
  }


  // MOV Wd, #imm
  //
  // MOV Wd, #immediate
  //
  // Uses the ARM64 MOVZ instruction.
  //
  // For TinyGopiLang v1, immediate values must fit in
  // the 16-bit unsigned immediate range.
  //
  // Example:
  //
  //     MOV W0, #10

  public static int movWImmediate(int destination, int value) {

    checkRegister(destination);

    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException("Immediate value out of range for MOV Wd, #imm: " + value);
    }

    /*
     * MOVZ Wd, #imm16
     *
     * sf     = 0       -> 32-bit W register
     * fixed  = 100101
     * hw     = 00      -> no shift
     *
     * Encoding:
     *
     * 0 10 100101 00 imm16 Rd
     *
     * Base:
     *
     * 0x52800000
     */

    return 0x52800000 | (value << 5) | destination;
  }

  // MOVK Wd, #imm, LSL #16

  public static int movkWImmediateLSL16(int destination, int value) {

    checkRegister(destination);

    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException(
          "Immediate value out of range for MOVK Wd, #imm: " + value);
    }

    /*
     * MOVK Wd, #imm16, LSL #16
     * Base: 0x72A00000
     */

    return 0x72A00000 | (value << 5) | destination;
  }

  // BL
  //
  // Branch with Link.
  //
  // The caller supplies the PC-relative byte offset.
  //
  //     target = currentPC + byteOffset
  //
  // ARM64 BL uses:
  //
  //     imm26 = byteOffset / 4
  //
  // The offset must be instruction-aligned and fit in
  // the signed 26-bit immediate.

  public static int bl(int byteOffset) {

    if ((byteOffset & 0x3) != 0) {
      throw new IllegalArgumentException("ARM64 BL target must be 4-byte aligned: " + byteOffset);
    }

    int imm26 = byteOffset >> 2;

    if (imm26 < -(1 << 25) || imm26 > ((1 << 25) - 1)) {

      throw new IllegalArgumentException("ARM64 BL target out of range: " + byteOffset);
    }

    /*
     * BL encoding:
     *
     * 100101 imm26
     *
     * Base:
     *
     * 0x94000000
     */

    return 0x94000000 | (imm26 & 0x03FFFFFF);
  }

  // ADD Wd, Wn, Wm
  //
  // Wd = Wn + Wm
  //
  // eg
  // ADD W0, W0, W1

  public static int addW(int destination, int left, int right) {

    checkRegister(destination);
    checkRegister(left);
    checkRegister(right);

    /*
     * AArch64 ADD (shifted register), 32-bit:
     *
     * sf    = 0
     * op    = 0
     * S     = 0
     * fixed = 0b01011
     * shift = 00
     *
     * Encoding:
     *
     * 00001011 000xxxxx xxxxxxx xxxxx
     *
     * More precisely:
     *
     * sf       bit 31
     * op       bit 30
     * S        bit 29
     * fixed    bits 28..24
     * shift    bits 23..22
     * Rm       bits 20..16
     * shamt    bits 15..10
     * Rn       bits 9..5
     * Rd       bits 4..0
     */

    return 0x0B000000 | (right << 16) | (left << 5) | destination;
  }

  // MOV Wd, Wn
  //
  // Implemented using:
  //
  // ORR Wd, WZR, Wn
  //
  // This avoids needing a separate MOV encoding.

  public static int movW(int destination, int source) {

    checkRegister(destination);
    checkRegister(source);

    /*
     * ORR Wd, WZR, Wn
     *
     * Encoding:
     *
     * 32-bit ORR shifted register
     *
     * WZR = register 31
     */

    return 0x2A0003E0 | (source << 16) | destination;
  }

  // RET
  //
  // Return from the current function.
  //
  // RET X30

  public static int ret() {

    return 0xD65F03C0;
  }

  // NOP
  //
  // Useful during development/debugging.

  public static int nop() {

    return 0xD503201F;
  }

  // PUSH Xn
  //
  // STR Xn, [SP, #-16]!

  public static int pushX(int register) {
    checkRegister(register);
    return 0xF81F0FE0 | register;
  }

  // POP Xn
  //
  // LDR Xn, [SP], #16

  public static int popX(int register) {
    checkRegister(register);
    return 0xF84107E0 | register;
  }

  // SVC #imm

  public static int svc(int immediate) {

    if (immediate < 0 || immediate > 0xFFFF) {
      throw new IllegalArgumentException("SVC immediate out of range: " + immediate);
    }

    return 0xD4000001 | (immediate << 5);
  }

  // Register validation

  private static void checkRegister(int register) {

    if (register < 0 || register > 31) {
      throw new IllegalArgumentException("Invalid ARM64 register: " + register);
    }
  }
}
