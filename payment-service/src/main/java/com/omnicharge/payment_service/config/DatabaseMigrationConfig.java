package com.omnicharge.payment_service.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs on startup to fix the PostgreSQL CHECK constraint on transactions.status.
 * Hibernate's ddl-auto=update cannot modify existing CHECK constraints,
 * so we drop and recreate it to include all valid enum values.
 */
@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixTransactionStatusConstraint() {
        try {
            // Drop the old constraint if it exists
            jdbcTemplate.execute(
                "ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_status_check"
            );

            // Recreate with all valid enum values
            jdbcTemplate.execute(
                "ALTER TABLE transactions ADD CONSTRAINT transactions_status_check " +
                "CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUND_PENDING','CANCELLED'))"
            );

            log.info("══════════════════════════════════════════════════");
            log.info("  DATABASE MIGRATION — COMPLETED SUCCESSFULLY");
            log.info("  Constraint 'transactions_status_check' updated");
            log.info("  Allowed: PENDING, SUCCESS, FAILED, REFUND_PENDING, CANCELLED");
            log.info("══════════════════════════════════════════════════");

        } catch (Exception ex) {
            // Table may not exist yet on first startup — Hibernate will create it
            log.warn("DB migration skipped (table may not exist yet): {}", ex.getMessage());
        }
    }
}
