#!/usr/bin/env bash

set -euo pipefail

CHAT_ID="oc_8c5601cb094768da80fec886659d1fda"
WORKDIR="/Users/song/.lark-channel-workspaces/codex/default/xxl-job-boost"
SUMMARY_FILE="$WORKDIR/.reminders/continue-20260609-0930.md"
LOG_DIR="/tmp/xxl-job-boost-run"
STAMP_FILE="$LOG_DIR/continue-20260609-0930.sent"

mkdir -p "$LOG_DIR"

if [[ -f "$STAMP_FILE" ]]; then
  exit 0
fi

SUMMARY_TEXT=""
if [[ -f "$SUMMARY_FILE" ]]; then
  SUMMARY_TEXT="$(sed -n '1,40p' "$SUMMARY_FILE")"
fi

MESSAGE=$'提醒：现在是 2026-06-09 09:30，可以继续昨天的 XXL-JOB Boost / Soybean 前端迁移对话了。\n\n当前入口：\n- 前端：http://192.168.1.120:5173/\n- 项目目录：/Users/song/.lark-channel-workspaces/codex/default/xxl-job-boost\n\n我已把上次进度摘要记在：\n- .reminders/continue-20260609-0930.md\n\n你回复“继续”即可，我会从业务页迁移接着做。'

/Users/song/.nvm/versions/node/v20.17.0/bin/lark-cli im +messages-send \
  --chat-id "$CHAT_ID" \
  --text "$MESSAGE" \
  --idempotency-key "continue-20260609-0930" >/tmp/xxl-job-boost-run/continue-20260609-0930.send.out 2>/tmp/xxl-job-boost-run/continue-20260609-0930.send.err

touch "$STAMP_FILE"
