package bd.edu.seu.jerseysee.repository;

import bd.edu.seu.jerseysee.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerOrderRepositoryContractTest {

    @Test
    void mutationLockQueryLocksOnlyTheRootOrderWithoutCollectionGraphOrDistinct() throws Exception {
        Method method = CustomerOrderRepository.class.getMethod("findByIdForUpdate", Long.class);

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(method.getAnnotation(Query.class).value())
                .isEqualTo("select customerOrder from CustomerOrder customerOrder where customerOrder.id = :id");
        assertThat(method.getAnnotation(EntityGraph.class)).isNull();
    }
}
