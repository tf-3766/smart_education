package com.zhongruan.edu.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.zhongruan.edu.ai.generation.AiTextGenerator;
import com.zhongruan.edu.feign.ai.BizAiContextFeignClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "edu.ai.jwt.secret=test-only-jwt-secret-with-at-least-32-bytes",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false",
    "spring.ai.model.chat=openai",
    "spring.ai.openai.api-key=test-dashscope-key",
    "spring.ai.openai.base-url=http://127.0.0.1:9/compatible-mode",
    "spring.ai.openai.chat.options.model=qwen-plus",
    "edu.ai.provider-name=aliyun-bailian"
})
class BailianConfigurationTest {
    @Autowired
    private AiTextGenerator generator;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private BizAiContextFeignClient contextClient;

    @Test
    void openAiCompatibleConfigurationCreatesBailianGenerator() {
        assertThat(applicationContext.getBeansOfType(ChatModel.class))
                .as("Spring AI should auto-configure a ChatModel")
                .isNotEmpty();
        assertThat(generator.configured()).isTrue();
        assertThat(generator.provider()).isEqualTo("aliyun-bailian");
        assertThat(generator.model()).isEqualTo("qwen-plus");
    }
}
