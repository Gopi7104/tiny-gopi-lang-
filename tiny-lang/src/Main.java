import ast.Program;
import backend.CodeGenerator;
import backend.MachineCodeBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

  public static void main(String[] args) {

    if (args.length < 1 || args.length > 3) {
      System.err.println("Usage: java Main <source.gopi> [-o output]");
      System.exit(1);
    }

    String sourceFile = args[0];

    try {

      String source = Files.readString(Path.of(sourceFile));


      Lexer lexer = new Lexer(source);

      var tokens = lexer.tokenize();


      Parser parser = new Parser(tokens);

      Program program = parser.parse();


      SemanticAnalyzer analyzer = new SemanticAnalyzer();

      analyzer.analyze(program);


      CodeGenerator generator = new CodeGenerator();

      MachineCodeBuffer machineCode = generator.generate(program);
      byte[] bytes = machineCode.toByteArray();

      if (args.length >= 2) {
        String outputName = args[1];
        if (args.length == 3 && args[1].equals("-o")) {
          outputName = args[2];
        }

        Path objectFile = Path.of(outputName + ".o");
        byte[] machO = backend.MachO.generateObjectFile(bytes);
        Files.write(objectFile, machO);

        ProcessBuilder pb =
            new ProcessBuilder(
                "sh",
                "-c",
                "ld -w -o "
                    + outputName
                    + " "
                    + objectFile
                    + " -e _main -lSystem -syslibroot $(xcrun -sdk macosx --show-sdk-path)"
                    + " -platform_version macos 11.0 11.0 && codesign -s - "
                    + outputName);
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();

        if (exitCode != 0) {
          System.err.println("Linker failed with exit code: " + exitCode);
        }
      }


    } catch (RuntimeException e) {

      System.err.println("Compilation failed:");

      System.err.println(e.getMessage());

      System.exit(1);

    } catch (Exception e) {

      System.err.println("I/O error:");

      System.err.println(e.getMessage());

      System.exit(1);
    }
  }
}
