package com.speaker.app.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 已有数据库增量变更：仅通过 ALTER（由代码判断列/条件后执行，可重复启动）。
 * <p>
 * 新库仍以 {@code schema.sql} 一次性建表；此后凡改表结构，请同步：
 * <ul>
 *   <li>在 {@code resources/db/schema_alter.sql} 追加 ALTER 说明（供 DBA 手工或审计）</li>
 *   <li>在本类 {@link #applyMigrations()} 中增加对应「幂等」步骤</li>
 * </ul>
 */
@Component
@Order(1)
public class SchemaAlterMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaAlterMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaAlterMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            applyMigrations();
        } catch (Exception e) {
            log.warn("数据库结构增量迁移跳过或失败（若已是最新版本可忽略）: {}", e.getMessage());
        }
    }

    private void applyMigrations() {
        questionBankItemsMigrations();
        practiceSessionsMigrations();
    }

    private void practiceSessionsMigrations() {
        final String table = "practice_sessions";
        addColumnIfMissing(table, "topic_source", "VARCHAR(16) NULL");
        addColumnIfMissing(table, "season_label", "VARCHAR(64) NULL");
        addColumnIfMissing(table, "allow_ai_expand", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing(table, "session_state_json", "TEXT NULL");
        addColumnIfMissing(table, "is_deleted", "TINYINT(1) NOT NULL DEFAULT 0");
    }

    private void questionBankItemsMigrations() {
        final String table = "question_bank_items";
        addColumnIfMissing(table, "answer_text", "TEXT NULL");
        addColumnIfMissing(table, "keywords_json", "TEXT NULL");
        addColumnIfMissing(table, "is_deleted", "TINYINT(1) NOT NULL DEFAULT 0");
        modifyTopicToVarchar512IfNeeded(table);
    }

    private void addColumnIfMissing(String table, String column, String ddlSuffix) {
        Integer n = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                table,
                column);
        if (n != null && n == 0) {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + ddlSuffix);
            log.info("已添加列 {}.{}", table, column);
        }
    }

    /**
     * 历史迁移：topic 由较短 VARCHAR 扩为 512；已为目标类型时可能抛错，忽略即可。
     */
    private void modifyTopicToVarchar512IfNeeded(String table) {
        try {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` MODIFY COLUMN topic VARCHAR(512) NOT NULL");
        } catch (Exception ignored) {
            // 已是 VARCHAR(512) 或其它环境差异
        }
    }
}
