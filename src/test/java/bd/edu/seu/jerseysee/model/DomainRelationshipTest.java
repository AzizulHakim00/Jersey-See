package bd.edu.seu.jerseysee.model;

import bd.edu.seu.jerseysee.model.enums.JerseyEdition;
import bd.edu.seu.jerseysee.model.enums.KitType;
import bd.edu.seu.jerseysee.model.enums.OrderStatus;
import bd.edu.seu.jerseysee.model.enums.PaymentMethod;
import bd.edu.seu.jerseysee.model.enums.PaymentStatus;
import bd.edu.seu.jerseysee.model.enums.PrintingType;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.model.enums.SizeOption;
import bd.edu.seu.jerseysee.model.enums.SleeveType;
import bd.edu.seu.jerseysee.repository.CategoryRepository;
import bd.edu.seu.jerseysee.repository.CustomerOrderRepository;
import bd.edu.seu.jerseysee.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DomainRelationshipTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsCoreRelationshipsAndReloadsStableOrderSnapshots() {
        User customer = new User();
        customer.setName("Amina Rahman");
        customer.setEmail("amina@example.com");
        customer.setPassword("encoded-password");
        customer.setPhone("01700000000");
        customer.setAddress("Dhaka");
        customer.setRole(Role.CUSTOMER);
        customer.setEnabled(true);
        customer = userRepository.saveAndFlush(customer);

        User salesman = new User();
        salesman.setName("Rahim Uddin");
        salesman.setEmail("rahim@example.com");
        salesman.setPassword("encoded-password");
        salesman.setPhone("01800000000");
        salesman.setAddress("Dhaka");
        salesman.setRole(Role.SALESMAN);

        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setEmployeeCode("SEU-001");
        employeeProfile.setPosition("Salesman");
        employeeProfile.setSalary(new BigDecimal("25000.00"));
        employeeProfile.setJoiningDate(LocalDate.of(2026, 1, 1));
        salesman.setEmployeeProfile(employeeProfile);
        salesman = userRepository.saveAndFlush(salesman);

        Category category = new Category();
        category.setName("Club Jerseys");
        category.setDescription("Official club shirts");

        Product product = new Product();
        product.setName("Barcelona Home 2026/27");
        product.setDescription("Home jersey");
        product.setBrand("Nike");
        product.setProductType(ProductType.JERSEY);
        product.setClubOrCountry("Barcelona");
        product.setSeason("2026/27");
        product.setKitType(KitType.HOME);
        product.setJerseyEdition(JerseyEdition.FAN);
        product.setSleeveType(SleeveType.SHORT);
        product.setBasePrice(new BigDecimal("5500.00"));
        product.setFeatured(true);
        product.setActive(true);
        category.addProduct(product);

        ProductVariant variant = new ProductVariant();
        variant.setSize(SizeOption.L);
        variant.setSku("BAR-H-2627-L");
        variant.setStockQuantity(12);
        variant.setPriceAdjustment(new BigDecimal("250.00"));
        product.addVariant(variant);
        category = categoryRepository.saveAndFlush(category);

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setDeliveryRecipientName("Amina Rahman");
        order.setDeliveryPhone("01700000000");
        order.setDeliveryAddress("Dhaka");
        order.setSubtotal(new BigDecimal("6000.00"));
        order.setDeliveryFee(new BigDecimal("100.00"));
        order.setTotal(new BigDecimal("6100.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 1, 10, 0));

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setProductName("Barcelona Home 2026/27");
        item.setSku("BAR-H-2627-L");
        item.setSize(SizeOption.L);
        item.setUnitPrice(new BigDecimal("5750.00"));
        item.setPrintingType(PrintingType.PLAYER);
        item.setCustomName("PEDRI");
        item.setCustomNumber("8");
        item.setPrintingCharge(new BigDecimal("250.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("6000.00"));
        order.addItem(item);

        Payment payment = new Payment();
        payment.setMethod(PaymentMethod.BKASH);
        payment.setAmount(new BigDecimal("6100.00"));
        payment.setStatus(PaymentStatus.PENDING);
        order.setPayment(payment);
        CustomerOrder savedOrder = customerOrderRepository.saveAndFlush(order);

        Long variantId = category.getProducts().get(0).getVariants().get(0).getId();
        Long orderId = savedOrder.getId();
        Long salesmanId = salesman.getId();
        entityManager.clear();

        ProductVariant reloadedVariant = categoryRepository.findById(category.getId()).orElseThrow()
                .getProducts().get(0).getVariants().get(0);
        OrderItem reloadedItem = customerOrderRepository.findById(orderId).orElseThrow()
                .getItems().get(0);
        User reloadedSalesman = userRepository.findById(salesmanId).orElseThrow();

        assertThat(reloadedSalesman.getEmployeeProfile().getEmployeeCode()).isEqualTo("SEU-001");
        assertThat(reloadedVariant.getId()).isEqualTo(variantId);
        assertThat(reloadedVariant.getSku()).isEqualTo("BAR-H-2627-L");
        assertThat(reloadedVariant.getPriceAdjustment()).isEqualByComparingTo("250.00");
        assertThat(reloadedItem.getProductVariant().getId()).isEqualTo(variantId);
        assertThat(reloadedItem.getProductName()).isEqualTo("Barcelona Home 2026/27");
        assertThat(reloadedItem.getSku()).isEqualTo("BAR-H-2627-L");
        assertThat(reloadedItem.getUnitPrice()).isEqualByComparingTo("5750.00");
        assertThat(reloadedItem.getPrintingCharge()).isEqualByComparingTo("250.00");
        assertThat(reloadedItem.getSubtotal()).isEqualByComparingTo("6000.00");
    }
}
