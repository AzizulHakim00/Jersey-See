package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.model.ProductVariant;
import bd.edu.seu.jerseysee.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jerseysee-storefront-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StorefrontSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Test
    void homePageRendersFeaturedProductsLoadedFromJpa() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Metro City Home Fan Jersey")));
    }

    @Test
    void administratorCanLogInAndOpenAdminDashboard() throws Exception {
        MockHttpSession session = login("admin@demo.local", "ADMIN");

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin dashboard")));
    }

    @Test
    void customerCanLogInSeeCatalogAndAddStockedVariantToCart() throws Exception {
        ProductVariant stocked = variantRepository.findAll().stream()
                .filter(variant -> variant.getStockQuantity() > 0 && variant.getProduct().isActive())
                .findFirst()
                .orElseThrow();
        MockHttpSession session = login("customer@demo.local", "CUSTOMER");

        mockMvc.perform(get("/catalog").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(stocked.getProduct().getName())));

        mockMvc.perform(post("/cart/items").session(session).with(csrf())
                        .param("variantId", stocked.getId().toString())
                        .param("quantity", "1")
                        .param("printingType", "NONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart?added"));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(stocked.getProduct().getName())));
    }

    @Test
    void customerSessionCannotOpenStaffProductManagement() throws Exception {
        MockHttpSession session = login("customer@demo.local", "CUSTOMER");

        mockMvc.perform(get("/staff/products").session(session))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession login(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(email).password("Demo123!"))
                .andExpect(authenticated().withRoles(role))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
