#!/usr/bin/env python3
"""
BioTrace Lab Hub Automated Test Runner
Pushes the Gemini API key from .env and report.pdf to the connected device via ADB,
triggers clinical multimodal document extraction, and verifies biomarker parsing.
"""

import os
import sys
import time
import json
import subprocess
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
ENV_PATH = ROOT_DIR / ".env"
PDF_PATH = ROOT_DIR / "app/src/test/java/io/biotrace/app/data/local/report.pdf"
PACKAGE_NAME = "io.biotrace.app.debug"

def run_adb(cmd_args, device=None, capture=True):
    base = ["adb"]
    if device:
        base.extend(["-s", device])
    full_cmd = base + cmd_args
    res = subprocess.run(full_cmd, stdout=subprocess.PIPE if capture else None,
                         stderr=subprocess.PIPE if capture else None, text=True)
    return res

def get_gemini_key():
    if not ENV_PATH.exists():
        raise FileNotFoundError(f".env file not found at {ENV_PATH}")
    with open(ENV_PATH) as f:
        for line in f:
            line = line.strip()
            if line.startswith("GEMINI_API_KEY="):
                key = line.split("=", 1)[1].strip().strip('"\'')
                if key:
                    return key
    raise ValueError(f"GEMINI_API_KEY not defined in {ENV_PATH}")

def get_connected_device():
    res = run_adb(["devices"])
    lines = res.stdout.strip().split("\n")[1:]
    devices = [l.split()[0] for l in lines if "\tdevice" in l]
    if not devices:
        raise RuntimeError("No active Android device detected via adb.")
    return devices[0]

def test_push_and_read_lab_report():
    print("=" * 60)
    print("  BioTrace Lab Hub ADB Automated Test Runner")
    print("=" * 60)

    # 1. Read API Key
    key = get_gemini_key()
    print(f"✅ Loaded GEMINI_API_KEY from .env (length: {len(key)})")

    # 2. Verify PDF
    if not PDF_PATH.exists():
        raise FileNotFoundError(f"report.pdf not found at {PDF_PATH}")
    pdf_size = PDF_PATH.stat().st_size
    print(f"✅ Found clinical report.pdf ({pdf_size} bytes / {pdf_size / (1024*1024):.2f} MB)")

    # 3. Detect Device
    device = get_connected_device()
    model = run_adb(["shell", "getprop", "ro.product.model"], device=device).stdout.strip()
    android_ver = run_adb(["shell", "getprop", "ro.build.version.release"], device=device).stdout.strip()
    print(f"✅ Target Device: {device} ({model}, Android {android_ver})")

    # 4. Push API Key
    print("⏳ Pushing Gemini API key to device Keystore...")
    # Push via local tmp
    key_tmp = Path("/tmp/bio_test_key.tmp")
    key_tmp.write_text(key)
    run_adb(["push", str(key_tmp), "/data/local/tmp/gemini_api_key.txt"], device=device)
    key_tmp.unlink(missing_ok=True)

    run_adb(["shell", f"run-as {PACKAGE_NAME} cp /data/local/tmp/gemini_api_key.txt /data/data/{PACKAGE_NAME}/files/gemini_api_key.txt"], device=device)
    run_adb(["shell", f"am broadcast -n {PACKAGE_NAME}/io.biotrace.app.testing.LabHubTestReceiver -a io.biotrace.app.TEST_PROVISION_KEY --es api_key '{key}'"], device=device)

    # 5. Push report.pdf
    print("⏳ Pushing report.pdf to device...")
    run_adb(["push", str(PDF_PATH), "/data/local/tmp/report.pdf"], device=device)
    run_adb(["shell", f"run-as {PACKAGE_NAME} cp /data/local/tmp/report.pdf /data/data/{PACKAGE_NAME}/files/report.pdf"], device=device)
    run_adb(["push", str(PDF_PATH), "/sdcard/Download/report.pdf"], device=device)

    # Verify device read
    verify_res = run_adb(["shell", f"run-as {PACKAGE_NAME} wc -c /data/data/{PACKAGE_NAME}/files/report.pdf"], device=device)
    remote_bytes = int(verify_res.stdout.strip().split()[0])
    print(f"✅ Verified remote report.pdf on device ({remote_bytes} bytes in sandbox)")
    assert remote_bytes == pdf_size, f"Size mismatch: expected {pdf_size}, got {remote_bytes}"

    # 6. Trigger Extraction
    print("⏳ Triggering Lab Hub document extraction on device...")
    run_adb(["shell", f"run-as {PACKAGE_NAME} rm -f /data/data/{PACKAGE_NAME}/files/test_pdf_result.json"], device=device)
    run_adb(["shell", f"am broadcast -n {PACKAGE_NAME}/io.biotrace.app.testing.LabHubTestReceiver -a io.biotrace.app.TEST_PROCESS_PDF --es pdf_path '/data/data/{PACKAGE_NAME}/files/report.pdf'"], device=device)

    # Poll for result
    max_wait = 90
    start = time.time()
    result_data = None

    while time.time() - start < max_wait:
        cat_res = run_adb(["shell", f"run-as {PACKAGE_NAME} cat /data/data/{PACKAGE_NAME}/files/test_pdf_result.json"], device=device)
        out = cat_res.stdout.strip()
        if out and "No such file" not in out:
            try:
                result_data = json.loads(out)
                break
            except json.JSONDecodeError:
                pass
        time.sleep(3)
        print(f"   ... analyzing report ({int(time.time() - start)}s elapsed)")

    if not result_data:
        print("❌ Test Timed Out waiting for result file.")
        sys.exit(1)

    print("\n" + "=" * 60)
    print("  TEST RESULTS SUMMARY")
    print("=" * 60)
    status = result_data.get("status")
    print(f"STATUS:             {status} {'✅' if status == 'SUCCESS' else '❌'}")
    print(f"Lab Center:         {result_data.get('labName', 'N/A')}")
    print(f"Report Date:        {result_data.get('reportDate', 'N/A')}")
    print(f"Page Count:         {result_data.get('pageCount')} pages")
    print(f"Preview Rendered:   {result_data.get('hasPreviewBitmap')}")
    print(f"Biomarkers Parsed:  {result_data.get('biomarkerCount')}")
    print("-" * 60)

    markers = result_data.get("biomarkers", [])
    for b in markers[:10]:
        name = b.get("standardName") or b.get("name")
        val = b.get("value")
        unit = b.get("unit")
        low = b.get("referenceLow")
        high = b.get("referenceHigh")
        oor = "⚠️ OUT OF RANGE" if b.get("isOutOfRange") else "NORMAL"
        print(f" • {name:28} : {val} {unit:10} (Ref: {low}-{high}) [{oor}]")

    if len(markers) > 10:
        print(f" ... and {len(markers) - 10} more biomarkers extracted successfully.")
    print("=" * 60)

    assert status == "SUCCESS", f"Test failed with error: {result_data.get('error')}"
    assert result_data.get("biomarkerCount", 0) > 0, "No biomarkers extracted from clinical report!"
    print("\n🎉 ALL ASSERTIONS PASSED! Lab Hub automated test succeeded.")

if __name__ == "__main__":
    test_push_and_read_lab_report()
