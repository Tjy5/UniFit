#!/usr/bin/env node

import { spawnSync } from 'node:child_process';

const namespace = process.env.SUIOS_E2E_NAMESPACE || 'SUIOS_E2E__';
const userBaseUrl = trimSlash(process.env.SUIOS_E2E_USER_BASE_URL || 'http://127.0.0.1:9090');
const adminBaseUrl = trimSlash(process.env.SUIOS_E2E_ADMIN_BASE_URL || 'http://127.0.0.1:9091');
const mysql = {
  bin: process.env.SUIOS_E2E_MYSQL_BIN || 'mysql',
  host: process.env.SUIOS_E2E_DB_HOST || '127.0.0.1',
  port: process.env.SUIOS_E2E_DB_PORT || '3306',
  database: process.env.SUIOS_E2E_DB_NAME || 'suios',
  user: process.env.SUIOS_E2E_DB_USER || process.env.USERSYSTEM_DB_USERNAME || 'dev_only_user',
  password: process.env.SUIOS_E2E_DB_PASSWORD || process.env.USERSYSTEM_DB_PASSWORD || 'dev_only_password',
};

const ids = {
  schoolName: `${namespace}School`,
  gradeName: `${namespace}Grade`,
  uniformName: `${namespace}Uniform`,
  adminUsername: process.env.SUIOS_E2E_ADMIN_USERNAME || 'admin',
  adminPassword: process.env.SUIOS_E2E_ADMIN_PASSWORD,
  adminPasswordHash: process.env.SUIOS_E2E_ADMIN_PASSWORD_HASH,
  userAccount: `${namespace}user_${Date.now()}`,
  userPassword: 'E2eUser123!',
};

