package bd.edu.seu.jerseysee.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(unique = true, length = 120)
    private String demoSeedKey;

    @Column(length = 2000)
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDemoSeedKey() { return demoSeedKey; }
    public void setDemoSeedKey(String demoSeedKey) { this.demoSeedKey = demoSeedKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Product> getProducts() { return products; }
}
