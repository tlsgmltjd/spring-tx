package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
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

    // 스프링에서 트랜잭션 실행 중에 내부에서 트랜잭션을 또 시작한다면, 둘을 묶어서 하나의 트랜잭션으로 만든다.
    // 각각의 트랜잭션을 논리 트랜잭션, 둘을 묶은 물리 트랜잭션.
    // 물리 트랜잭션은 실제 디비에 적용되는 트랜잭션이며, 논리 트랜잭션은 트랜잭션 매니저를 통해 사용되는 단위이다.
    // 구분하는 이유 -> 트랜잭션 내부에서 트랜잭션을 또 사용하면 여러 복잡한 케이스가 생겨서 단순한 원칙을 만들 수 있다.
    // 원칙: 모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋된다, 논리 트랜잭션 중 하나라도 롤백시 물리 트랜잭션은 롤백된다.

    @Test
    void inner_commit() {
        log.info("-- 외부 트랜잭션 시작");
        // to manual commit -> set auto commit false -> 트랜잭션 시작
        TransactionStatus outer = txm.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());
        // outer.isNewTransaction()=true

        // 내부 트랜잭션은 외부 트랜잭션에 참여한다. -> "내부 트랜잭션이 외부 트랜잭션을 그대로 이어받는다."
        // -> 외부와 내부 트랜잭션이 하나의 물리 트랜잭션으로 묶인다.
        innerLogic();

        // 2025-04-24T14:09:11.268+09:00  INFO 59560 --- [    Test worker] hello.springtx.propagation.BasicTxTest   : 내부 트랜잭션 커밋
        // 2025-04-24T14:09:11.268+09:00  INFO 59560 --- [    Test worker] hello.springtx.propagation.BasicTxTest   : 외부 트랜잭션 커밋
        // 내부 트랜잭션은 외부 트랜잭션에 참여했기 때문에 커밋해도 무시된다. 물리 트랜잭션에 내부 트랜잭션이 커밋 해버리면 큰일난다.
        // -> 여러 트랜잭션이 함께 사용되는 경우 외부 트랜잭션이 실제 물리 트랜잭션을 관리하기 된다.
        log.info("외부 트랜잭션 커밋");
        txm.commit(outer);
    }

    private void innerLogic() {
        log.info("-- 내부 트랜잭션 시작");
        // Participating in existing transaction
        TransactionStatus inner = txm.getTransaction(new DefaultTransactionDefinition());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        // inner.isNewTransaction()=false

        log.info("내부 트랜잭션 커밋");
        txm.commit(inner);
    }

    @Test
    void outer_rollback() {
        log.info("-- 외부 트랜잭션 시작");
        TransactionStatus outer = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- 내부 트랜잭션 시작");
        TransactionStatus inner = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("내부 트랜잭션 커밋");
        txm.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txm.rollback(outer);
    }

    @Test
    void inner_rollback() {
        log.info("-- 외부 트랜잭션 시작");
        TransactionStatus outer = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("-- 내부 트랜잭션 시작");
        TransactionStatus inner = txm.getTransaction(new DefaultTransactionDefinition());

        log.info("내부 트랜잭션 롤백");
        txm.rollback(inner);
        // Participating transaction failed - marking existing transaction as rollback-only
        // -> 내부 트랜잭션 롤백시, "물리 트랜잭션 롤백을 호출하지 않고(물리 트랜잭션은 트랜잭션 끝날 때까지 유지되어야함)"
        // 기존 트랜잭션을 롤백전용(rollback-only)으로 마크한다.

        log.info("외부 트랜잭션 커밋");
        Assertions.assertThatThrownBy(() -> txm.commit(outer)).isInstanceOf(UnexpectedRollbackException.class);
        // Global transaction is marked as rollback-only but transactional code requested commit
        // Rolling back JDBC transaction on Connection
        // -> 외부 트랜잭션에서 트랜잭션 동기화 매니저에서 롤백전용이 마크되어있는지 확인한다. 마크되어있다면 물리트랜잭션을 롤백시킨다.
        // -> 내부 롤백, 외부 커밋시, 즉 커밋 호출했는데 내부에서 롤백되어 실제로 물리적 롤백 호출시 --> UnexpectedRollbackException 예외를 트랜잭션 매니저가 던진다.
    }

    @Test
    void inner_rollback_requires_new() {
        log.info("-- 외부 트랜잭션 시작");
        TransactionStatus outer = txm.getTransaction(new DefaultTransactionDefinition());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("-- 내부 트랜잭션 시작");

        // REQUIRES_NEW -> 외부, 내부 트랜잭션을 별도의 물리 트랜잭션을 갖게 하는 옵션, 각각의 디비 커넥션을 갖고, 서로 영향을 주지 않는다.
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // Suspending current transaction, creating new transaction with name [null] <- 외부 트랜잭션을 잠시 미룬다.
        // Acquired Connection [HikariProxyConnection@1842475577 ...] -> 새로운 커넥션을 획득한다.

        TransactionStatus inner = txm.getTransaction(definition);
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());

        log.info("내부 트랜잭션 롤백");
        txm.rollback(inner);

        log.info("외부 트랜잭션 커밋");
        txm.commit(outer);
    }

}
