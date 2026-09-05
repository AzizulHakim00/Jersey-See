package bd.edu.seu.jerseysee.controller;

import bd.edu.seu.jerseysee.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicDemoLoginControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerDemoLoginCreatesAuthenticatedSessionWithoutSendingAPasswordToTheBrowser() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        UserDetails customer = User.withUsername("customer@jerseysee.demo")
                .password("server-only")
                .roles("CUSTOMER")
                .build();
        when(userDetailsService.loadUserByUsername("customer@jerseysee.demo")).thenReturn(customer);

        PublicDemoLoginController controller = new PublicDemoLoginController(userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();

        String view = controller.login("customer", request);

        assertThat(view).isEqualTo("redirect:/dashboard");
        Object stored = request.getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(stored).isInstanceOf(SecurityContext.class);
        SecurityContext context = (SecurityContext) stored;
        assertThat(context.getAuthentication().isAuthenticated()).isTrue();
        assertThat(context.getAuthentication().getName()).isEqualTo("customer@jerseysee.demo");
        verify(userDetailsService).loadUserByUsername("customer@jerseysee.demo");
    }

    @Test
    void unknownDemoAccountIsRejected() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        PublicDemoLoginController controller = new PublicDemoLoginController(userDetailsService);

        assertThatThrownBy(() -> controller.login("unknown", new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(404));
    }
}
