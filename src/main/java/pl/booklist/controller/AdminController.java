package pl.booklist.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.booklist.dto.RegisterForm;
import pl.booklist.exception.UsernameTakenException;
import pl.booklist.service.UserService;

/**
 * Administrator panel, available only to the ADMIN account (see SecurityConfig):
 * overview of accounts, adding users and setting a new password for someone who forgot theirs.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 100;

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        var users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("totalOwned", users.stream().mapToLong(UserService.UserSummary::ownedBooks).sum());
        model.addAttribute("totalWishlist", users.stream().mapToLong(UserService.UserSummary::wishlistBooks).sum());
        return "admin";
    }

    @PostMapping("/users")
    public String addUser(@Valid @ModelAttribute("form") RegisterForm form,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Hasła nie są takie same");
        }
        if (!bindingResult.hasErrors()) {
            try {
                userService.register(form.getUsername(), form.getPassword());
                redirectAttributes.addFlashAttribute("message",
                        "Dodano użytkownika " + form.getUsername().trim() + ". Przekaż tej osobie login i hasło.");
                return "redirect:/admin";
            } catch (UsernameTakenException e) {
                bindingResult.rejectValue("username", "taken", "Ta nazwa użytkownika jest już zajęta");
            }
        }
        return dashboard(model);
    }

    @PostMapping("/users/{id}/password")
    public String setPassword(@PathVariable Long id,
                              @RequestParam String password,
                              RedirectAttributes redirectAttributes) {
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            redirectAttributes.addFlashAttribute("error", "Nowe hasło musi mieć co najmniej 8 znaków.");
            return "redirect:/admin";
        }
        userService.setPassword(id, password).ifPresentOrElse(
                username -> redirectAttributes.addFlashAttribute("message",
                        "Ustawiono nowe hasło dla " + username + ". Przekaż je tej osobie, a potem może je zmienić w „Zmień hasło”."),
                () -> redirectAttributes.addFlashAttribute("error", "Nie ma takiego użytkownika."));
        return "redirect:/admin";
    }
}
