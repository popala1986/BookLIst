package pl.booklist.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.booklist.dto.BookDTO;
import pl.booklist.mapper.BookMapper;
import pl.booklist.model.Book;
import pl.booklist.repository.BookRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private final Book bookEntity1 = new Book(1L, "Wiedźmin", "Sapkowski", true, "url1");
    private final Book bookEntity2 = new Book(2L, "Lód", "Dukaj", false, "url2");

    private final BookDTO bookDto1 = new BookDTO(1L, "Wiedźmin DTO", "Sapkowski DTO", true, "url1");
    private final BookDTO bookDto2 = new BookDTO(2L, "Lód DTO", "Dukaj DTO", false, "url2");

    @Test
    @DisplayName("should find all books and map them to DTOs")
    void findAllBooks_shouldReturnMappedDTOList() {
        //given
        List<Book> entityList = List.of(bookEntity1, bookEntity2);

        when(bookRepository.findAll()).thenReturn(entityList);

        when(bookMapper.toDto(bookEntity1)).thenReturn(bookDto1);
        when(bookMapper.toDto(bookEntity2)).thenReturn(bookDto2);

        // WHEN
        List<BookDTO> result = bookService.findAllBooks();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(bookDto1, bookDto2);

        verify(bookRepository, times(1)).findAll();

        verify(bookMapper, times(1)).toDto(bookEntity1);
        verify(bookMapper, times(1)).toDto(bookEntity2);
    }

    @Test
    @DisplayName("should find unowned books and map them to DTOs")
    void findUnownedBooks_shouldReturnOnlyUnownedMappedDTOs() {
        // GIVEN
        List<Book> unownedEntityList = List.of(bookEntity2);

        when(bookRepository.findByOwnedFalse()).thenReturn(unownedEntityList);
        when(bookMapper.toDto(bookEntity2)).thenReturn(bookDto2);

        // WHEN
        List<BookDTO> result = bookService.findUnownedBooks();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOwned()).isFalse();

        verify(bookRepository, times(1)).findByOwnedFalse();
        verify(bookMapper, times(1)).toDto(bookEntity2);
    }

    @Test
    @DisplayName("should find owned books and map them to DTOs")
    void findOwnedBooks_shouldReturnOnlyOwnedMappedDTOs() {
        // GIVEN
        List<Book> ownedEntityList = List.of(bookEntity1);

        when(bookRepository.findByOwnedTrue()).thenReturn(ownedEntityList);
        when(bookMapper.toDto(bookEntity1)).thenReturn(bookDto1);

        // WHEN
        List<BookDTO> result = bookService.findOwnedBooks();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOwned()).isTrue();
        assertThat(result).containsExactly(bookDto1);

        verify(bookRepository, times(1)).findByOwnedTrue();
        verify(bookMapper, times(1)).toDto(bookEntity1);
    }

    @Test
    @DisplayName("should return an empty list when no books are found")
    void findAllBooks_shouldReturnEmptyListWhenRepositoryIsEmpty() {
        // GIVEN
        List<Book> emptyList = List.of();
        when(bookRepository.findAll()).thenReturn(emptyList);

        // WHEN
        List<BookDTO> result = bookService.findAllBooks();

        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(bookRepository, times(1)).findAll();

        verify(bookMapper, never()).toDto(any(Book.class));
    }
}
