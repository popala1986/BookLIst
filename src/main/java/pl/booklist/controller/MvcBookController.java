package pl.booklist.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.booklist.dto.BookDTO;
import pl.booklist.exception.BookNotFoundException;
import pl.booklist.service.BookService;

/**
 * Controller for handling Thymeleaf views (pages).
 * Handles adding books to the wishlist and displaying the main book lists.
 */
@Controller
public class MvcBookController {

    private final BookService bookService;

    public MvcBookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ownedBooks", bookService.findOwnedBooks());
        model.addAttribute("wishlistBooks", bookService.findUnownedBooks());
        return "index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("newBook", new BookDTO());
        return "book-add";
    }

    @PostMapping("/add")
    public String addBookToWishlist(@Valid @ModelAttribute("newBook") BookDTO bookDTO,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "book-add";
        }
        bookDTO.setOwned(false);
        BookDTO saved = bookService.addBook(bookDTO);

        redirectAttributes.addFlashAttribute("message", "Książka '" + saved.getTitle() + "' dodana do listy życzeń!");
        return "redirect:/";
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Książka została usunięta.");
        } catch (BookNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/books/moveToOwned/{id}")
    public String moveToOwned(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.markAsOwned(id);
            redirectAttributes.addFlashAttribute("message", "Książka została dodana do kolekcji.");
        } catch (BookNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }
}
