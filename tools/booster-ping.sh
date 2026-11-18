#!/system/bin/sh
# Run as root: su -c /data/local/tmp/booster-ping.sh
set -eu

BASE="/data/local/tmp"
REQUEST="$BASE/booster_ramguard_ping.request"
RESPONSE="$BASE/booster_ramguard_ping.response"
NONCE="ping_$(date +%s)_$$"

rm -f "$RESPONSE"
printf '%s\n' "$NONCE" > "$REQUEST"

i=0
while [ "$i" -lt 50 ]; do
    if [ -r "$RESPONSE" ] && grep -qx "SUCCESSFUL:$NONCE" "$RESPONSE"; then
        echo "SUCCESSFUL"
        exit 0
    fi
    sleep 0.1
    i=$((i + 1))
done

echo "TIMEOUT: RAM Guard did not reply (callback/listener may not be active, or SELinux blocked file access)."
exit 1
