package bd.edu.seu.jerseysee.service;

import bd.edu.seu.jerseysee.config.CacheConfig;
import bd.edu.seu.jerseysee.model.Product;
import bd.edu.seu.jerseysee.model.enums.ProductType;
import bd.edu.seu.jerseysee.repository.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@SpringJUnitConfig({CacheConfig.class, CatalogCacheTest.Config.class})
class CatalogCacheTest {
    @Configuration static class Config {
        @Bean ProductRepository products() { return mock(ProductRepository.class); }
        @Bean ProductService service(ProductRepository products) {
            return new ProductService(products, mock(ProductVariantRepository.class),
                    mock(CategoryRepository.class), mock(ProductImageStorage.class));
        }
    }
    @Autowired ProductService service;
    @Autowired ProductRepository products;
    @Autowired CacheManager caches;
    @Test void cachesImmutablePublicCardsAndDefersEvictionUntilCommit() {
        Product product = new Product(); product.setName("Original shirt");
        when(products.findTop8ByActiveTrueAndFeaturedTrueAndProductTypeOrderByIdDesc(ProductType.JERSEY))
                .thenReturn(List.of(product));
        var first = service.featuredCards();
        product.setName("Changed shirt");
        assertThat(service.featuredCards().get(0).name()).isEqualTo("Original shirt");
        verify(products, times(1)).findTop8ByActiveTrueAndFeaturedTrueAndProductTypeOrderByIdDesc(ProductType.JERSEY);
        assertThatThrownBy(() -> first.clear()).isInstanceOf(UnsupportedOperationException.class);
        TransactionSynchronizationManager.initSynchronization();
        try {
            caches.getCache("featuredCards").clear();
            assertThat(service.featuredCards().get(0).name()).isEqualTo("Original shirt");
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
        assertThat(service.featuredCards().get(0).name()).isEqualTo("Changed shirt");
        verify(products, times(2)).findTop8ByActiveTrueAndFeaturedTrueAndProductTypeOrderByIdDesc(ProductType.JERSEY);
    }
}
