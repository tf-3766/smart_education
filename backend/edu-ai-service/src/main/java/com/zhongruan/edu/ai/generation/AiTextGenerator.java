package com.zhongruan.edu.ai.generation;

import reactor.core.publisher.Flux;

public interface AiTextGenerator {
    Flux<String> stream(String systemPrompt, String userPrompt);

    String generate(String systemPrompt, String userPrompt);

    String provider();

    String model();

    boolean configured();
}
