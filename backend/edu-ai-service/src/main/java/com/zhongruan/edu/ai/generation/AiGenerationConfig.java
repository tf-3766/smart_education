package com.zhongruan.edu.ai.generation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class AiGenerationConfig {
    @Bean
    @ConditionalOnMissingBean(AiTextGenerator.class)
    AiTextGenerator aiTextGenerator(
            ObjectProvider<ChatModel> chatModelProvider,
            @Value("${edu.ai.provider-name:aliyun-bailian}") String provider,
            @Value("${spring.ai.openai.chat.options.model:configured-model}") String model) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return fallbackAiTextGenerator();
        }
        ChatClient chatClient = ChatClient.create(chatModel);
        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(16).build();
        ChatClient conversationalClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
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
            public Flux<String> stream(
                    String systemPrompt, String userPrompt, String conversationId, Object... tools) {
                var prompt = conversationalClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId));
                if (tools != null && tools.length > 0) {
                    prompt = prompt.tools(tools);
                }
                return prompt.stream().content();
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
                return provider;
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

    private AiTextGenerator fallbackAiTextGenerator() {
        return new AiTextGenerator() {
            private static final String MESSAGE =
                    "AI 模型尚未配置；授权上下文检查已完成，请配置 AI_CHAT_PROVIDER=openai 和 DASHSCOPE_API_KEY 后重试。";

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
