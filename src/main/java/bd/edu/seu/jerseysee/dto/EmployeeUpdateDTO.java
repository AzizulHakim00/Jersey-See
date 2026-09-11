package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeUpdateDTO {

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

    @NotNull(message = "Select a staff role.")
    private Role role;

    @NotBlank(message = "Employee code is required.")
    @Size(max = 50, message = "Employee code must be at most 50 characters.")
    private String employeeCode;

    @NotBlank(message = "Position is required.")
    @Size(max = 120, message = "Position must be at most 120 characters.")
    private String position;

    @NotNull(message = "Salary is required.")
    @DecimalMin(value = "0.00", message = "Salary cannot be negative.")
    private BigDecimal salary;

    @NotNull(message = "Joining date is required.")
    @PastOrPresent(message = "Joining date cannot be in the future.")
    private LocalDate joiningDate;

    @Pattern(regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
            message = "Password must contain upper- and lower-case letters, a number, and a symbol.")
    private String password = "";

    private String passwordConfirmation = "";

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password == null ? "" : password; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation == null ? "" : passwordConfirmation;
    }
}
