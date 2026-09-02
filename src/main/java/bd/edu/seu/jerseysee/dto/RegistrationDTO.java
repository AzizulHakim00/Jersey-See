package bd.edu.seu.jerseysee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationDTO {

    @NotBlank(message = "Name is required.")
    @Size(max = 120, message = "Name must be at most 120 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Email must be at most 255 characters.")
    private String email;

    @NotBlank(message = "Phone is required.")
    @Size(max = 30, message = "Phone must be at most 30 characters.")
    private String phone;

    @NotBlank(message = "Address is required.")
    @Size(max = 1000, message = "Address must be at most 1000 characters.")
    private String address;

    @NotBlank(message = "Password is required.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
            message = "Password must contain upper- and lower-case letters, a number, and a symbol.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String passwordConfirmation;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) { this.passwordConfirmation = passwordConfirmation; }
}