async function main() {
  console.log(`Full-chain E2E namespace: ${namespace}`);
  assert(ids.adminPassword, 'Set SUIOS_E2E_ADMIN_PASSWORD for the prepared admin test account.');
  preflightMysql();
  preflightSchema();
  await preflightHttp(`${userBaseUrl}/api/s-schools/selectList`, 'user backend');
  await preflightHttp(`${adminBaseUrl}/auth/login`, 'admin backend', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: ids.adminUsername, password: ids.adminPassword }),
  });

  const seed = seedDatabase();
  console.log(`Seed ready: school=${seed.schoolId}, grade=${seed.gradeId}, uniform=${seed.uniformId}, size=${seed.sizeId}`);

  let userToken;
  let userId;
  let order;
  let orderItem;

  try {
    await requestJson(`${userBaseUrl}/api/users/register`, {
      method: 'POST',
      body: { userAccount: ids.userAccount, userPassword: ids.userPassword },
      expectedStatuses: [200],
    });

    const login = await requestJson(`${userBaseUrl}/api/users/login`, {
      method: 'POST',
      body: { userAccount: ids.userAccount, userPassword: ids.userPassword },
    });
    userToken = unwrapData(login).token;
    userId = unwrapData(login).userId;
    assert(userToken, 'User login did not return a token.');
    assert(userId, 'User login did not return a userId.');

    await requestJson(`${userBaseUrl}/api/users/info`, { headers: auth(userToken) });

    const schools = unwrapData(await requestJson(`${userBaseUrl}/api/s-schools/selectList`));
    assert(findBy(schools, 'schoolName', ids.schoolName), 'Seed school is not discoverable through user API.');

    const grades = unwrapData(await requestJson(`${userBaseUrl}/api/s-grades/listBySchool?schoolId=${seed.schoolId}`));
    assert(findBy(grades, 'gradeName', ids.gradeName), 'Seed grade is not discoverable through user API.');

    const uniforms = unwrapData(await requestJson(`${userBaseUrl}/api/s-uniform/active`, { headers: auth(userToken) }));
    assert(findBy(uniforms, 'name', ids.uniformName), 'Seed uniform is not discoverable through active uniform API.');

    const sizes = unwrapData(await requestJson(`${userBaseUrl}/api/s-sizes/all`, { headers: auth(userToken) }));
    assert(findBy(sizes, 'sizeName', '165'), 'Seed size is not discoverable through size API.');

    const recommendation = unwrapData(await requestJson(`${userBaseUrl}/api/size-recommendations/recommend`, {
      method: 'POST',
      headers: auth(userToken),
      body: {
        uniformId: seed.uniformId,
        messageUserAge: 13,
        messageUserSex: '男',
        height: 165,
        weight: 52,
        chest: 84,
        waist: 72,
        hip: 89,
        shoulder: 39,
        source: 'full_chain_e2e',
      },
    }));
    assert(recommendation.recommendationLogId, 'Recommendation did not return recommendationLogId.');
    assert(recommendation.available || recommendation.message, 'Recommendation response has neither a usable recommendation nor an explicit fallback message.');

    const targetSizeName = recommendation.recommended?.sizeName || '165';
    const sizeId = sizeIdByName(sizes, targetSizeName) || seed.sizeId;

    await requestJson(`${userBaseUrl}/api/cart/add?uniformId=${seed.uniformId}&sizeId=${sizeId}&quantity=1&recommendationLogId=${recommendation.recommendationLogId}`, {
      method: 'POST',
      headers: auth(userToken),
    });

    const address = unwrapData(await requestJson(`${userBaseUrl}/api/s-addresses`, {
      method: 'POST',
      headers: auth(userToken),
      expectedStatuses: [200, 201],
      body: {
        recipientName: `${namespace}Recipient`,
        phoneNumber: '13800138000',
        province: 'Guangdong',
        city: 'Shenzhen',
        district: 'Nanshan',
        streetAddress: `${namespace} Test Street`,
        isDefault: true,
      },
    }));
    assert(address.id, 'Address creation did not return an id.');

    order = unwrapData(await requestJson(`${userBaseUrl}/api/s-orders/create`, {
      method: 'POST',
      headers: auth(userToken),
      body: { addressId: address.id, remark: `${namespace} full-chain order` },
      expectedStatuses: [201],
    }));
    assert(order.id, 'Order creation did not return an id.');

    const readback = unwrapData(await requestJson(`${userBaseUrl}/api/s-orders/${order.id}`, { headers: auth(userToken) }));
    orderItem = readback.orderItems?.find((item) => Number(item.uniformId) === Number(seed.uniformId));
    assert(orderItem, 'Order readback does not contain the seed uniform.');
    assert(Number(orderItem.sizeId || sizeId) === Number(sizeId), 'Order item size does not match the selected/recommended size.');

    await requestJson(`${userBaseUrl}/api/s-orders/${order.id}/fulfillment-commands`, {
      method: 'POST',
      headers: auth(userToken),
      body: { command: 'SIMULATE_PAYMENT_SUCCESS', reason: `${namespace} payment simulation` },
    });

    const adminLoginForFulfillment = unwrapData(await requestJson(`${adminBaseUrl}/auth/login`, {
      method: 'POST',
      body: { username: ids.adminUsername, password: ids.adminPassword },
    }));
    const adminTokenForFulfillment = adminLoginForFulfillment.token;
    assert(adminTokenForFulfillment, 'Admin login for fulfillment did not return token.');

    await requestJson(`${adminBaseUrl}/orders/${order.id}/fulfillment-commands`, {
      method: 'POST',
      headers: auth(adminTokenForFulfillment),
      body: { command: 'SHIP_ORDER', reason: `${namespace} ship order` },
    });

    await requestJson(`${adminBaseUrl}/orders/${order.id}/fulfillment-commands`, {
      method: 'POST',
      headers: auth(adminTokenForFulfillment),
      body: { command: 'CONFIRM_RECEIPT', reason: `${namespace} confirm receipt` },
    });

    const feedback = unwrapData(await requestJson(`${userBaseUrl}/api/size-feedback/order-items/${orderItem.orderItemId}`, {
      method: 'PUT',
      headers: auth(userToken),
      body: {
        recommendedSize: recommendation.recommended?.sizeName || targetSizeName,
        purchasedSize: orderItem.sizeNameSnapshot || targetSizeName,
        satisfaction: 'FIT',
        issueParts: [],
        note: `${namespace} feedback`,
      },
    }));
    assert(feedback.orderItemId, 'Feedback save did not return orderItemId.');

    const feedbackReadback = unwrapData(await requestJson(`${userBaseUrl}/api/size-feedback/order-items/${orderItem.orderItemId}`, { headers: auth(userToken) }));
    assert(feedbackReadback.satisfaction === 'FIT', 'Feedback readback did not reflect saved satisfaction.');

    const adminLogin = unwrapData(await requestJson(`${adminBaseUrl}/auth/login`, {
      method: 'POST',
      body: { username: ids.adminUsername, password: ids.adminPassword },
    }));
    const adminToken = adminLogin.token;
    assert(adminToken, 'Admin login did not return token.');

    await requestJson(`${adminBaseUrl}/auth/info`, { headers: auth(adminToken) });
    const adminOrders = unwrapData(await requestJson(`${adminBaseUrl}/orders/list?pageNum=1&pageSize=20&userId=${userId}`, { headers: auth(adminToken) }));
    const adminOrder = (adminOrders.records || []).find((item) => Number(item.id) === Number(order.id));
    assert(adminOrder, 'Admin order list cannot observe the created E2E order.');

    await poll(async () => {
      const stats = unwrapData(await requestJson(`${adminBaseUrl}/dashboard/stats`, { headers: auth(adminToken) }));
      assert(Array.isArray(stats.recentOrders), 'Dashboard stats response does not include recentOrders.');
      return true;
    }, 'dashboard stats response shape');

    console.log('Full-chain E2E passed.');
  } finally {
    if (process.env.SUIOS_E2E_SKIP_TEARDOWN !== 'true') {
      teardownDatabase();
      console.log('E2E teardown complete.');
    }
  }
}

