package com.zhongruan.edu.ai.generation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class AiGenerationConfig {
    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    AiTextGenerator springAiTextGenerator(
            ChatClient.Builder builder,
            @Value("${spring.ai.openai.chat.options.model:configured-model}") String model) {
        ChatClient chatClient = builder.build();
        return new AiTextGenerator() {
            @Override
            public Flux<String> stream(String systemPrompt, String userPrompt) {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .stream()
                        .content();
            }

            @Override
            public String generate(String systemPrompt, String userPrompt) {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            }

            @Override
            public String provider() {
                return "spring-ai-openai";
            }

            @Override
            public String model() {
                return model;
            }

            @Override
            public boolean configured() {
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(AiTextGenerator.class)
    AiTextGenerator fallbackAiTextGenerator() {
        return new AiTextGenerator() {
            private static final String MESSAGE =
                    "AI 模型尚未配置；课程权限与上下文检查已完成，请配置 AI_CHAT_PROVIDER 和模型密钥后重试。";

            @Override
            public Flux<String> stream(String systemPrompt, String userPrompt) {
                return Flux.just(MESSAGE);
            }

            @Override
            public String generate(String systemPrompt, String userPrompt) {
                return MESSAGE;
            }

            @Override
            public String provider() {
                return "fallback";
            }

            @Override
            public String model() {
                return "none";
            }

            @Override
            public boolean configured() {
                return false;
            }
        };
    }
}
