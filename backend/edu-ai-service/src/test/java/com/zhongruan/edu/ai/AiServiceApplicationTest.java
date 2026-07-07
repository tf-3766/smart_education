package com.zhongruan.edu.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "edu.ai.jwt.secret=test-only-jwt-secret-with-at-least-32-bytes",
        "edu.ai.jwt.ttl=PT15M",
        "edu.ai.jwt.issuer=edu-biz-service-test",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureWebTestClient
class AiServiceApplicationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealthIsAvailableWithoutAiProviderConfiguration() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
