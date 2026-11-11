package bd.edu.seu.jerseysee.security;

import bd.edu.seu.jerseysee.model.User;
import bd.edu.seu.jerseysee.model.enums.Role;
import bd.edu.seu.jerseysee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtLoginFlowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Test void apiLoginReturnsTokenOnlyForValidCredentials() throws Exception {
        User user = new User();
        user.setName("API customer"); user.setEmail("jwt-api@example.com");
        user.setPassword(encoder.encode("Password1!")); user.setRole(Role.CUSTOMER);
        user.setEnabled(true); user.setPhone("01700000001"); user.setAddress("Dhaka");
        users.saveAndFlush(user);
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"jwt-api@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"jwt-api@example.com\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test void browserLoginUsesJwtAndApiRejectsCookieOnlyAuthentication() throws Exception {
        User user = new User();
        user.setName("JWT customer"); user.setEmail("jwt@example.com");
        user.setPassword(encoder.encode("Password1!")); user.setRole(Role.CUSTOMER);
        user.setEnabled(true); user.setPhone("01700000000"); user.setAddress("Dhaka");
        users.saveAndFlush(user);
        mvc.perform(post("/login").param("username", user.getEmail()).param("password", "Password1!"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"));
        mvc.perform(post("/access-denied").with(csrf())
                        .requestAttr(WebAttributes.ACCESS_DENIED_403, new MissingCsrfTokenException("stale")))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(model().attribute("formExpired", true));
        var response = mvc.perform(post("/login").with(csrf()).param("username", user.getEmail())
                        .param("password", "Password1!")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard")).andReturn().getResponse();
        Cookie access = response.getCookie(JwtCookieService.NAME);
        assertThat(access).isNotNull(); assertThat(access.isHttpOnly()).isTrue();
        mvc.perform(get("/dashboard").cookie(access)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access.getValue()))
                .andExpect(status().isOk());
        mvc.perform(get("/staff/employees").cookie(access)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(csrf()).cookie(access)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge(JwtCookieService.NAME, 0));
        mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login").param("logout", "")).andExpect(status().isOk());
        mvc.perform(post("/login").with(csrf()).param("username", user.getEmail())
                        .param("password", "Password1!"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(cookie().exists(JwtCookieService.NAME));
        user.setPassword(encoder.encode("NewPassword1!")); users.saveAndFlush(user);
        mvc.perform(get("/dashboard").cookie(access)).andExpect(status().is3xxRedirection());
    }
}