function preflightMysql() {
  runMysql('SELECT 1;', 'MySQL connectivity check failed. Verify SUIOS_E2E_DB_* variables and that mysql is on PATH.');
}

function preflightSchema() {
  const required = [
    ['s_schools', 'school_name'],
    ['s_grades', 'grade_name'],
    ['s_uniform', 'category_key'],
    ['s_sizes', 'size_name'],
    ['s_inventory_sku', 'stock_quantity'],
    ['s_shopping_cart_items', 'recommendation_log_id'],
    ['s_order_items', 'size_id'],
    ['recommendation_log', 'request_source'],
    ['recommendation_log', 'order_item_id'],
    ['size_feedback', 'satisfaction'],
    ['user_size_preference_profile', 'source_summary'],
    ['s_user_activity_logs', 'action_type'],
    ['admin_user', 'username'],
  ];
  const values = required
    .map(([table, column], index) => `${index === 0 ? '' : 'UNION ALL '}SELECT '${table}' AS table_name, '${column}' AS column_name`)
    .join('\n');
  const rows = runMysql(`
    SELECT CONCAT(v.table_name, '.', v.column_name)
    FROM (${values}) v
    LEFT JOIN information_schema.columns c
      ON c.table_schema = DATABASE()
     AND c.table_name = v.table_name
     AND c.column_name = v.column_name
    WHERE c.column_name IS NULL;
  `, 'Schema compatibility check failed.');
  assert(!rows.trim(), `Missing required E2E schema columns:\n${rows}`);
}

