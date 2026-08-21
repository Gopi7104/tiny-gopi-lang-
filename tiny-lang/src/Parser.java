import ast.Expression;
import ast.Function;
import ast.Program;
import java.util.ArrayList;
import java.util.List;

public class Parser {

  private final List<Token> tokens;
  private int current = 0;

  public Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  public Program parse() {

    List<Function> functions = new ArrayList<>();

    while (!check(TokenType.EOF)) {
      functions.add(parseFunction());
    }

    return new Program(functions);
  }

  private Function parseFunction() {

    consume(TokenType.FUNC, "Expected 'func' at beginning of function.");

    Token name = consume(TokenType.IDENTIFIER, "Expected function name.");

    consume(TokenType.LPAREN, "Expected '(' after function name.");

    List<Function.Parameter> parameters = new ArrayList<>();

    if (!check(TokenType.RPAREN)) {

      do {
        parameters.add(parseParameter());
      } while (match(TokenType.COMMA));
    }

    consume(TokenType.RPAREN, "Expected ')' after parameters.");

    consume(TokenType.LBRACE, "Expected '{' before function body.");

    match(TokenType.RETURN);

    Expression returnExpression = parseExpression();

    consume(TokenType.SEMICOLON, "Expected ';' after return expression.");

    consume(TokenType.RBRACE, "Expected '}' after function body.");

    return new Function(name.lexeme(), parameters, returnExpression);
  }

  private Function.Parameter parseParameter() {

    consume(TokenType.NUM, "Expected 'num' before parameter.");

    Token name = consume(TokenType.IDENTIFIER, "Expected parameter name.");

    return new Function.Parameter(name.lexeme());
  }

  private Expression parseExpression() {
    return parseAddition();
  }

  private Expression parseAddition() {

    Expression expression = parsePrimary();

    while (match(TokenType.PLUS)) {

      Expression right = parsePrimary();

      expression = new Expression.BinaryAdd(expression, right);
    }

    return expression;
  }

  private Expression parsePrimary() {

    // Number literal

    if (match(TokenType.NUMBER)) {

      Token token = previous();

      int value;

      try {

        value = Integer.parseInt(token.lexeme());

      } catch (NumberFormatException e) {

        throw error(token, "Invalid integer literal: " + token.lexeme());
      }

      return new Expression.NumberLiteral(value);
    }

    if (match(TokenType.IDENTIFIER)) {

      Token name = previous();

      // Function call

      if (match(TokenType.LPAREN)) {

        List<Expression> arguments = new ArrayList<>();

        if (!check(TokenType.RPAREN)) {

          do {

            arguments.add(parseExpression());

          } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after function arguments.");

        return new Expression.FunctionCall(name.lexeme(), arguments);
      }

      // Normal variable

      return new Expression.Variable(name.lexeme());
    }

    // Parenthesized expression

    if (match(TokenType.LPAREN)) {

      Expression expression = parseExpression();

      consume(TokenType.RPAREN, "Expected ')' after expression.");

      return expression;
    }

    throw error(peek(), "Expected expression.");
  }

  private boolean match(TokenType... types) {

    for (TokenType type : types) {

      if (check(type)) {

        advance();

        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {

    if (check(type)) {
      return advance();
    }

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {

    if (isAtEnd()) {
      return type == TokenType.EOF;
    }

    return peek().type() == type;
  }

  private Token advance() {

    if (!isAtEnd()) {
      current++;
    }

    return previous();
  }

  private boolean isAtEnd() {

    return peek().type() == TokenType.EOF;
  }

  private Token peek() {

    return tokens.get(current);
  }

  private Token previous() {

    return tokens.get(current - 1);
  }

  // Errors
  private RuntimeException error(Token token, String message) {

    return new RuntimeException(
        "Parser error at " + token.line() + ":" + token.column() + ": " + message);
  }
}
