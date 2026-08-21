public enum TokenType {
  // keywords
  FUNC,
  NUM,
  RETURN,

  // identifiers & literals
  IDENTIFIER,
  NUMBER,

  // operators
  PLUS,

  // delimiters
  LPAREN, // (
  RPAREN, // )

  LBRACE, // {
  RBRACE, // }

  COMMA, // ,
  SEMICOLON, // ;

  // End of input
  EOF
}
