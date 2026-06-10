package com.esunbank.seating.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共用層：將 XSS 跳脫設定套用至 Spring MVC 使用的 ObjectMapper。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer htmlEscapingCustomizer() {
        return builder -> builder.postConfigurer(JacksonConfig::applyHtmlEscapes);
    }

    private static void applyHtmlEscapes(ObjectMapper mapper) {
        mapper.getFactory().setCharacterEscapes(new HtmlCharacterEscapes());
    }
}
