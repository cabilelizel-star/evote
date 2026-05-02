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
        // Log ALL environment variables for debugging
        System.out.println(">>> ALL ENV VARS:");
        System.getenv().forEach((k, v) -> {
            if (k.toUpperCase().contains("MYSQL") || k.toUpperCase().contains("DATABASE")
                    || k.toUpperCase().contains("DB_")) {
                System.out.println("    " + k + " = "
                    + (k.toUpperCase().contains("PASSWORD") || k.toUpperCase().contains("URL")
                       ? v.substring(0, Math.min(v.length(), 30)) + "..." : v));
            }
        });

        String url      = null;
        String user     = null;
        String password = null;

        // Priority 1: MYSQL_URL (jdbc format)
        String mysqlUrl = System.getenv("MYSQL_URL");
        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            if (mysqlUrl.startsWith("jdbc:mysql://")) {
                url      = mysqlUrl;
                user     = getEnv("MYSQLUSER", getEnv("MYSQL_USER", "root"));
                password = getEnv("MYSQLPASSWORD", getEnv("MYSQL_PASSWORD", ""));
                System.out.println(">>> Using MYSQL_URL (jdbc): " + url.substring(0, 40) + "...");
            } else if (mysqlUrl.startsWith("mysql://")) {
                // Parse mysql://user:pass@host:port/db
                try {
                    String stripped = mysqlUrl.substring(8);
                    String userInfo = stripped.substring(0, stripped.indexOf('@'));
                    String hostPart = stripped.substring(stripped.indexOf('@') + 1);
                    user     = userInfo.contains(":") ? userInfo.split(":")[0] : userInfo;
                    password = userInfo.contains(":") ? userInfo.split(":", 2)[1] : "";
                    url = "jdbc:mysql://" + hostPart
                        + (hostPart.contains("?") ? "&" : "?")
                        + "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
                    System.out.println(">>> Parsed MYSQL_URL (mysql://): " + url.substring(0, 40) + "...");
                } catch (Exception e) {
                    System.out.println(">>> Failed to parse MYSQL_URL: " + e.getMessage());
                }
            }
        }

        // Priority 2: MYSQLHOST + MYSQLPORT + MYSQLDATABASE + MYSQLUSER + MYSQLPASSWORD
        if (url == null) {
            String host = getEnv("MYSQLHOST", null);
            if (host != null) {
                String port = getEnv("MYSQLPORT", "3306");
                String db   = getEnv("MYSQLDATABASE", "railway");
                user        = getEnv("MYSQLUSER", getEnv("MYSQL_USER", "root"));
                password    = getEnv("MYSQLPASSWORD", getEnv("MYSQL_PASSWORD", ""));
                url = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
                System.out.println(">>> Using MYSQLHOST: " + url.substring(0, 40) + "...");
            }
        }

        // Priority 3: MYSQL_PUBLIC_URL
        if (url == null) {
            String pub = getEnv("MYSQL_PUBLIC_URL", null);
            if (pub != null && pub.startsWith("mysql://")) {
                try {
                    String stripped = pub.substring(8);
                    String userInfo = stripped.substring(0, stripped.indexOf('@'));
                    String hostPart = stripped.substring(stripped.indexOf('@') + 1);
                    user     = userInfo.contains(":") ? userInfo.split(":")[0] : userInfo;
                    password = userInfo.contains(":") ? userInfo.split(":", 2)[1] : "";
                    url = "jdbc:mysql://" + hostPart
                        + (hostPart.contains("?") ? "&" : "?")
                        + "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
                    System.out.println(">>> Using MYSQL_PUBLIC_URL: " + url.substring(0, 40) + "...");
                } catch (Exception e) {
                    System.out.println(">>> Failed to parse MYSQL_PUBLIC_URL: " + e.getMessage());
                }
            }
        }

        // Fallback
        if (url == null) {
            System.out.println(">>> WARNING: No MySQL env vars found! Using localhost fallback.");
            url      = "jdbc:mysql://127.0.0.1:3306/evote_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
            user     = "root";
            password = "root";
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
    }

    private String getEnv(String key, String defaultVal) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultVal;
    }
}
