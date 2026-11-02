package pl.booklist.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.booklist.dto.ChangePasswordForm;
import pl.booklist.service.UserService;

/**
 * The logged-in user's own account: changing the password.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("form", new ChangePasswordForm());
        return "account-password";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("form") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (form.getNewPassword() != null && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Hasła nie są takie same");
        }
        if (bindingResult.hasErrors()) {
            return "account-password";
        }
        if (!userService.changePassword(authentication.getName(), form.getCurrentPassword(), form.getNewPassword())) {
            bindingResult.rejectValue("currentPassword", "wrong", "Obecne hasło jest nieprawidłowe");
            return "account-password";
        }
        redirectAttributes.addFlashAttribute("message",
                "Hasło zostało zmienione. Na innych urządzeniach zaloguj się nowym hasłem.");
        return "redirect:/";
    }
}
