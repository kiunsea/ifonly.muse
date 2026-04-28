-- App Settings table for configurable application settings
CREATE TABLE IF NOT EXISTS app_setting (
    setting_key   TEXT NOT NULL PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description   TEXT,
    updated_at    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Cleanup Path table for scheduled file deletion
CREATE TABLE IF NOT EXISTS cleanup_path (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    path        TEXT    NOT NULL UNIQUE,
    description TEXT,
    path_type   TEXT    NOT NULL DEFAULT 'UNKNOWN',
    enabled     INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Task execution history table for audit trail across task types
CREATE TABLE IF NOT EXISTS task_execution_history (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id  TEXT    NOT NULL,
    task_group    TEXT    NOT NULL,
    task_key      TEXT    NOT NULL,
    task_name     TEXT,
    status        TEXT    NOT NULL,
    success       INTEGER NOT NULL DEFAULT 0,
    target_count  INTEGER,
    success_count INTEGER,
    failure_count INTEGER,
    started_at    TEXT,
    completed_at  TEXT,
    metadata_json TEXT,
    error_message TEXT,
    created_at    TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_task_execution_history_created_at
    ON task_execution_history(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_execution_history_task_group
    ON task_execution_history(task_group);

-- Cleanup trash item table for two-phase deletion (move then purge)
CREATE TABLE IF NOT EXISTS cleanup_trash_item (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_id    TEXT    NOT NULL,
    original_path   TEXT    NOT NULL,
    trash_path      TEXT    NOT NULL UNIQUE,
    item_type       TEXT    NOT NULL,
    size_bytes      INTEGER,
    moved_at        TEXT    NOT NULL,
    expire_at       TEXT    NOT NULL,
    status          TEXT    NOT NULL DEFAULT 'MOVED',
    delete_attempts INTEGER NOT NULL DEFAULT 0,
    deleted_at      TEXT,
    last_error      TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_cleanup_trash_item_expire_status
    ON cleanup_trash_item(status, expire_at);

CREATE INDEX IF NOT EXISTS idx_cleanup_trash_item_execution
    ON cleanup_trash_item(execution_id);

-- Echo Note Message table — 사용자 PC 에 저장되는 echo-note 보관함 메시지
-- echo-server 와 분리된 로컬 저장소. 자동 스케줄러가 status=READY + scheduled_at <= now() 인 항목을 발송 대상으로 선정.
CREATE TABLE IF NOT EXISTS echo_note_message (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    recipient_email      TEXT    NOT NULL,
    original_message     TEXT    NOT NULL,
    ai_generated_message TEXT,                                              -- AI 프리뷰 생성 후 채워짐
    locale               TEXT    NOT NULL DEFAULT 'ko',
    status               TEXT    NOT NULL DEFAULT 'DRAFT',                  -- DRAFT | READY | SENT
    scheduled_at         TEXT,                                              -- Phase 3 스케줄러가 설정 (3개월 후 임의 날짜). 사용자에게는 비공개
    sent_at              TEXT,                                              -- 발송 완료 시각 (Phase 3)
    created_at           TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at           TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE INDEX IF NOT EXISTS idx_echo_note_message_status_scheduled
    ON echo_note_message(status, scheduled_at);

CREATE INDEX IF NOT EXISTS idx_echo_note_message_created_at
    ON echo_note_message(created_at DESC);
