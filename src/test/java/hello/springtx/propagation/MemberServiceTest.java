package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
class MemberServiceTest {
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired LogRepository logRepository;

    /**
     * memberService    @Tx:OFF
     * memberRepository @Tx:ON
     * logRepository    @Tx:ON
     */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        // when -> 모든 데이터가 정상 저장된다.
        memberService.joinV1(username);

        // then
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Tx:OFF
     * memberRepository @Tx:ON
     * logRepository    @Tx:ON RuntimeException
     */
    @Test
    void outerTxOff_fail() {
        // given
        String username = "로그예외_outerTxOff_fail";

        // when -> Member 저장은 커밋, Log 저장은 롤백
        assertThatThrownBy(() -> memberService.joinV1(username));

        // then
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService    @Tx:ON
     * memberRepository @Tx:OFF
     * logRepository    @Tx:OFF
     */
    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        // when -> 모든 데이터가 정상 저장된다.
        memberService.joinV1(username);

        // then
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Tx:ON
     * memberRepository @Tx:ON
     * logRepository    @Tx:ON
     */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        // when -> memberService 에서 트랜잭션 시작 ->> memberRepo, logRepo 에서 시작한 트랜잭션들은 기존 트랜잭션을 이어받아 실행, 각각 Repo Tx는 커밋하지 않고 넘김.
        memberService.joinV1(username);

        // then
        Assertions.assertTrue(memberRepository.find(username).isPresent());
        Assertions.assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService    @Tx:ON
     * memberRepository @Tx:ON
     * logRepository    @Tx:ON RuntimeException
     */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        // when -> memberService 신규 트랜잭션 생성 -> memberRepo 커밋(물리 커밋 호출 X) -> logRepo 롤백(롤백온리 마킹) -> memberService 물리 롤백
        assertThatThrownBy(() -> memberService.joinV1(username));

        // then
        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService    @Tx:ON
     * memberRepository @Tx:ON
     * logRepository    @Tx:ON RuntimeException
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        // when -> logRepo Tx에서 예외 발생하여 롤백(물리 트랜잭션에 롤백온리 마킹) -> 하지만, 맴버 서비스에서는 해당 예외를 잡아서 먹어버림.
        // -> 물리 커밋하려하지만 롤백 온리가 마킹되어있어 물리 트랜잭션을 롤백함. -> UnexpectedRollbackException
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        // then
        Assertions.assertTrue(memberRepository.find(username).isEmpty());
        Assertions.assertTrue(logRepository.find(username).isEmpty());
    }
}
