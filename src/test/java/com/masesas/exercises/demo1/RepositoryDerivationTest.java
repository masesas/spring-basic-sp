package com.masesas.exercises.demo1;

import com.masesas.exercises.demo1.service.DetailKaryawanService;
import com.masesas.exercises.demo1.service.KaryawanService;
import com.masesas.exercises.demo1.service.KaryawanTrainingService;
import com.masesas.exercises.demo1.service.RekeningService;
import com.masesas.exercises.demo1.service.TrainingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/none",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
@Import(RepositoryDerivationTest.TestCacheConfig.class)
class RepositoryDerivationTest {

    @Autowired
    private KaryawanService karyawanService;

    @Autowired
    private DetailKaryawanService detailKaryawanService;

    @Autowired
    private RekeningService rekeningService;

    @Autowired
    private TrainingService trainingService;

    @Autowired
    private KaryawanTrainingService karyawanTrainingService;

    @Test
    void contextLoadsWithAllDerivedQueries() {
        assertThat(karyawanService).isNotNull();
        assertThat(detailKaryawanService).isNotNull();
        assertThat(rekeningService).isNotNull();
        assertThat(trainingService).isNotNull();
        assertThat(karyawanTrainingService).isNotNull();
    }

    /** Cache in-memory supaya test tidak perlu server Redis yang benar-benar hidup. */
    @TestConfiguration
    static class TestCacheConfig {

        @Bean
        @Primary
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
