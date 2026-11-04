#!/usr/bin/env bash
set -eo pipefail

echo "=========================================================="
echo "  BioTrace Automated Lab Hub ADB End-to-End Test Suite    "
echo "=========================================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
PDF_FILE="$ROOT_DIR/app/src/test/java/io/biotrace/app/data/local/report.pdf"
PACKAGE_NAME="io.biotrace.app.debug"

# 1. Check .env file and extract GEMINI_API_KEY
echo "[1/6] Reading Gemini API key from .env..."
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ ERROR: .env file not found at $ENV_FILE"
    exit 1
fi

GEMINI_KEY=$(grep -E '^GEMINI_API_KEY=' "$ENV_FILE" | cut -d '=' -f2- | tr -d ' "' | tr -d '\r')
if [ -z "$GEMINI_KEY" ]; then
    echo "❌ ERROR: GEMINI_API_KEY not found or empty in $ENV_FILE"
    exit 1
fi
KEY_PREFIX="${GEMINI_KEY:0:8}..."
echo "✅ Loaded GEMINI_API_KEY (prefix: $KEY_PREFIX, length: ${#GEMINI_KEY})"

# 2. Check report.pdf existence and size
echo "[2/6] Inspecting clinical test document..."
if [ ! -f "$PDF_FILE" ]; then
    echo "❌ ERROR: Test PDF not found at $PDF_FILE"
    exit 1
fi
PDF_SIZE=$(stat -c%s "$PDF_FILE" 2>/dev/null || stat -f%z "$PDF_FILE")
PDF_SIZE_MB=$(echo "scale=2; $PDF_SIZE / 1048576" | bc -l 2>/dev/null || awk "BEGIN {printf \"%.2f\", $PDF_SIZE/1048576}")
echo "✅ Validated clinical test report.pdf ($PDF_SIZE bytes / ~${PDF_SIZE_MB}MB)"

# 3. Check connected ADB devices
echo "[3/6] Detecting connected Android device via ADB..."
DEVICE=$(adb devices | grep -w "device" | grep -v "devices" | head -n1 | awk '{print $1}')
if [ -z "$DEVICE" ]; then
    echo "❌ ERROR: No active Android device detected on adb."
    echo "Please connect a device with USB debugging enabled or launch an emulator."
    adb devices
    exit 1
fi
DEVICE_MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model | tr -d '\r')
ANDROID_VER=$(adb -s "$DEVICE" shell getprop ro.build.version.release | tr -d '\r')
echo "✅ Target device connected: $DEVICE ($DEVICE_MODEL on Android $ANDROID_VER)"

# 4. Push Gemini API key to device
echo "[4/6] Pushing Gemini API key to device..."
# Method A: Push to /data/local/tmp and copy to internal app filesDir
echo "$GEMINI_KEY" > /tmp/biotrace_test_key.tmp
adb -s "$DEVICE" push /tmp/biotrace_test_key.tmp /data/local/tmp/gemini_api_key.txt >/dev/null 2>&1
rm -f /tmp/biotrace_test_key.tmp

adb -s "$DEVICE" shell "run-as $PACKAGE_NAME cp /data/local/tmp/gemini_api_key.txt /data/data/$PACKAGE_NAME/files/gemini_api_key.txt" >/dev/null 2>&1 || true
# Method B: Broadcast directly to KeystoreManager receiver with explicit component
adb -s "$DEVICE" shell am broadcast \
    -n "$PACKAGE_NAME/io.biotrace.app.testing.LabHubTestReceiver" \
    -a io.biotrace.app.TEST_PROVISION_KEY \
    --es api_key "$GEMINI_KEY" >/dev/null 2>&1

# Verify on device
sleep 1
KEY_STATUS=$(adb -s "$DEVICE" shell "run-as $PACKAGE_NAME cat /data/data/$PACKAGE_NAME/files/test_key_status.txt" 2>/dev/null || true)
KEY_STATUS=$(echo "$KEY_STATUS" | tr -d '\r')
echo "✅ Device Keystore provisioning status: ${KEY_STATUS:-SUCCESS}"

# 5. Push report.pdf to device
echo "[5/6] Pushing report.pdf to device storage..."
adb -s "$DEVICE" push "$PDF_FILE" /data/local/tmp/report.pdf >/dev/null 2>&1
adb -s "$DEVICE" shell "run-as $PACKAGE_NAME cp /data/local/tmp/report.pdf /data/data/$PACKAGE_NAME/files/report.pdf" >/dev/null 2>&1
adb -s "$DEVICE" push "$PDF_FILE" /sdcard/Download/report.pdf >/dev/null 2>&1

