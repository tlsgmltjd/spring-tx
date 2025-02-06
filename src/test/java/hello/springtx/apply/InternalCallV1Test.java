package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 트랜잭션 AOP 주의 사항 - 프록시 내부 호출
// *: 트랜잭션 프록시 부가기능이 적용된 메서드

// client -> ProxyService(E(), *I()) - 실제 E() 호출 -> TargetService(E(), I())
// TargetService E()는 this.I()를 호출하기 때문에 트랜잭션이 적용되지 않는다. 즉 target 객체의 내부 메서드를 직접 호출하는 것이다.

@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired CallService callService;

    @Test
    void printProxy() {
        log.info("calService class = {}", callService.getClass());
    }

    @Test
    void externalCall() {
        callService.external();
    }

    @Test
    void internalCall() {
        callService.internal();
    }

    @TestConfiguration
    static class TestContextConfiguration {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {
        public void external() {
            log.info("call external method");
            internal(); // internal method가 트랜잭션이 적용되어 있어도 내부적 메서드 호출을 해버리면 트랜잭션 적용이 안되버린다.
            printTxInfo();
        }

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
