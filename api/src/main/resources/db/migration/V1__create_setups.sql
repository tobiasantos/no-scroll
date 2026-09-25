CREATE TABLE setups (
    id                     SERIAL PRIMARY KEY,
    app_package            VARCHAR(200) NOT NULL,
    app_name               VARCHAR(100) NOT NULL,
    daily_limit_minutes    INTEGER NOT NULL,
    alert_mode             VARCHAR(10) NOT NULL DEFAULT 'ONCE',
    alert_interval_minutes INTEGER
);

INSERT INTO setups (app_package, app_name, daily_limit_minutes, alert_mode, alert_interval_minutes) VALUES
    ('com.instagram.android', 'Instagram', 60, 'ONCE', NULL),
    ('com.zhiliaoapp.musically', 'TikTok', 30, 'PERIODIC', 15);
