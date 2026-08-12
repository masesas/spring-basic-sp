package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class DummyJsonConfig {

    private final AppConfigProperties properties;

    @Bean
    RestTemplate dummyJsonRestTemplate() {
        AppConfigProperties.DummyJson dummyJson = properties.getDummyJson();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(dummyJson.getConnectTimeout());
        factory.setReadTimeout(dummyJson.getReadTimeout());
        return new RestTemplate(factory);
    }
}