function seedDatabase() {
  const sql = `
    SET @ns := ${q(namespace)};
    SET @school_name := ${q(ids.schoolName)};
    SET @grade_name := ${q(ids.gradeName)};
    SET @uniform_name := ${q(ids.uniformName)};

    INSERT INTO s_schools (school_name, school_address, contact_person, contact_phone, create_by, create_time, update_by, update_time, remark)
    SELECT @school_name, CONCAT(@ns, 'Address'), CONCAT(@ns, 'Contact'), '13800138000', @ns, NOW(), @ns, NOW(), @ns
    WHERE NOT EXISTS (SELECT 1 FROM s_schools WHERE school_name = @school_name);
    SELECT @school_id := school_id FROM s_schools WHERE school_name = @school_name LIMIT 1;

    INSERT INTO s_grades (grade_name, school_id, create_by, create_time, update_by, update_time, remark)
    SELECT @grade_name, @school_id, @ns, NOW(), @ns, NOW(), @ns
    WHERE NOT EXISTS (SELECT 1 FROM s_grades WHERE grade_name = @grade_name AND school_id = @school_id);
    SELECT @grade_id := grade_id FROM s_grades WHERE grade_name = @grade_name AND school_id = @school_id LIMIT 1;

    INSERT INTO s_sizes (size_name, min_height, max_height, min_weight, max_weight, min_chest, max_chest, min_waist, max_waist, min_hip, max_hip, min_shoulder, max_shoulder, create_by, create_time, update_by, update_time, remark)
    SELECT '165', 160, 170, 45, 65, 80, 88, 68, 76, 84, 94, 36, 42, @ns, NOW(), @ns, NOW(), @ns
    WHERE NOT EXISTS (SELECT 1 FROM s_sizes WHERE size_name = '165' AND remark = @ns);
    SELECT @size_id := id FROM s_sizes WHERE size_name = '165' AND remark = @ns LIMIT 1;

    INSERT INTO s_uniform (name, intro, image, price, status, average_rating, review_count, create_by, create_time, update_by, update_time, remark, school_id, grade_id, category_key)
    SELECT @uniform_name, CONCAT(@ns, 'Intro'), '', 199.00, 0, 0.00, 0, @ns, NOW(), @ns, NOW(), @ns, @school_id, @grade_id, 'GENERAL'
    WHERE NOT EXISTS (SELECT 1 FROM s_uniform WHERE name = @uniform_name);
    SELECT @uniform_id := id FROM s_uniform WHERE name = @uniform_name LIMIT 1;

    INSERT INTO s_inventory_sku (uniform_id, size_id, stock_quantity, reserved_quantity, safety_stock, reorder_point, lead_time_days, status, create_by, create_time, update_by, update_time, remark)
    SELECT @uniform_id, @size_id, 100, 0, 5, 10, 3, 'ACTIVE', @ns, NOW(), @ns, NOW(), @ns
    WHERE NOT EXISTS (SELECT 1 FROM s_inventory_sku WHERE uniform_id = @uniform_id AND size_id = @size_id AND status = 'ACTIVE');

    INSERT INTO admin_user (username, password, nickname, avatar, status)
    SELECT ${q(ids.adminUsername)}, ${q(ids.adminPasswordHash || ids.adminPassword)}, '系统管理员', '', 1
    WHERE ${q(ids.adminPasswordHash || '')} <> ''
      AND NOT EXISTS (SELECT 1 FROM admin_user WHERE username = ${q(ids.adminUsername)});

    SELECT CONCAT(@school_id, '\\t', @grade_id, '\\t', @uniform_id, '\\t', @size_id);
  `;
  const output = runMysql(sql, 'E2E seed setup failed.');
  const parts = lastMysqlOutputLine(output).split(/\t/).map(Number);
  assert(parts.length === 4 && parts.every(Boolean), `Seed setup did not return usable ids: ${output}`);
  return { schoolId: parts[0], gradeId: parts[1], uniformId: parts[2], sizeId: parts[3] };
}

