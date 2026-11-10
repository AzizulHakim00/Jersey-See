package bd.edu.seu.jerseysee.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    CacheManager cacheManager() {
        var cache = new CaffeineCacheManager("featuredCards");
        cache.setCaffeine(Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(3)));
        cache.setAllowNullValues(false);
        return new TransactionAwareCacheManagerProxy(cache);
    }
}
