package kr.co.stageon.queue.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueueTokenServiceTest {

    private final QueueTokenService tokenService = new QueueTokenService();

    @Test
    void issuedTokensAreUniqueAndHashTo64HexCharacters() {
        String first = tokenService.issue();
        String second = tokenService.issue();

        assertThat(first).isNotEqualTo(second);
        assertThat(tokenService.hash(first)).matches("[0-9a-f]{64}");
        assertThat(tokenService.hash(first)).isEqualTo(tokenService.hash(first));
    }

    @Test
    void missingTokenHashesToEmptyValue() {
        assertThat(tokenService.hash(null)).isEmpty();
        assertThat(tokenService.hash("  ")).isEmpty();
    }
}
