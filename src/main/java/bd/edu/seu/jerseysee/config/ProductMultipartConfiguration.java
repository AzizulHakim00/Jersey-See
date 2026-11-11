package bd.edu.seu.jerseysee.config;

import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.multipart.support.MultipartFilter;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration(proxyBeanMethods = false)
class ProductMultipartConfiguration {

    @Bean
    FilterRegistrationBean<MultipartFilter> productMultipartFilter() {
        var filter = new MultipartFilter();
        filter.setMultipartResolverBeanName(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME);

        var registration = new FilterRegistrationBean<>(filter);
        registration.setUrlPatterns(List.of("/staff/products", "/staff/products/*"));
        // Make the product form's hidden CSRF field available before security checks it.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
