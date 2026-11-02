package pl.booklist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import pl.booklist.service.UserService;

/**
 * Administrator panel, available only to the ADMIN account (see SecurityConfig).
 * A starting point for future admin features.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        var users = userService.findAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("totalOwned", users.stream().mapToLong(UserService.UserSummary::ownedBooks).sum());
        model.addAttribute("totalWishlist", users.stream().mapToLong(UserService.UserSummary::wishlistBooks).sum());
        return "admin";
    }
}
