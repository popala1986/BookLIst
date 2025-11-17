package pl.booklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.booklist.dto.BookDTO;
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
    public String addBookToWishlist(@ModelAttribute("newBook") BookDTO bookDTO, RedirectAttributes redirectAttributes) {
        bookDTO.setOwned(false);

        try {
            bookService.addBook(bookDTO);

            redirectAttributes.addFlashAttribute("message", "Książka '" + bookDTO.getTitle() + "' dodana do listy życzeń!");
            redirectAttributes.addFlashAttribute("coverUrl", bookDTO.getCoverUrl());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas dodawania książki: " + e.getMessage());
        }
        return "redirect:/";
    }
}