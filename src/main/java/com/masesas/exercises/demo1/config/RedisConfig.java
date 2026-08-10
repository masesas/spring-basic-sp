package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.config.prop.AppConfigProperties;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final AppConfigProperties appConfigProp;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(appConfigProp.getRedis().getHost());
        config.setPort(appConfigProp.getRedis().getPort());
        config.setUsername(appConfigProp.getRedis().getUsername());
        config.setPassword(RedisPassword.of(appConfigProp.getRedis().getPassword()));

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();

        builder
                .clientResources(DefaultClientResources.create())
                .commandTimeout(appConfigProp.getRedis().getTimeout())
                .poolConfig(new GenericObjectPoolConfig<>() {{
                    setMaxTotal(appConfigProp.getRedis().getLettucePoolMaxActive());
                    setMaxIdle(appConfigProp.getRedis().getLettucePoolMaxIdle());
                    setMinIdle(appConfigProp.getRedis().getLettucePoolMinIdle());
                    setMaxWait(appConfigProp.getRedis().getLettucePoolMaxWait());
                }});

        LettuceClientConfiguration clientConfiguration = builder.build();

        return new LettuceConnectionFactory(config, clientConfiguration);
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        String prefix = normalizePrefix(appConfigProp.getRedis().getKeyPrefix());
        if (!prefix.isEmpty()) {
            config = config.computePrefixWith(CacheKeyPrefix.prefixed(prefix));
        }

        return config;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) return "";
        String p = prefix.trim();
        if (p.isEmpty()) return "";
        return p.endsWith(":") ? p : (p + ":");
    }
}