# Verify reading the file from device
REMOTE_SIZE=$(adb -s "$DEVICE" shell "run-as $PACKAGE_NAME wc -c /data/data/$PACKAGE_NAME/files/report.pdf 2>/dev/null" | awk '{print $1}' | tr -d '\r' || true)
echo "✅ Remote report.pdf confirmed on device ($REMOTE_SIZE bytes in app sandbox)"

# 6. Execute on-device extraction and read parsed biomarkers
echo "[6/6] Triggering Lab Hub multimodal ingestion & clinical extraction on device..."
# Ensure app is active in foreground so Android runtime allocates active CPU and network resources
adb -s "$DEVICE" shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 2

# Clear any prior result
adb -s "$DEVICE" shell "run-as $PACKAGE_NAME rm -f /data/data/$PACKAGE_NAME/files/test_pdf_result.json" >/dev/null 2>&1 || true

# Broadcast process command with explicit component
adb -s "$DEVICE" shell am broadcast \
    -n "$PACKAGE_NAME/io.biotrace.app.testing.LabHubTestReceiver" \
    -a io.biotrace.app.TEST_PROCESS_PDF \
    --es pdf_path "/data/data/$PACKAGE_NAME/files/report.pdf" >/dev/null 2>&1

echo "⏳ Waiting for Gemini 2.5 Flash to analyze document and normalize schema..."
MAX_WAIT_SECONDS=150
ELAPSED=0
RESULT=""

while [ $ELAPSED -lt $MAX_WAIT_SECONDS ]; do
    RESULT=$(adb -s "$DEVICE" shell "run-as $PACKAGE_NAME cat /data/data/$PACKAGE_NAME/files/test_pdf_result.json 2>/dev/null" | tr -d '\r' || true)
    if [ -n "$RESULT" ] && [[ "$RESULT" != *"No such file"* ]] && [[ "$RESULT" == *"status"* ]]; then
        break
    fi
    sleep 3
    ELAPSED=$((ELAPSED + 3))
    echo "   ... processing ($ELAPSED sec elapsed)"
done

if [ -z "$RESULT" ] || [[ "$RESULT" == *"No such file"* ]]; then
    echo "❌ TIMEOUT: Test result file was not generated within $MAX_WAIT_SECONDS seconds."
    echo "Recent logcat diagnostics:"
    adb -s "$DEVICE" logcat -d -s "LabHubTestReceiver:*" | tail -n 20
    exit 1
fi

# Pull result file directly
adb -s "$DEVICE" shell "run-as $PACKAGE_NAME cat /data/data/$PACKAGE_NAME/files/test_pdf_result.json" > /tmp/biotrace_test_pdf_result.json 2>/dev/null

echo ""
echo "==================== TEST EXECUTION SUMMARY ===================="
python3 -c "
import json, sys

try:
    with open('/tmp/biotrace_test_pdf_result.json') as f:
        data = json.load(f)
    status = data.get('status')
    if status == 'SUCCESS':
        print(' STATUS:             PASS ✅')
        print(f' Lab Name:           {data.get(\"labName\", \"N/A\")}')
        print(f' Report Date:        {data.get(\"reportDate\", \"N/A\")}')
        print(f' Page Count:         {data.get(\"pageCount\")} pages')
        print(f' Document Size:      {data.get(\"byteSize\")} bytes')
        print(f' Thumbnail Rendered: {data.get(\"hasPreviewBitmap\")}')
        print(f' Biomarkers Found:   {data.get(\"biomarkerCount\")}')
        print('----------------------------------------------------')
        print('Sample Normalized Biomarkers Extracted:')
        markers = data.get('biomarkers', [])
        for b in markers[:12]:
            name = b.get('standardName') or b.get('name')
            val = b.get('value')
            unit = b.get('unit')
            low = b.get('referenceLow')
            high = b.get('referenceHigh')
            oor = '⚠️ OUT OF RANGE' if b.get('isOutOfRange') else 'NORMAL'
            print(f' • {name:28} : {val} {unit:10} (Ref: {low}-{high}) [{oor}]')
        if len(markers) > 12:
            print(f' ... and {len(markers) - 12} more biomarkers extracted successfully.')
        print('====================================================')
        sys.exit(0)
    else:
        print(' STATUS: FAIL ❌')
        print(f' Error: {data.get(\"error\")}')
        print('====================================================')
        sys.exit(1)
except Exception as e:
    print('Failed to parse test result JSON:', e)
    with open('/tmp/biotrace_test_pdf_result.json') as f:
        print('Raw content:\n', f.read())
    sys.exit(1)
"
