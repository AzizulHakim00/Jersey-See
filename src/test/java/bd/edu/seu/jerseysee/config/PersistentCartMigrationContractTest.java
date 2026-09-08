package bd.edu.seu.jerseysee.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentCartMigrationContractTest {

    @Test
    void productionUsesFlywayToCreateOnlyThePersistentCustomerCartSchema() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        String defaults = Files.readString(Path.of("src/main/resources/application.properties"));
        String production = Files.readString(Path.of("src/main/resources/application-production.properties"));
        Path migrationPath = Path.of("src/main/resources/db/migration/V2__create_customer_cart_item.sql");

        assertThat(pom).contains("flyway-core", "flyway-mysql");
        assertThat(defaults).contains("spring.flyway.enabled=false");
        assertThat(production).contains(
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=true",
                "spring.flyway.baseline-version=1",
                "app.public-demo.enabled=${JERSEYSEE_PUBLIC_DEMO_ENABLED:false}");

        assertThat(migrationPath).exists();
        String migration = Files.readString(migrationPath);
        assertThat(migration).containsIgnoringCase(
                "create table customer_cart_item",
                "line_id",
                "customer_id",
                "product_variant_id",
                "quantity",
                "printing_type",
                "printing_name",
                "printing_number",
                "unique",
                "foreign key",
                "references users",
                "references product_variant",
                "on delete cascade",
                "index");
        assertThat(migration)
                .doesNotContainIgnoringCase("drop table", "truncate", "delete from users", "delete from product");
    }
}
