package bd.edu.seu.jerseysee.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.main.lazy-initialization=true")
class CriticalAuthenticationInitializationTest {

    private final ConfigurableApplicationContext context;

    CriticalAuthenticationInitializationTest(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Test
    void criticalAuthenticationBeansRemainEagerWhenGlobalLazyInitializationIsEnabled() {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        assertThat(beanFactory.getBeanDefinition("securityFilterChain").isLazyInit())
                .as("POST /login must be handled by Spring Security on the first request")
                .isFalse();
        assertThat(beanFactory.getBeanDefinition("customUserDetailsService").isLazyInit())
                .as("the user-details lookup must be ready for the first authentication attempt")
                .isFalse();
    }
}
