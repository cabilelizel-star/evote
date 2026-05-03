package com.evote;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class EvoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvoteApplication.class, args);
    }

    /**
     * Safely adds image columns to the voters table if they don't exist yet.
     * Uses MEDIUMBLOB (up to 16MB) for ID photos and selfies.
     */
    @Bean
    public CommandLineRunner migrateDb(JdbcTemplate db) {
        return args -> {
            addColumnIfMissing(db, "id_photo",     "MEDIUMBLOB");
            addColumnIfMissing(db, "selfie_photo", "MEDIUMBLOB");
        };
    }

    private void addColumnIfMissing(JdbcTemplate db, String column, String type) {
        try {
            // Check if column already exists
            Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voters' AND COLUMN_NAME = ?",
                Integer.class, column);
            if (count != null && count > 0) {
                System.out.println("Column already exists, skipping: " + column);
                return;
            }
            db.execute("ALTER TABLE voters ADD COLUMN " + column + " " + type);
            System.out.println("Migration OK: added column " + column);
        } catch (Exception e) {
            System.err.println("Migration failed for " + column + ": " + e.getMessage());
        }
    }
}
