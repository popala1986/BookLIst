package pl.booklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data for a new account: from the administrator's "Dodaj użytkownika" form
 * or from the registration form (when registration is on).
 */
public class RegisterForm {

    @NotBlank(message = "Podaj nazwę użytkownika")
    @Size(min = 3, max = 50, message = "Nazwa użytkownika musi mieć od 3 do 50 znaków")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Dozwolone są litery, cyfry oraz . _ -")
    private String username;

    @NotBlank(message = "Podaj hasło")
    @Size(min = 8, max = 100, message = "Hasło musi mieć co najmniej 8 znaków")
    private String password;

    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
