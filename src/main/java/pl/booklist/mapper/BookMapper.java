package pl.booklist.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.booklist.dto.BookDTO;
import pl.booklist.model.Book;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookMapper {
    BookDTO toDto(Book book);
    @Mapping(target = "owner", ignore = true)
    Book toEntity(BookDTO dto);

    List<BookDTO> toDtoList(List<Book> books);

}