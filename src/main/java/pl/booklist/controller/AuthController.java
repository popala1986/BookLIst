package pl.booklist.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.booklist.dto.RegisterForm;
import pl.booklist.exception.UsernameTakenException;
import pl.booklist.service.UserService;

/**
 * Login and registration pages.
 */
@Controller
public class AuthController {

    private final UserService userService;
    private final boolean registrationEnabled;

    public AuthController(UserService userService,
                          @Value("${app.registration.enabled:true}") boolean registrationEnabled) {
        this.userService = userService;
        this.registrationEnabled = registrationEnabled;
    }

    @ModelAttribute("registrationEnabled")
    public boolean registrationEnabled() {
        return registrationEnabled;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return isLoggedIn(authentication) ? "redirect:/" : "login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isLoggedIn(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (!registrationEnabled) {
            return "register";
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Hasła nie są takie same");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form.getUsername(), form.getPassword());
        } catch (UsernameTakenException e) {
            bindingResult.rejectValue("username", "taken", "Ta nazwa użytkownika jest już zajęta");
            return "register";
        }
        redirectAttributes.addFlashAttribute("message", "Konto zostało utworzone. Możesz się zalogować.");
        return "redirect:/login";
    }

    private static boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
