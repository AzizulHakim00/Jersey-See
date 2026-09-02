package bd.edu.seu.jerseysee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class EmployeeProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getEmployeeProfile() != this) {
            user.setEmployeeProfile(this);
        }
    }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
