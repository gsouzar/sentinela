ALTER TABLE app_users ADD COLUMN subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_users ADD COLUMN subscription_due BIGINT;
CREATE TABLE monitored_sites (
 id VARCHAR(36) PRIMARY KEY,user_id VARCHAR(36) NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 name VARCHAR(80) NOT NULL,origin VARCHAR(300) NOT NULL,domain VARCHAR(253) NOT NULL,
 revision INTEGER NOT NULL DEFAULT 1,verification_token VARCHAR(80) NOT NULL,verified_at BIGINT,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,integration_hash VARCHAR(64) UNIQUE,
 next_check BIGINT NOT NULL DEFAULT 0,lease_until BIGINT NOT NULL DEFAULT 0,created_at BIGINT NOT NULL,
 UNIQUE(user_id,origin)
);
CREATE INDEX idx_sites_due ON monitored_sites(next_check,enabled);
CREATE TABLE site_checks (
 id VARCHAR(36) PRIMARY KEY,site_id VARCHAR(36) NOT NULL REFERENCES monitored_sites(id) ON DELETE CASCADE,
 revision INTEGER NOT NULL,checked_at BIGINT NOT NULL,status VARCHAR(30) NOT NULL,
 response_ms BIGINT NOT NULL,http_code INTEGER NOT NULL,tls_expires BIGINT,result_json TEXT NOT NULL
);
CREATE INDEX idx_checks_site_date ON site_checks(site_id,revision,checked_at);
CREATE TABLE site_events (
 id VARCHAR(36) PRIMARY KEY,site_id VARCHAR(36) NOT NULL REFERENCES monitored_sites(id) ON DELETE CASCADE,
 revision INTEGER NOT NULL,event_id VARCHAR(36) NOT NULL,type VARCHAR(40) NOT NULL,
 simulated BOOLEAN NOT NULL,occurred_at BIGINT NOT NULL,received_at BIGINT NOT NULL,
 UNIQUE(site_id,event_id)
);
CREATE INDEX idx_events_site_date ON site_events(site_id,revision,received_at);
