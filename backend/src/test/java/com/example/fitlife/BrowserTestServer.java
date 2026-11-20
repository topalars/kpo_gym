package com.example.fitlife;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

/**
 * Optional disposable PostgreSQL fixture for browser QA. Requires ADMIN_EMAIL / ADMIN_PASSWORD.
 */
public class BrowserTestServer {
    public static void main(String[] args) throws Exception {
        var pg = EmbeddedPostgres.builder().setPort(0).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pg.close();
            } catch (Exception ignored) {
            }
        }));
        SpringApplication.run(FitLifeApplication.class, "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"), "--spring.datasource.username=postgres", "--spring.datasource.password=", "--server.port=8081");
    }
}
