package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