function teardownDatabase() {
  runMysql(`
    SET @ns := ${q(namespace)};
    DELETE sf FROM size_feedback sf JOIN s_order_items oi ON oi.order_item_id = sf.order_item_id JOIN s_orders o ON o.id = oi.order_id JOIN user_account ua ON ua.user_id = o.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE rl FROM recommendation_log rl LEFT JOIN user_account ua ON ua.user_id = rl.user_id WHERE rl.request_source = 'full_chain_e2e' OR ua.user_account LIKE CONCAT(@ns, '%');
    DELETE sc FROM s_shopping_cart_items sc JOIN user_account ua ON ua.user_id = sc.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE oi FROM s_order_items oi JOIN s_orders o ON o.id = oi.order_id JOIN user_account ua ON ua.user_id = o.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE o FROM s_orders o JOIN user_account ua ON ua.user_id = o.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE a FROM s_address a JOIN user_account ua ON ua.user_id = a.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE up FROM user_size_preference_profile up JOIN user_account ua ON ua.user_id = up.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE ue FROM user_size_preference_event ue JOIN user_account ua ON ua.user_id = ue.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE al FROM s_user_activity_logs al LEFT JOIN user_account ua ON ua.user_id = al.user_id WHERE ua.user_account LIKE CONCAT(@ns, '%') OR al.log_detail LIKE CONCAT('%', @ns, '%');
    DELETE um FROM user_message um JOIN user_account ua ON ua.user_id = um.message_user_id WHERE ua.user_account LIKE CONCAT(@ns, '%');
    DELETE FROM user_account WHERE user_account LIKE CONCAT(@ns, '%');
    DELETE im FROM s_inventory_movements im JOIN s_inventory_sku sku ON sku.sku_id = im.sku_id JOIN s_uniform u ON u.id = sku.uniform_id WHERE u.name LIKE CONCAT(@ns, '%');
    DELETE sku FROM s_inventory_sku sku JOIN s_uniform u ON u.id = sku.uniform_id WHERE u.name LIKE CONCAT(@ns, '%');
    DELETE FROM s_uniform WHERE name LIKE CONCAT(@ns, '%');
    DELETE g FROM s_grades g JOIN s_schools s ON s.school_id = g.school_id WHERE s.school_name LIKE CONCAT(@ns, '%');
    DELETE FROM s_schools WHERE school_name LIKE CONCAT(@ns, '%');
    DELETE FROM s_sizes WHERE remark = @ns;
  `, 'E2E teardown failed.');
}

async function preflightHttp(url, name, options = {}) {
  try {
    const response = await fetch(url, options);
    if (response.status >= 500) {
      throw new Error(`HTTP ${response.status}`);
    }
  } catch (error) {
    throw new Error(`${name} is unavailable at ${url}: ${error.message}`);
  }
}

async function requestJson(url, options = {}) {
  const expectedStatuses = options.expectedStatuses || [200];
  const headers = { ...(options.headers || {}) };
  let body = options.body;
  if (body !== undefined && typeof body !== 'string') {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }
  const response = await fetch(url, { ...options, headers, body });
  const text = await response.text();
  const json = text ? JSON.parse(text) : null;
  if (!expectedStatuses.includes(response.status)) {
    throw new Error(`${options.method || 'GET'} ${url} returned HTTP ${response.status}: ${text}`);
  }
  if (json && typeof json.code === 'number') {
    assert(json.code === 200, `${url} returned application code ${json.code}: ${json.message || json.msg || text}`);
  }
  return json;
}

function runMysql(sql, failureMessage) {
  const args = ['--batch', '--raw', '--skip-column-names', '-h', mysql.host, '-P', mysql.port, '-u', mysql.user, mysql.database];
  const result = spawnSync(mysql.bin, args, {
    input: `SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;\n${sql}`,
    encoding: 'utf8',
    env: { ...process.env, MYSQL_PWD: mysql.password },
  });
  if (result.status !== 0) {
    throw new Error(`${failureMessage}\n${result.stderr || result.stdout}`);
  }
  return result.stdout || '';
}

function lastMysqlOutputLine(output) {
  const lines = output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  return lines.at(-1) || '';
}

async function poll(fn, label, timeoutMs = 10000) {
  const started = Date.now();
  let lastError;
  while (Date.now() - started < timeoutMs) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      await new Promise((resolve) => setTimeout(resolve, 500));
    }
  }
  throw new Error(`Timed out waiting for ${label}: ${lastError?.message || 'unknown error'}`);
}

function unwrapData(response) {
  return response?.data ?? response;
}

function auth(token) {
  return { Authorization: `Bearer ${token}` };
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function findBy(items, key, value) {
  return Array.isArray(items) ? items.find((item) => item?.[key] === value) : undefined;
}

function sizeIdByName(sizes, sizeName) {
  return findBy(sizes, 'sizeName', sizeName)?.id;
}

function q(value) {
  return `'${String(value).replaceAll('\\', '\\\\').replaceAll("'", "''")}'`;
}

function trimSlash(value) {
  return value.replace(/\/+$/, '');
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
