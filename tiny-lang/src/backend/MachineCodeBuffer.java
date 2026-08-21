package backend;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class MachineCodeBuffer {

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  // Emit one 32-bit ARM64 instruction

  public void emit(int instruction) {

    /*
     * ARM64 instructions are always 32 bits.
     *
     * On little-endian ARM64 machines, the least significant
     * byte comes first in memory.
     *
     * Example:
     *
     *   instruction = 0xD65F03C0
     *
     * memory:
     *
     *   C0 03 5F D6
     */

    buffer.write(instruction & 0xFF);
    buffer.write((instruction >>> 8) & 0xFF);
    buffer.write((instruction >>> 16) & 0xFF);
    buffer.write((instruction >>> 24) & 0xFF);
  }

  // Current size

  public int size() {
    return buffer.size();
  }

  // Current instruction count
  //
  // Every ARM64 instruction is exactly 4 bytes.

  public int instructionCount() {
    return buffer.size() / 4;
  }

  // Return a copy of the generated machine code

  public byte[] toByteArray() {
    return buffer.toByteArray();
  }

  // Clear the buffer

  public void clear() {

    buffer.reset();
  }

  // Patch one existing 32-bit ARM64 instruction
  //
  // byteOffset must point to the beginning of an instruction.

  public void patch(int byteOffset, int instruction) {

    if (byteOffset < 0 || byteOffset + 4 > buffer.size()) {

      throw new IllegalArgumentException("Invalid machine-code patch offset: " + byteOffset);
    }

    byte[] bytes = buffer.toByteArray();

    bytes[byteOffset] = (byte) (instruction & 0xFF);

    bytes[byteOffset + 1] = (byte) ((instruction >>> 8) & 0xFF);

    bytes[byteOffset + 2] = (byte) ((instruction >>> 16) & 0xFF);

    bytes[byteOffset + 3] = (byte) ((instruction >>> 24) & 0xFF);

    buffer.reset();

    buffer.writeBytes(bytes);
  }

  // Debug representation

  public String hexDump() {

    byte[] bytes = buffer.toByteArray();

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < bytes.length; i++) {

      if (i > 0) {
        result.append(' ');
      }

      result.append(String.format("%02X", bytes[i] & 0xFF));
    }

    return result.toString();
  }

  @Override
  public String toString() {

    return Arrays.toString(buffer.toByteArray());
  }
}
