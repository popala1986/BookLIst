package pl.booklist.mapper;

import org.mapstruct.Mapper;
import pl.booklist.dto.BookDTO;
import pl.booklist.model.Book;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {
    BookDTO toDto(Book book);
    Book toEntity(BookDTO dto);

    List<BookDTO> toDtoList(List<Book> books);
    List<Book> toEntityList(List<BookDTO> dtos);

}