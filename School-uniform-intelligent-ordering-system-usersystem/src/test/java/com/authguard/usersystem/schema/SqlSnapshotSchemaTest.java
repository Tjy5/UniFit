package com.authguard.usersystem.schema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SqlSnapshotSchemaTest {

    private static final Path ROOT_SQL = Path.of("..", "suios.sql");

    @Test
    void rootSqlSnapshotShouldCoverRecommendationCalibrationAndPreferenceSchema() throws IOException {
        String sql = Files.readString(ROOT_SQL, StandardCharsets.UTF_8);

        assertColumns(sql, "recommendation_log", List.of(
                "school_id",
                "uniform_id",
                "category_key",
                "recommended_size_id",
                "recommended_size_name",
                "order_item_id",
                "request_source",
                "calibration_applied",
                "calibration_details",
                "personalization_applied",
                "personalization_details"
        ));
        assertColumns(sql, "s_shopping_cart_items", List.of("uniform_id", "size_id", "recommendation_log_id"));
        assertColumns(sql, "s_order_items", List.of("uniform_id", "size_id", "size_name_snapshot", "review_id"));
        assertColumns(sql, "s_uniform", List.of("category_key"));
        assertColumns(sql, "s_inventory_sku", List.of(
                "sku_id",
                "uniform_id",
                "size_id",
                "stock_quantity",
                "reserved_quantity",
                "safety_stock",
                "reorder_point",
                "status"
        ));
        assertColumns(sql, "size_feedback", List.of(
                "feedback_id",
                "user_id",
                "order_id",
                "order_item_id",
                "recommended_size",
                "purchased_size",
                "satisfaction",
                "issue_parts"
        ));
        assertColumns(sql, "calibration_params", List.of(
                "param_id",
                "scope_type",
                "scope_id",
                "target_size_id",
                "adjustment_value",
                "sample_size",
                "unique_user_count",
                "feedback_distribution",
                "confidence_level"
        ));
        assertColumns(sql, "calibration_audit", List.of("audit_id", "param_id", "action", "new_value"));
        assertColumns(sql, "user_size_preference_profile", List.of(
                "profile_id",
                "user_id",
                "category_key",
                "explicit_preference",
                "learned_direction",
                "source_summary"
        ));
        assertColumns(sql, "user_size_preference_event", List.of(
                "event_id",
                "order_item_id",
                "feedback_id",
                "uniform_id",
                "category_key",
                "recommended_size",
                "purchased_size",
                "satisfaction",
                "issue_parts",
                "direction_delta"
        ));
        assertColumns(sql, "s_order_status_logs", List.of(
                "id",
                "order_id",
                "event_type",
                "actor_type",
                "actor_id",
                "from_order_status",
                "to_order_status",
                "from_payment_status",
                "to_payment_status",
                "from_shipping_status",
                "to_shipping_status",
                "reason",
                "create_time"
        ));
        assertColumns(sql, "s_user_activity_logs", List.of(
                "log_id",
                "user_id",
                "session_id",
                "action_type",
                "target_type",
                "target_id",
                "log_detail",
                "request_url",
                "request_method",
                "log_time"
        ));
    }

    private static void assertColumns(String sql, String tableName, List<String> columns) {
        String tableSql = tableDefinition(sql, tableName);
        for (String column : columns) {
            assertTrue(
                    tableSql.contains("`" + column + "`"),
                    () -> tableName + " is missing column " + column
            );
        }
    }

    private static String tableDefinition(String sql, String tableName) {
        Pattern pattern = Pattern.compile("CREATE TABLE `?" + Pattern.quote(tableName) + "`?\\s*\\((.*?)\\) ENGINE", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        assertTrue(matcher.find(), () -> "Missing table " + tableName + " in " + ROOT_SQL);
        return matcher.group(1);
    }
}
