package com.stockflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot 4 dropped the auto-config that used to wire Flyway before JPA.
 * We declare a {@link Flyway} bean manually (with {@code initMethod=migrate})
 * and then patch the {@code entityManagerFactory} bean definition so it
 * depends on our flyway bean. This forces the migration to run before any
 * JPA query hits the database.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
    }

    @Bean
    public static BeanDefinitionRegistryPostProcessor flywayOrderingPostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                String[] candidates = {"entityManagerFactory", "jpaSharedEM_entityManagerFactory"};
                for (String name : candidates) {
                    if (registry.containsBeanDefinition(name)) {
                        BeanDefinition def = registry.getBeanDefinition(name);
                        def.setDependsOn(mergeDependsOn(def, "flyway"));
                        log.info("Added 'flyway' dependency to bean '{}'", name);
                    }
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // no-op
            }

            private String[] mergeDependsOn(BeanDefinition def, String... extras) {
                String[] existing = def.getDependsOn();
                if (existing == null || existing.length == 0) {
                    return extras;
                }
                String[] merged = new String[existing.length + extras.length];
                System.arraycopy(existing, 0, merged, 0, existing.length);
                System.arraycopy(extras, 0, merged, existing.length, extras.length);
                return merged;
            }
        };
    }
}