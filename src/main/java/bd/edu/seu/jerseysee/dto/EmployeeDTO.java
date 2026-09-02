package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.Role;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeDTO extends RegistrationDTO {

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
}
