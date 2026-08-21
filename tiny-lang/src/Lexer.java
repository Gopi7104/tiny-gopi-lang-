import java.util.ArrayList;
import java.util.List;

public class Lexer {

  private final String source;

  private int position = 0;
  private int line = 1;
  private int column = 1;

  public Lexer(String source) {
    this.source = source;
  }

  public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<>();

    while (!isAtEnd()) {
      skipWhitespace();

      if (isAtEnd()) {
        break;
      }

      tokens.add(nextToken());
    }

    tokens.add(new Token(TokenType.EOF, "", line, column));

    return tokens;
  }

  private Token nextToken() {
    int startLine = line;
    int startColumn = column;

    char c = advance();

    switch (c) {
      case '(':
        return token(TokenType.LPAREN, "(", startLine, startColumn);

      case ')':
        return token(TokenType.RPAREN, ")", startLine, startColumn);

      case '{':
        return token(TokenType.LBRACE, "{", startLine, startColumn);

      case '}':
        return token(TokenType.RBRACE, "}", startLine, startColumn);

      case ',':
        return token(TokenType.COMMA, ",", startLine, startColumn);

      case ';':
        return token(TokenType.SEMICOLON, ";", startLine, startColumn);

      case '+':
        return token(TokenType.PLUS, "+", startLine, startColumn);

      default:
        break;
    }

    if (Character.isDigit(c)) {
      return number(c, startLine, startColumn);
    }

    if (Character.isLetter(c) || c == '_') {
      return identifier(c, startLine, startColumn);
    }

    throw error("Unexpected character '" + c + "'", startLine, startColumn);
  }

  // Number

  private Token number(char first, int startLine, int startColumn) {
    StringBuilder value = new StringBuilder();
    value.append(first);

    while (!isAtEnd() && Character.isDigit(peek())) {
      value.append(advance());
    }

    return token(TokenType.NUMBER, value.toString(), startLine, startColumn);
  }

  private Token identifier(char first, int startLine, int startColumn) {

    StringBuilder value = new StringBuilder();
    value.append(first);

    while (!isAtEnd()) {
      char c = peek();

      if (!Character.isLetterOrDigit(c) && c != '_') {
        break;
      }

      value.append(advance());
    }

    String text = value.toString();

    TokenType type =
        switch (text) {
          case "func" -> TokenType.FUNC;
          case "num" -> TokenType.NUM;
          case "return" -> TokenType.RETURN;
          default -> TokenType.IDENTIFIER;
        };

    return token(type, text, startLine, startColumn);
  }

  private void skipWhitespace() {
    while (!isAtEnd()) {
      char c = peek();

      if (c == ' ' || c == '\t' || c == '\r') {
        advance();
        continue;
      }

      if (c == '\n') {
        advance();
        line++;
        column = 1;
        continue;
      }

      break;
    }
  }

  private char advance() {
    char c = source.charAt(position);
    position++;
    column++;
    return c;
  }

  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }

    return source.charAt(position);
  }

  private boolean isAtEnd() {
    return position >= source.length();
  }

  private Token token(TokenType type, String lexeme, int line, int column) {

    return new Token(type, lexeme, line, column);
  }

  private RuntimeException error(String message, int line, int column) {

    return new RuntimeException("Lexer error at " + line + ":" + column + ": " + message);
  }
}
