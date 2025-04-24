package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txm;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager txManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("-- begin tx");
        TransactionStatus status = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- commit");
        txm.commit(status);
    }

    @Test
    void rollback() {
        log.info("-- begin tx");
        TransactionStatus status = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- rollback");
        txm.rollback(status);
    }

    @Test
    void double_commit() {
        // 히카리 커넥션 풀에서 반환해주는 커넥션 객체는 물리 커넥션을 감싸서 반환하기 때문에,
        // 다른 물리적 커넥션이라도 커넥션 풀에 반환된 객체를 다시 할당받을 수도 있다.

        log.info("-- begin tx1");
        TransactionStatus status1 = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- commit tx1");
        txm.commit(status1);

        log.info("-- begin tx2");
        TransactionStatus status2 = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- commit tx2");
        txm.commit(status2);
    }

    @Test
    void double_commit_rollback() {
        // 1번과 2번 트랜잭션은 각각의 커넥션을 갖는다.
        // 서로의 트랜잭션에 간섭하지 않고 독립적으로 동작한다.

        log.info("-- begin tx1");
        TransactionStatus status1 = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- commit tx1");
        txm.commit(status1);

        log.info("-- begin tx2");
        TransactionStatus status2 = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- rollback tx2");
        txm.rollback(status2);
    }

}
