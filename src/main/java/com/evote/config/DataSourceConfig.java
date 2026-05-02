package com.evote.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        // Railway can inject DB credentials in several formats.
        // We try each one and use the first that works.
        String url      = null;
        String user     = null;
        String password = null;

        // Format 1: MYSQLHOST / MYSQLUSER / MYSQLPASSWORD / MYSQLDATABASE / MYSQLPORT
        String host = System.getenv("MYSQLHOST");
        if (host != null && !host.isBlank()) {
            String port = getEnv("MYSQLPORT", "3306");
            String db   = getEnv("MYSQLDATABASE", "railway");
            user        = getEnv("MYSQLUSER", "root");
            password    = getEnv("MYSQLPASSWORD", "");
            url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        }

        // Format 2: MYSQL_HOST / MYSQL_USER / MYSQL_PASSWORD / MYSQL_DATABASE
        if (url == null) {
            host = System.getenv("MYSQL_HOST");
            if (host != null && !host.isBlank()) {
                String port = getEnv("MYSQL_PORT", "3306");
                String db   = getEnv("MYSQL_DATABASE", "railway");
                user        = getEnv("MYSQL_USER", "root");
                password    = getEnv("MYSQL_PASSWORD", "");
                url = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
            }
        }

        // Format 3: DATABASE_URL (jdbc:mysql://...)
        if (url == null) {
            String dbUrl = System.getenv("DATABASE_URL");
            if (dbUrl != null && dbUrl.startsWith("mysql://")) {
                // Convert mysql:// to jdbc:mysql://
                url      = "jdbc:" + dbUrl;
                user     = getEnv("MYSQL_USER", "root");
                password = getEnv("MYSQL_PASSWORD", "");
            }
        }

        // Fallback: local dev
        if (url == null) {
            url      = "jdbc:mysql://127.0.0.1:3306/evote_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
            user     = "root";
            password = "root";
        }

        System.out.println(">>> Connecting to: " + url);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(20000);
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }

    private String getEnv(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultVal;
    }
}
