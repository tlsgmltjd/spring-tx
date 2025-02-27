package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService rollbackService;

    @Test
    void runtimeException() {
        // o.s.orm.jpa.JpaTransactionManager: Creating new transaction with name [hello.springtx.exception.RollbackTest$RollbackService.runtimeException]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
        // ...
        // o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
        // -> tx rollback
        Assertions.assertThrows(RuntimeException.class, () -> rollbackService.runtimeException());
    }

    @Test
    void checkEx() {
        // o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
        // -> rx commit
        Assertions.assertThrows(MyException.class, () -> rollbackService.checkedException());
    }

    @Test
    void rollbackFor() {
        // o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
        // -> rx rollback
        Assertions.assertThrows(MyException.class, () -> rollbackService.rollbackFor());
    }

    @TestConfiguration
    static class RollbackTestConfiguration {
        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    @Slf4j
    static class RollbackService {

        // 런타입 예외: 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        // 체크 예외: 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        // 체크 예외 rollbackFor 지정: 롤백
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call checkedException rollbackFor");
            throw new MyException();
        }

    }

    static class MyException extends Exception {}

}
