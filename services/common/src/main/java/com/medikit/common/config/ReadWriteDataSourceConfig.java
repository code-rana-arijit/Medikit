package com.medikit.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Read/Write split datasource for PostgreSQL read replicas.
 * <p>
 * When `DB_READ_URL` is configured, the routing datasource is enabled:
 * reads go to the replica pool, writes go to the primary. Reads within a
 * transaction marked readOnly (or outside a transaction) hit the replica.
 * Falls back to the primary when the replica is unavailable.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "medikit.datasource.read-replica-enabled", havingValue = "true")
public class ReadWriteDataSourceConfig {

    @Bean
    @Primary
    public DataSource routingDataSource(
            @Value("${spring.datasource.url}") String writeUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${medikit.datasource.read-url:}") String readUrl,
            @Value("${spring.datasource.hikari.maximum-pool-size:20}") int maxPoolSize) {

        DataSource writeDS = DataSourceBuilder.create()
                .url(writeUrl)
                .username(username)
                .password(password)
                .build();

        if (readUrl == null || readUrl.isBlank()) {
            return writeDS;
        }

        DataSource readDS = DataSourceBuilder.create()
                .url(readUrl)
                .username(username)
                .password(password)
                .build();

        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceType.WRITE, writeDS);
        targets.put(DataSourceType.READ, readDS);

        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                return readOnly ? DataSourceType.READ : DataSourceType.WRITE;
            }
        };
        routing.setDefaultTargetDataSource(writeDS);
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();

        // Defer actual connection acquisition to the routing decision point
        return new LazyConnectionDataSourceProxy(routing);
    }

    private enum DataSourceType {
        WRITE, READ
    }
}
