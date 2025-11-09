package pl.booklist.mapper;

import pl.booklist.dto.BookDTO;
import pl.booklist.model.Book;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between Book entities and BookDTOs.
 * Provides methods to convert data between persistence and presentation layers.
 * This mapper is stateless and can be used across the application.
 *
 * @author Paweł Opala
 * @version 1.0
 */
public class BookMapper {

    /**
     * Converts a Book entity to a BookDTO.
     *
     * @param book the Book entity to convert
     * @return the corresponding BookDTO, or null if input is null
     */
    public static BookDTO toDTO(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        return new BookDTO(book.getTitle(),book.getAuthor(),book.isOwned(), book.getCoverUrl());
    }

    /**
     * Converts a BookDTO to a Book entity.
     *
     * @param bookDTO the BookDTO to convert
     * @return the corresponding Book entity, or null if input is null
     */
    public static Book toEntity(BookDTO bookDTO) {
        if (bookDTO == null) {
            throw new IllegalArgumentException("BookDTO cannot be null");
        }
        return new Book(bookDTO.getTitle(), bookDTO.getAuthor(), bookDTO.isOwned(), bookDTO.getCoverUrl());
    }

    /**
     * Converts a list of Book entities to a list of BookDTOs.
     *
     * @param books the list of Book entities to convert
     * @return a list of corresponding BookDTOs; empty list if input is empty
     * @throws IllegalArgumentException if the input list is null
     */
    public static List<BookDTO> toDTOList(List<Book> books) {
        if (books == null) {
            throw new IllegalArgumentException("Book list cannot be null");
        }
        return books.stream()
                .map(BookMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of BookDTOs to a list of Book entities.
     *
     * @param dtos the list of BookDTOs to convert
     * @return a list of corresponding Book entities; empty list if input is empty
     * @throws IllegalArgumentException if the input list is null
     */
    public static List<Book> toEntityList(List<BookDTO> dtos) {
        if (dtos == null) {
            throw new IllegalArgumentException("BookDTO list cannot be null");
        }
        return dtos.stream()
                .map(BookMapper::toEntity)
                .collect(Collectors.toList());
    }
}
