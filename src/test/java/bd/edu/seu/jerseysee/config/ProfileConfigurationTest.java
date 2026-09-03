package bd.edu.seu.jerseysee.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.demo-data.enabled=false")
class ProfileConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void automatedTestsOpenAnIsolatedH2DatabaseInsteadOfProductionMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:jerseysee");
            assertThat(connection.getMetaData().getUserName()).isEqualToIgnoringCase("SA");
        }
    }
}
