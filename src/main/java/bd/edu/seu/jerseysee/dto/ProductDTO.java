package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductDTO {

    @NotNull(message = "Select a category.")
    private Long categoryId;

    @NotBlank(message = "Product name is required.")
    @Size(max = 255, message = "Product name must be at most 255 characters.")
    private String name;

    @NotBlank(message = "Description is required.")
    @Size(max = 4000, message = "Description must be at most 4000 characters.")
    private String description;

    @NotBlank(message = "Brand is required.")
    @Size(max = 255, message = "Brand must be at most 255 characters.")
    private String brand;

    @NotNull(message = "Select a product type.")
    private ProductType productType;

    @Size(max = 255, message = "Club or country must be at most 255 characters.")
    private String clubOrCountry;

    @Size(max = 100, message = "Season must be at most 100 characters.")
    private String season;

    private KitType kitType;
    private JerseyEdition jerseyEdition;
    private SleeveType sleeveType;

    @NotNull(message = "Base price is required.")
    @DecimalMin(value = "0.00", message = "Base price cannot be negative.")
    private BigDecimal basePrice;

    private boolean featured;
    private boolean active = true;

    @Valid
    private List<ProductVariantDTO> variants = new ArrayList<>();

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public List<ProductVariantDTO> getVariants() { return variants; }
    public void setVariants(List<ProductVariantDTO> variants) {
        this.variants = variants == null ? new ArrayList<>() : new ArrayList<>(variants);
    }
}
