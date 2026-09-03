package bd.edu.seu.jerseysee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @Column(name = "stored_name", nullable = false, length = 100)
    private String storedName;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;

    protected ProductImage() {
    }

    public ProductImage(String storedName, byte[] content) {
        this.storedName = storedName;
        this.content = content == null ? null : content.clone();
    }

    public String getStoredName() {
        return storedName;
    }

    public byte[] getContent() {
        return content == null ? null : content.clone();
    }
}
