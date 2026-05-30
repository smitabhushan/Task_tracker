package com.tasktracker.api.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "databaseUrlOverrides";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String explicitDbUrl = environment.getProperty("DB_URL");
        String databaseUrl = environment.getProperty("DATABASE_URL");

        if (StringUtils.hasText(explicitDbUrl) || !StringUtils.hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        if (databaseUrl.startsWith("jdbc:mysql://")) {
            properties.put("spring.datasource.url", databaseUrl);
        } else if (databaseUrl.startsWith("mysql://")) {
            applyMysqlDatabaseUrl(databaseUrl, environment, properties);
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    private void applyMysqlDatabaseUrl(String databaseUrl, ConfigurableEnvironment environment, Map<String, Object> properties) {
        URI uri = URI.create(databaseUrl);
        String path = StringUtils.hasText(uri.getPath()) ? uri.getPath() : "/task_tracker";
        String query = StringUtils.hasText(uri.getQuery()) ? uri.getQuery() : "useSSL=true&allowPublicKeyRetrieval=true";

        properties.put("spring.datasource.url", "jdbc:mysql://" + uri.getHost() + port(uri) + path + "?" + query);

        if (!StringUtils.hasText(environment.getProperty("DB_USERNAME")) && StringUtils.hasText(uri.getUserInfo())) {
            String[] credentials = uri.getUserInfo().split(":", 2);
            properties.put("spring.datasource.username", decode(credentials[0]));
            if (credentials.length > 1 && !StringUtils.hasText(environment.getProperty("DB_PASSWORD"))) {
                properties.put("spring.datasource.password", decode(credentials[1]));
            }
        }
    }

    private String port(URI uri) {
        return uri.getPort() == -1 ? "" : ":" + uri.getPort();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
