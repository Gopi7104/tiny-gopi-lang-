Gopi-Lang is a simple native programming language with its own compiler.

  The compiler is built completely in Java, while Gopi-Lang is the language being compiled. The compiler converts Gopi-Lang source code directly into native machine code.

Current Status: Early development — currently supports functions for adding two or more numbers.

Compiler

The compiler is written entirely in Java and follows a traditional compiler pipeline:

Gopi-Lang Source Code
        ↓
      Lexer
        ↓
      Parser
        ↓
       AST
        ↓
Semantic Analysis
        ↓
  Code Generation
        ↓
   Machine Code

How to Run

Use the gopic compiler command:

gopic path/filename.gopi

Example:

gopic examples/add.gopi

Current Language Support

At the moment, Gopi-Lang supports a function that adds two or more numbers.

Example:

func add(num a, num b) {
    a + b
}

This is the initial version of the language. More features will be added as development continues.

 Platform Support

Currently, Gopi-Lang is built for:

*  macOS

Future versions can be expanded to support:

*  Linux
*  Windows
*  Other architectures and platforms

 Future Plans

The language will gradually be expanded with features such as:

* Variables
* More data types
* Arithmetic operations
* Conditional statements
* Loops
* Arrays
* Strings
* Multiple functions
* Function return values
* Better error handling
* Compiler optimizations
* Cross-platform support

 Project Status

Gopi-Lang is currently an experimental compiler project focused on understanding how a programming language works internally — from source code and lexical analysis all the way to native machine code.

The current implementation is intentionally small, providing a foundation for building a more complete native programming language in the future.
