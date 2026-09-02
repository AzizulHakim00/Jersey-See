package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.Category;
import bd.edu.seu.jerseysee.model.EmployeeProfile;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AssociationFetchContractTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Test
    void activeDetailKeepsItsCategoryAvailableAfterDetach() {
        Product product = productRepository.saveAndFlush(product("Detail category"));
        entityManager.clear();

        Product detail = productRepository.findWithVariantsByIdAndActiveTrue(product.getId()).orElseThrow();
        entityManager.clear();

        assertThat(detail.getCategory().getName()).isEqualTo("Detail category");
    }

    @Test
    void managementDetailKeepsCategoryAndVariantsAvailableAfterDetach() {
        Product product = productRepository.saveAndFlush(product("Management category"));
        entityManager.clear();

        Product detail = productRepository.findWithVariantsById(product.getId()).orElseThrow();
        entityManager.clear();

        assertThat(detail.getCategory().getName()).isEqualTo("Management category");
        assertThat(detail.getVariants()).extracting(ProductVariant::getSku).containsExactly("MANAGEMENT-CATEGORY-M");
    }

    @Test
    void employeeListKeepsUserFieldsAvailableAfterDetach() {
        User user = new User();
        user.setName("Amina Rahman");
        user.setEmail("amina@example.com");
        user.setPassword("encoded");
        user.setPhone("01700000000");
        user.setAddress("Dhaka");
        user.setRole(Role.SALESMAN);
        EmployeeProfile profile = new EmployeeProfile();
        profile.setEmployeeCode("EMP-101");
        profile.setPosition("Sales associate");
        profile.setSalary(new BigDecimal("30000.00"));
        profile.setJoiningDate(LocalDate.of(2026, 1, 1));
        user.setEmployeeProfile(profile);
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        EmployeeProfile listed = employeeProfileRepository.findAllWithUser().get(0);
        entityManager.clear();

        assertThat(listed.getUser().getName()).isEqualTo("Amina Rahman");
        assertThat(listed.getUser().getEmail()).isEqualTo("amina@example.com");
    }

    private Product product(String categoryName) {
        Category category = new Category();
        category.setName(categoryName);
        entityManager.persist(category);
        Product product = new Product();
        product.setCategory(category);
        product.setName(categoryName + " jersey");
        product.setDescription("A technical test jersey.");
        product.setBrand("JerseySee");
        product.setProductType(ProductType.JERSEY);
        product.setBasePrice(new BigDecimal("80.00"));
        product.setActive(true);
        ProductVariant variant = new ProductVariant();
        variant.setSize(SizeOption.M);
        variant.setSku(categoryName.replace(' ', '-').toUpperCase() + "-M");
        variant.setStockQuantity(4);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        product.addVariant(variant);
        return product;
    }
}
