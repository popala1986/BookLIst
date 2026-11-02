package pl.booklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Data Transfer Object for transferring book data between layers.
 * Contains basic information about a book: id, title, author, ownership status, and cover image URL.
 * Used for input/output operations without exposing JPA entity internals.
 *
 * @author Pawel Opala
 */
public class BookDTO {

    /**
     * Unique identifier of the book.
     */
    private Long id;

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
     * URL of the book's cover image.
     */
    private String coverUrl;

    /**
     * Default constructor.
     */
    public BookDTO() {
    }

    /**
     * Constructs a new BookDTO with all fields.
     *
     * @param id        the unique identifier of the book
     * @param title     the title of the book
     * @param author    the author of the book
     * @param owned     true if the book is owned, false otherwise
     * @param coverUrl  the URL of the book's cover image
     */
    public BookDTO(Long id, String title, String author, boolean owned, String coverUrl) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.owned = owned;
        this.coverUrl = coverUrl;
    }

    /**
     * Constructs a new BookDTO without id (used for creation).
     *
     * @param title     the title of the book
     * @param author    the author of the book
     * @param owned     true if the book is owned, false otherwise
     * @param coverUrl  the URL of the book's cover image
     */
    public BookDTO(String title, String author, boolean owned, String coverUrl) {
        this.title = title;
        this.author = author;
        this.owned = owned;
        this.coverUrl = coverUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isOwned() {
        return owned;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookDTO bookDTO = (BookDTO) o;
        return owned == bookDTO.owned &&
                Objects.equals(id, bookDTO.id) &&
                Objects.equals(title, bookDTO.title) &&
                Objects.equals(author, bookDTO.author) &&
                Objects.equals(coverUrl, bookDTO.coverUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, owned, coverUrl);
    }

    @Override
    public String toString() {
        return "BookDTO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", owned=" + owned +
                ", coverUrl='" + coverUrl + '\'' +
                '}';
    }
}
