package pl.booklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for transferring book data between layers.
 * Contains basic information about a book: title, author, and ownership status.
 * Used for input/output operations without exposing JPA entity internals.
 *
 * @author Pawel
 * @version 1.0
 */
public class BookDTO {

    /**
     * Title of the book. Cannot be blank and must not exceed 50 characters.
     */
    @NotBlank
    @Size(max = 50)
    private String title;

    /**
     * Author of the book. Cannot be blank and must not exceed 100 characters.
     */
    @NotBlank
    @Size(max = 100)
    private String author;

    /**
     * Indicates whether the book is owned by the user.
     */
    private boolean owned;

    /**
     * Default constructor.
     */
    public BookDTO() {
    }

    /**
     * Constructs a new BookDTO with the specified title, author, and ownership status.
     *
     * @param title  the title of the book
     * @param author the author of the book
     * @param owned  true if the book is owned, false otherwise
     */
    public BookDTO(String title, String author, boolean owned) {
        this.title = title;
        this.author = author;
        this.owned = owned;
    }

    /**
     * Constructs a BookDTO from a Book entity.
     *
     * @param book the Book entity to copy from
     */
    public BookDTO(pl.booklist.model.Book source) {
        this.title = source.getTitle();
        this.author = source.getAuthor();
        this.owned = source.isOwned();
    }

    /**
     * Returns the title of the book.
     *
     * @return the book title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the book.
     *
     * @param title the book title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the author of the book.
     *
     * @return the book author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the book.
     *
     * @param author the book author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Returns whether the book is owned.
     *
     * @return true if owned, false otherwise
     */
    public boolean isOwned() {
        return owned;
    }

    /**
     * Sets the ownership status of the book.
     *
     * @param owned true if owned, false otherwise
     */
    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    /**
     * Returns a string representation of the BookDTO.
     *
     * @return a string with book details
     */
    @Override
    public String toString() {
        return "BookDTO{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", owned=" + owned +
                '}';
    }
}
