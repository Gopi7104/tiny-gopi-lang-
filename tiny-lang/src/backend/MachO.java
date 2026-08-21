package backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MachO {

  private static void write32(ByteArrayOutputStream out, int value) {
    out.write(value & 0xFF);
    out.write((value >>> 8) & 0xFF);
    out.write((value >>> 16) & 0xFF);
    out.write((value >>> 24) & 0xFF);
  }

  private static void write64(ByteArrayOutputStream out, long value) {
    write32(out, (int) value);
    write32(out, (int) (value >>> 32));
  }

  private static void writeString(ByteArrayOutputStream out, String str, int exactLength) {
    byte[] bytes = str.getBytes();
    for (int i = 0; i < exactLength; i++) {
      if (i < bytes.length) out.write(bytes[i]);
      else out.write(0);
    }
  }

  public static byte[] generateObjectFile(byte[] machineCode) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    int LC_SEGMENT_64 = 0x19;
    int LC_SYMTAB = 0x2;

    int segmentCmdSize = 72 + 80;
    int symtabCmdSize = 24;
    int sizeofcmds = segmentCmdSize + symtabCmdSize;

    // 1. Header (32 bytes)
    write32(out, 0xfeedfacf);
    write32(out, 0x0100000c);
    write32(out, 0x00000000);
    write32(out, 0x1);
    write32(out, 2);
    write32(out, sizeofcmds);
    write32(out, 0);
    write32(out, 0);

    // 2. Segment Command (72 bytes)
    write32(out, LC_SEGMENT_64);
    write32(out, segmentCmdSize);
    writeString(out, "", 16);
    write64(out, 0);
    write64(out, machineCode.length);

    int codeOffset = 32 + sizeofcmds;
    write64(out, codeOffset);
    write64(out, machineCode.length);
    write32(out, 7);
    write32(out, 7);
    write32(out, 1);
    write32(out, 0);

    // 3. Section Command (80 bytes)
    writeString(out, "__text", 16);
    writeString(out, "__TEXT", 16);
    write64(out, 0);
    write64(out, machineCode.length);
    write32(out, codeOffset);
    write32(out, 2); // align = 2^2 = 4 bytes
    write32(out, 0);
    write32(out, 0);
    write32(out, 0x80000400); // flags
    write32(out, 0);
    write32(out, 0);
    write32(out, 0);

    // 4. Symtab Command (24 bytes)
    write32(out, LC_SYMTAB);
    write32(out, symtabCmdSize);

    int symoff = codeOffset + machineCode.length;
    write32(out, symoff);
    write32(out, 1); // nsyms

    int stroff = symoff + 16;
    write32(out, stroff);
    write32(out, 16); // strsize

    // 5. Machine Code
    out.write(machineCode);

    // 6. nlist_64 (16 bytes)
    write32(out, 1); // n_strx
    out.write(0x0f); // n_type (N_SECT | N_EXT)
    out.write(1); // n_sect
    out.write(0); // n_desc
    out.write(0); // n_desc
    write64(out, 0); // n_value (offset 0, which is _start)

    // 7. String Table (16 bytes)
    out.write(0);
    out.write("_main".getBytes());
    for (int i = 0; i < 10; i++) {
      out.write(0);
    }

    return out.toByteArray();
  }
}
