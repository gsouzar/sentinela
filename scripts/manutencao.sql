-- Exemplo PostgreSQL: revise politica de retencao e backup antes de executar.
DELETE FROM rate_buckets WHERE window_start < (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)*1000)::bigint - 86400000;
DELETE FROM consumed_refresh WHERE expires_at < (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)*1000)::bigint;
DELETE FROM sessions WHERE refresh_expires < (EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)*1000)::bigint;
