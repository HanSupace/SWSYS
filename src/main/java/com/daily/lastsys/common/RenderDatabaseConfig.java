package com.daily.lastsys.common;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class RenderDatabaseConfig {

    @Bean
    @ConditionalOnProperty(name = "DATABASE_URL")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource renderDataSource(@Value("${DATABASE_URL}") String databaseUrl) {
        URI databaseUri = URI.create(databaseUrl);
        String[] credentials = databaseUri.getRawUserInfo().split(":", 2);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://%s:%d%s".formatted(
                databaseUri.getHost(),
                databaseUri.getPort() < 0 ? 5432 : databaseUri.getPort(),
                databaseUri.getPath()
        ));
        dataSource.setUsername(decode(credentials[0]));
        dataSource.setPassword(credentials.length > 1 ? decode(credentials[1]) : "");
        return dataSource;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
