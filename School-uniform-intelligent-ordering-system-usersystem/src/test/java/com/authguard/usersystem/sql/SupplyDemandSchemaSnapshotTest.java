package com.authguard.usersystem.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SupplyDemandSchemaSnapshotTest {

    @Test
    void migrationShouldContainSupplyDemandSchemaObjects() throws IOException {
        String sql = Files.readString(Path.of("sql/migrations/20260518_supply_demand_management.sql"));

        assertTrue(sql.contains("ADD COLUMN size_id BIGINT"));
        assertTrue(sql.contains("idx_orderitem_uniform_size"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS s_inventory_sku"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS s_inventory_movements"));
        assertTrue(sql.contains("uk_inventory_sku_uniform_size_status"));
        assertTrue(sql.contains("idx_inventory_sku_low_stock"));
        assertTrue(sql.contains("related_order_item_id BIGINT"));
        assertTrue(sql.contains("fk_inventory_movement_order_item"));
    }

    @Test
    void rootSnapshotShouldContainAppliedSupplyDemandSchemaObjects() throws IOException {
        String sql = Files.readString(Path.of("../suios.sql"));

        assertTrue(sql.contains("CREATE TABLE `s_order_items`"));
        assertTrue(sql.contains("`size_id` bigint(0)"));
        assertTrue(sql.contains("INDEX `idx_orderitem_uniform_size`(`uniform_id`, `size_id`)"));
        assertTrue(sql.contains("CREATE TABLE `s_inventory_sku`"));
        assertTrue(sql.contains("CREATE TABLE `s_inventory_movements`"));
        assertTrue(sql.contains("UNIQUE INDEX `uk_inventory_sku_uniform_size_status`"));
        assertTrue(sql.contains("INDEX `idx_inventory_sku_low_stock`"));
        assertTrue(sql.contains("`related_order_item_id` bigint(0)"));
        assertTrue(sql.contains("CONSTRAINT `fk_inventory_movement_order_item`"));
    }
}
