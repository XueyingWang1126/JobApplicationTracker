package com.xueying.jobapplicationtracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs startup diagnostics so reviewers can verify runtime wiring quickly.
 */
@Component
public class StartupDiagnostics implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;

    public StartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String port = firstNonBlank(
                environment.getProperty("local.server.port"),
                environment.getProperty("server.port"),
                "8080"
        );
        String dbUrl = firstNonBlank(environment.getProperty("spring.datasource.url"), "not-configured");
        String minioEnabledRaw = firstNonBlank(environment.getProperty("minio.enabled"), "true");
        boolean minioEnabled = isEnabled(minioEnabledRaw);
        String minioEndpoint = firstNonBlank(environment.getProperty("minio.endpoint"), "not-configured");
        String minioBucket = firstNonBlank(environment.getProperty("minio.bucket"), "not-configured");

        log.info("job-application-tracker started");
        log.info("HTTP port: {}", port);
        log.info("DB URL: {}", dbUrl);
        log.info("MinIO enabled: {}", minioEnabled);
        if (minioEnabled) {
            log.info("MinIO endpoint: {}", minioEndpoint);
            log.info("MinIO bucket: {}", minioBucket);
        }
        log.info("Main URLs: http://localhost:{}/login | /dashboard | /applications | /documents", port);
    }

    private boolean isEnabled(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "on".equals(normalized);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}

