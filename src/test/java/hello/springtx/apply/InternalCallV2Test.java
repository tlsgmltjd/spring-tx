package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 메서드 내부 호출 시 트랜잭션 적용 안되는 문제를 클래스를 분리하여 해결
// + 스프링은 6버전 이전에는 public method 에만 선언적 트랜잭션을 허용했다. -> protected나 package-private 메서드에 트랜잭션을 의도적으로 적용하는 일은 드물기 때문에 스프링이 막아둠
// -> 스프링 6버전 이후부터는 public, protected, package-private 메서드 모두 선언적 트랜잭션 적용을 허용한다.

@Slf4j
@SpringBootTest
public class InternalCallV2Test {

    @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("calService class = {}", callService.getClass());
    }

    @Test
    void externalCallV2() {
        callService.external();
    }

    @TestConfiguration
    static class TestContextConfiguration {
        @Bean
        CallService callService() {
            return new CallService(internalService());
        }

        @Bean
        InternalService internalService() {
            return new InternalService();
        }
    }

    @Slf4j
    static class CallService {

        private final InternalService internalService;

        public CallService(InternalService internalService) {
            this.internalService = internalService;
        }

        public void external() {
            log.info("call external method");
            printTxInfo();
            internalService.internal();
        }

        private void printTxInfo() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isActive: {}", isActive);
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isReadOnly: {}", isReadOnly);
        }
    }

    @Slf4j
    static class InternalService {
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isActive: {}", isActive);
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("isReadOnly: {}", isReadOnly);
        }
    }

}
