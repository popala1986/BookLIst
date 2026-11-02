package pl.booklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data from the "Zmień hasło" form.
 */
public class ChangePasswordForm {

    @NotBlank(message = "Podaj obecne hasło")
    private String currentPassword;

    @NotBlank(message = "Podaj nowe hasło")
    @Size(min = 8, max = 100, message = "Hasło musi mieć co najmniej 8 znaków")
    private String newPassword;

    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
