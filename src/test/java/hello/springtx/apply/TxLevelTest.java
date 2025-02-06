package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 스프링에서 우선순위는 "더 구체적이고 자세한 것"이 더 높은 우선순위를 가진다.

@Slf4j
@SpringBootTest
class TxLevelTest {

    @Autowired LevelService levelService;

    @Test
    void orderTest() {
        levelService.w();
        levelService.r();
    }

    @TestConfiguration
    static class TxConfig {
        @Bean
        LevelService service() {
            return new LevelService();
        }
    }


    @Slf4j
    @Transactional(readOnly = true)
    static class LevelService {

        // 더 구체적인 메서드에 붙은 트랜잭션 어노테이션 설정이 적용된다 (readonly false)
        @Transactional(readOnly = false)
        public void w() {
            log.info("w");
            printTxInfo();
        }

        public void r() {
            log.info("r");
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
