#!/bin/sh
set -eu
# Create a completed dump atomically. Keep prior copies; retention is a deliberate operator action.
while :; do
  stamp=$(date -u +%Y%m%dT%H%M%SZ)
  if pg_dump -Fc -f "/backups/fitlife-$stamp.dump.tmp"; then
    mv "/backups/fitlife-$stamp.dump.tmp" "/backups/fitlife-$stamp.dump"
    echo "Backup saved: fitlife-$stamp.dump"
  else
    echo "Backup failed; retrying in 60 seconds" >&2
    sleep 60
    continue
  fi
  sleep 86400
done
