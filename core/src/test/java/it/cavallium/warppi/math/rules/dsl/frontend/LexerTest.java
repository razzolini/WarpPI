package it.cavallium.warppi.math.rules.dsl.frontend;

import it.cavallium.warppi.math.rules.dsl.DslError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static it.cavallium.warppi.math.rules.dsl.frontend.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {
	private final List<DslError> errors = new ArrayList<>();

	@BeforeEach
	void setUp() {
		errors.clear();
	}

	@Test
	void emptyInput() {
		final Lexer lexer = new Lexer("", errors::add);
		final List<Token> expected = Collections.singletonList(
			new Token(EOF, "", 0)
		);
		assertEquals(expected, lexer.lex());
	}

	@Test
	void multipleLexCalls() {
		final Lexer lexer = new Lexer("", errors::add);
		lexer.lex();
		assertThrows(IllegalStateException.class, lexer::lex);
	}

	@Test
	void validRule() {
		final Lexer lexer = new Lexer(
				"reduction TestRule_123:\n" +
				"  x + y * z = -(a_123 +- 3 / 2.2) -> [\n" +
				"    x^a_123 = cos(pi) - log(e, e), // comment\n" +
				"    undefined, /*\n" +
				"comment */ ]\n",
				errors::add
		);
		final List<Token> expected = Arrays.asList(
				new Token(REDUCTION, "reduction", 0),
				new Token(IDENTIFIER, "TestRule_123", 10),
				new Token(COLON, ":", 22),
				new Token(IDENTIFIER, "x", 26),
				new Token(PLUS, "+", 28),
				new Token(IDENTIFIER, "y", 30),
				new Token(TIMES, "*", 32),
				new Token(IDENTIFIER, "z", 34),
				new Token(EQUALS, "=", 36),
				new Token(MINUS, "-", 38),
				new Token(LEFT_PAREN, "(", 39),
				new Token(IDENTIFIER, "a_123", 40),
				new Token(PLUS_MINUS, "+-", 46),
				new Token(NUMBER, "3", 49),
				new Token(DIVIDE, "/", 51),
				new Token(NUMBER, "2.2", 53),
				new Token(RIGHT_PAREN, ")", 56),
				new Token(ARROW, "->", 58),
				new Token(LEFT_BRACKET, "[", 61),
				new Token(IDENTIFIER, "x", 67),
				new Token(POWER, "^", 68),
				new Token(IDENTIFIER, "a_123", 69),
				new Token(EQUALS, "=", 75),
				new Token(COS, "cos", 77),
				new Token(LEFT_PAREN, "(", 80),
				new Token(PI, "pi", 81),
				new Token(RIGHT_PAREN, ")", 83),
				new Token(MINUS, "-", 85),
				new Token(LOG, "log", 87),
				new Token(LEFT_PAREN, "(", 90),
				new Token(E, "e", 91),
				new Token(COMMA, ",", 92),
				new Token(E, "e", 94),
				new Token(RIGHT_PAREN, ")", 95),
				new Token(COMMA, ",", 96),
				new Token(UNDEFINED, "undefined", 113),
				new Token(COMMA, ",", 122),
				new Token(RIGHT_BRACKET, "]", 138),
				new Token(EOF, "", 140)
		);
		assertEquals(expected, lexer.lex());
		assertTrue(errors.isEmpty());
	}

	@Test
	void incompleteNumberOtherChar() {
		final Lexer lexer = new Lexer("2. 5 + 3", errors::add);

		final List<Token> expectedTokens = Arrays.asList(
				new Token(NUMBER, "5", 3),
				new Token(PLUS, "+", 5),
				new Token(NUMBER, "3", 7),
				new Token(EOF, "", 8)
		);
		assertEquals(expectedTokens, lexer.lex());

		final List<DslError> expectedErrors = Collections.singletonList(
				new IncompleteNumberLiteral(0, "2.")
		);
		assertEquals(expectedErrors, errors);
	}

	@Test
	void incompleteNumberEof() {
		final Lexer lexer = new Lexer("2.", errors::add);

		final List<Token> expectedTokens = Collections.singletonList(
				new Token(EOF, "", 2)
		);
		assertEquals(expectedTokens, lexer.lex());

		final List<DslError> expectedErrors = Collections.singletonList(
				new IncompleteNumberLiteral(0, "2.")
		);
		assertEquals(expectedErrors, errors);
	}

	@Test
	void unexpectedCharacters() {
		final Lexer lexer = new Lexer("reduction @| .: {}", errors::add);

		final List<Token> expectedTokens = Arrays.asList(
				new Token(REDUCTION, "reduction", 0),
				new Token(COLON, ":", 14),
				new Token(EOF, "", 18)
		);
		assertEquals(expectedTokens, lexer.lex());

		final List<DslError> expectedErrors = Arrays.asList(
				new UnexpectedCharacters(10, "@|"),
				new UnexpectedCharacters(13, "."),
				new UnexpectedCharacters(16, "{}")
		);
		assertEquals(expectedErrors, errors);
	}

	@Test
	void unterminatedComment() {
		final Lexer lexer = new Lexer("reduction /* test:\n x -> x", errors::add);

		final List<Token> expectedTokens = Arrays.asList(
				new Token(REDUCTION, "reduction", 0),
				new Token(EOF, "", 26)
		);
		assertEquals(expectedTokens, lexer.lex());

		final List<DslError> expectedErrors = Collections.singletonList(
				new UnterminatedComment(10)
		);
		assertEquals(expectedErrors, errors);
	}

	@Test
	void errorOrder() {
		final Lexer lexer = new Lexer(".2. @", errors::add);

		final List<Token> expectedTokens = Collections.singletonList(
				new Token(EOF, "", 5)
		);
		assertEquals(expectedTokens, lexer.lex());

		final List<DslError> expectedErrors = Arrays.asList(
				new UnexpectedCharacters(0, "."),
				new IncompleteNumberLiteral(1, "2."),
				new UnexpectedCharacters(4, "@")
		);
		assertEquals(expectedErrors, errors);
	}
}
