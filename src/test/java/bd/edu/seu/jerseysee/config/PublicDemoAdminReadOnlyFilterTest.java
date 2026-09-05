package bd.edu.seu.jerseysee.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDemoAdminReadOnlyFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicDemoAdminCanReadStaffPages() throws Exception {
        authenticate("admin@jerseysee.demo");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/staff/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new PublicDemoAdminReadOnlyFilter().doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void publicDemoAdminCannotMutateStaffPages() throws Exception {
        authenticate("admin@jerseysee.demo");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/staff/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new PublicDemoAdminReadOnlyFilter().doFilter(request, response, chain);

        assertThat(chain.called).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void privateAdministratorIsNotRestricted() throws Exception {
        authenticate("owner@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/staff/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingChain chain = new RecordingChain();

        new PublicDemoAdminReadOnlyFilter().doFilter(request, response, chain);

        assertThat(chain.called).isTrue();
    }

    private void authenticate(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email, "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static class RecordingChain implements FilterChain {
        private boolean called;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            called = true;
        }
    }
}
