package bd.edu.seu.jerseysee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileDTO {

    @NotBlank(message = "Name is required.")
    @Size(max = 120, message = "Name must be at most 120 characters.")
    private String name;

    @NotBlank(message = "Phone is required.")
    @Size(max = 30, message = "Phone must be at most 30 characters.")
    private String phone;

    @NotBlank(message = "Address is required.")
    @Size(max = 1000, message = "Address must be at most 1000 characters.")
    private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
