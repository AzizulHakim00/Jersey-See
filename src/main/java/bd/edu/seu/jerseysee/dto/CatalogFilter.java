package bd.edu.seu.jerseysee.dto;

import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class CatalogFilter {

    private String keyword;
    private Long categoryId;
    private String category;
    private ProductType productType;
    private String clubOrCountry;
    private JerseyEdition edition;
    private KitType kitType;
    private SizeOption size;
    private Boolean available;

    @DecimalMin(value = "0.00", message = "Minimum price cannot be negative.")
    private BigDecimal minimumPrice;

    @DecimalMin(value = "0.00", message = "Maximum price cannot be negative.")
    private BigDecimal maximumPrice;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    public String getClubOrCountry() { return clubOrCountry; }
    public void setClubOrCountry(String clubOrCountry) { this.clubOrCountry = clubOrCountry; }
    public JerseyEdition getEdition() { return edition; }
    public void setEdition(JerseyEdition edition) { this.edition = edition; }
    public KitType getKitType() { return kitType; }
    public void setKitType(KitType kitType) { this.kitType = kitType; }
    public SizeOption getSize() { return size; }
    public void setSize(SizeOption size) { this.size = size; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public BigDecimal getMinimumPrice() { return minimumPrice; }
    public void setMinimumPrice(BigDecimal minimumPrice) { this.minimumPrice = minimumPrice; }
    public BigDecimal getMaximumPrice() { return maximumPrice; }
    public void setMaximumPrice(BigDecimal maximumPrice) { this.maximumPrice = maximumPrice; }
}
