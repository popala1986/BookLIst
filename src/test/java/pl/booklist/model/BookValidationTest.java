package pl.booklist.model;


import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("valid book should pass validation")
    void validBookShouldPassValidation() {
        Book book = new Book(null,"Clean Code", "Robert C. Martin", true, "http://cover.url");
        Set<ConstraintViolation<Book>> violations = validator.validate(book);

        assertTrue(violations.isEmpty(), "Expected no validation errors for a valid book");
    }

    @Test
    @DisplayName("Blank title should fail validation")
    void blankTitleShouldFailValidation() {
        Book book = new Book(null,"", "Robert C. Martin", true, "http://cover.url");

        Set<ConstraintViolation<Book>> violations = validator.validate(book);

        assertFalse(violations.isEmpty(), "Expected validation errors for blank title");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")),
                "Expected validation error on 'title' field"
        );
    }

    @Test
    @DisplayName("Null author should fail validation")
    void nullAuthorShouldFailValidation() {
        Book book = new Book(null,"Robinson Kruzoe", null, true, "http://cover.url");

        Set<ConstraintViolation<Book>> violations = validator.validate(book);

        assertFalse(violations.isEmpty(), "Expected validation errors for null author");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("author")),
                "Expected validation error on 'author' field"
        );
    }

@Test
@DisplayName("Too long author should fail validation")
void tooLongAuthorShouldFailValidation() {
    String longAuthor = "Robinson Kruzoe".repeat(20); // > 100 characters
    Book book = new Book(null,"Clean Code", longAuthor, true, "http://cover.url");

    Set<ConstraintViolation<Book>> violations = validator.validate(book);

    assertFalse(violations.isEmpty(), "Expected validation errors for too long author's name");
    assertTrue(
            violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("author")),
            "Expected validation error on 'author' field"
    );
}


    @Test
    @DisplayName("Invalid URL should fail validation (only if @URL is used)")
    void invalidUrlShouldFailValidation() {
        Book book = new Book(null,"Valid Title", "Valid Author", true, "invalid-url");

        Set<ConstraintViolation<Book>> violations = validator.validate(book);

        assertFalse(violations.isEmpty(), "Expected validation error for invalid URL value");
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("coverUrl")),
                "Expected validation error on 'coverUrl' field"
        );
    }
}
