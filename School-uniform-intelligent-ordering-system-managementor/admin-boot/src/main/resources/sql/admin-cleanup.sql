-- Cleanup script for legacy RuoYi-derived admin tables.
-- IMPORTANT:
-- 1. Take a full database backup before running any statement below.
-- 2. Run Section A only after admin-api/admin-web have completed cutover.
-- 3. Run Section B only after the new admin system has been verified in production.

SET FOREIGN_KEY_CHECKS = 0;

-- Section A: safe to remove after the new admin system replaces legacy scheduling/codegen modules.
DROP TABLE IF EXISTS gen_table_column;
DROP TABLE IF EXISTS gen_table;

DROP TABLE IF EXISTS qrtz_blob_triggers;
DROP TABLE IF EXISTS qrtz_cron_triggers;
DROP TABLE IF EXISTS qrtz_fired_triggers;
DROP TABLE IF EXISTS qrtz_paused_trigger_grps;
DROP TABLE IF EXISTS qrtz_scheduler_state;
DROP TABLE IF EXISTS qrtz_simple_triggers;
DROP TABLE IF EXISTS qrtz_simprop_triggers;
DROP TABLE IF EXISTS qrtz_calendars;
DROP TABLE IF EXISTS qrtz_locks;
DROP TABLE IF EXISTS qrtz_triggers;
DROP TABLE IF EXISTS qrtz_job_details;

-- Section B: remove only after confirming the legacy managementor system is no longer needed.
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_post;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_logininfor;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_job_log;
DROP TABLE IF EXISTS sys_job;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_post;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_config;

SET FOREIGN_KEY_CHECKS = 1;
