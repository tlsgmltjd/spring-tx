package hello.springtx.apply;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InitTxTest {

    @Autowired
    Hello hello;

    @Test
    void go() {
        // 초기화 코드는 스프링이 빈 초기화 시점에 자동으로 call
    }

    @TestConfiguration
    static class TestContextConfiguration {
        @Bean
        Hello hello() {
            return new Hello();
        }
    }

    static class Hello {
        @PostConstruct
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("isActive: {}", isActive);

            // INFO 5931 --- [    Test worker] hello.springtx.apply.InitTxTest          : isActive: false
            // 초기화 시점에는 트랜잭션 적용이 안됨
            // -> 초기화 코드 호출 이후 트랜잭션 AOP가 적용되기 때문에 초기화 시점에는 트랜잭션 획득이 불가능하다.

        }

        // 스프링 빈 라이프사이클
        // 스프링 컨테이너 생성 -> 스프링 빈 생성 -> 의존관계 주입 ->
        // 초기화 콜백 실행 (@PostConstruct) -> 빈 후처리기 동작하여 프록시 객체 생성 -> 프록시 객체 등의 후처리 완료 후 빈 등록 ->
        // 스프링 애플리케이션 컨텍스트 초기화 완료 (ApplicationReadyEvent) 및 애플리케이션 실행 ->
        // 빈 사용 -> 소멸 전 콜백 -> 스프링 종료

        @EventListener(ApplicationReadyEvent.class)
        @Transactional
        public void initV2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("ApplicationReadyEvent isActive: {}", isActive);

            // INFO 7473 --- [    Test worker] hello.springtx.apply.InitTxTest          : ApplicationReadyEvent isActive: true
        }
    }

}
