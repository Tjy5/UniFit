-- Normalize recommendation_log.request_source into the backend RecommendationSource enum.
--
-- Mapping applied by this migration:
--   NULL or blank original value -> unknown
--   mall-home-dialog, mall_home_dialog, mall home dialog, MALL_HOME_DIALOG -> mall-home-dialog
--   wishlist, WISHLIST -> wishlist
--   order-feedback, order_feedback, order feedback, ORDER_FEEDBACK -> order-feedback
--   product-detail, product_detail, product detail, PRODUCT_DETAIL -> product-detail
--   unknown, UNKNOWN -> unknown
--   any other original value -> unknown
--
-- Optional deployment audit before running:
-- SELECT DISTINCT request_source AS original_request_source
-- FROM recommendation_log
-- WHERE request_source IS NULL
--    OR TRIM(request_source) = ''
--    OR LOWER(REPLACE(REPLACE(TRIM(request_source), '_', '-'), ' ', '-')) NOT IN
--       ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown');

UPDATE recommendation_log
SET request_source = CASE
    WHEN request_source IS NULL OR TRIM(request_source) = '' THEN 'unknown'
    WHEN LOWER(REPLACE(REPLACE(TRIM(request_source), '_', '-'), ' ', '-')) IN
         ('mall-home-dialog', 'wishlist', 'order-feedback', 'product-detail', 'unknown')
        THEN LOWER(REPLACE(REPLACE(TRIM(request_source), '_', '-'), ' ', '-'))
    ELSE 'unknown'
END;

ALTER TABLE recommendation_log
    MODIFY COLUMN request_source VARCHAR(64) NOT NULL DEFAULT 'unknown' COMMENT '请求来源';
