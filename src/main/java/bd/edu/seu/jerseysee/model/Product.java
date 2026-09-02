package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    /** Internal nullable key used only to identify deterministic demo seed rows. */
    @Column(unique = true, length = 120)
    private String demoSeedKey;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    private String clubOrCountry;
    private String season;

    @Enumerated(EnumType.STRING)
    private KitType kitType;

    @Enumerated(EnumType.STRING)
    private JerseyEdition jerseyEdition;

    @Enumerated(EnumType.STRING)
    private SleeveType sleeveType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;

    private String storedImageName;
    private String originalImageName;
    private String imageContentType;
    private Long imageSize;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDemoSeedKey() { return demoSeedKey; }
    public void setDemoSeedKey(String demoSeedKey) { this.demoSeedKey = demoSeedKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    public String getClubOrCountry() { return clubOrCountry; }
    public void setClubOrCountry(String clubOrCountry) { this.clubOrCountry = clubOrCountry; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public KitType getKitType() { return kitType; }
    public void setKitType(KitType kitType) { this.kitType = kitType; }
    public JerseyEdition getJerseyEdition() { return jerseyEdition; }
    public void setJerseyEdition(JerseyEdition jerseyEdition) { this.jerseyEdition = jerseyEdition; }
    public SleeveType getSleeveType() { return sleeveType; }
    public void setSleeveType(SleeveType sleeveType) { this.sleeveType = sleeveType; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getStoredImageName() { return storedImageName; }
    public void setStoredImageName(String storedImageName) { this.storedImageName = storedImageName; }
    public String getOriginalImageName() { return originalImageName; }
    public void setOriginalImageName(String originalImageName) { this.originalImageName = originalImageName; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public Long getImageSize() { return imageSize; }
    public void setImageSize(Long imageSize) { this.imageSize = imageSize; }
    public List<ProductVariant> getVariants() { return variants; }
}
