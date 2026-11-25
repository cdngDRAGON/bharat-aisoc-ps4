#!/usr/bin/env python3
import subprocess
import time
import os
import sys
import argparse

def get_default_device():
    res = subprocess.run(["adb", "get-serialno"], capture_output=True, text=True)
    s = res.stdout.strip()
    return s if s and s != "unknown" else None

parser = argparse.ArgumentParser(description="Record OnePlus 15 / Android Demo Walkthrough")
parser.add_argument("--serial", default=get_default_device(), help="Target ADB device serial (defaults to adb get-serialno)")
args, _ = parser.parse_known_args()
DEVICE_ID = args.serial

def adb(cmd):
    full_cmd = ["adb", "-s", DEVICE_ID] + cmd
    return subprocess.run(full_cmd, capture_output=True, text=True)

def adb_shell(cmd):
    return adb(["shell"] + cmd.split())

def main():
    print(f"[*] Preparing OnePlus 15 ({DEVICE_ID}) for demo walkthrough...")
    
    # Prepare unconfined recording binary in /data/local/tmp
    adb_shell("cp /system/bin/screenrecord /data/local/tmp/myrecord")
    adb_shell("chmod 755 /data/local/tmp/myrecord")
    adb_shell("rm -f /data/local/tmp/bharat_ps4_demo.mp4")
    time.sleep(1)
    
    # Ensure app is in foreground and on Tab 0
    adb_shell("am start -n com.example.executorchllamademo/.MainActivity")
    time.sleep(2)
    adb_shell("input tap 220 452") # Tab 0 Tutor
    time.sleep(1)
    adb_shell("input swipe 600 800 600 2400 300") # scroll to top
    time.sleep(1)
    
    # Start screenrecord in background (42 seconds limit)
    print("[*] Starting screenrecord (42 seconds limit)...")
    rec_proc = subprocess.Popen([
        "adb", "-s", DEVICE_ID, "shell",
        "/data/local/tmp/myrecord", "--bit-rate", "8000000", "--time-limit", "42", "/data/local/tmp/bharat_ps4_demo.mp4"
    ])
    
    time.sleep(2)
    
    try:
        # Step 1: Reactive Hindi localization (0s - 6s)
        print("[1/5] Demonstrating reactive whole-screen Hindi localization...")
        adb_shell("input tap 30 580") # English
        time.sleep(2)
        adb_shell("input tap 197 579") # Hindi
        time.sleep(3)
        
        # Step 2: Trigger SpinQuant INT4 roadmap in Hindi (6s - 17s)
        print("[2/5] Triggering SpinQuant INT4 roadmap in Hindi (streaming on-device)...")
        adb_shell("input swipe 200 700 1100 700 400") # ensure SpinQuant chip visible
        time.sleep(1)
        adb_shell("input tap 274 705") # SpinQuant chip
        # Wait 9 seconds for Llama-3.2-1B-Instruct to stream at >40 t/s
        time.sleep(9)
        
        # Step 3: Scroll to localized inline quiz & answer Option D instantly (17s - 25s)
        print("[3/5] Inspecting 3-week milestones and answering localized Hindi inline quiz (<10ms)...")
        adb_shell("input swipe 600 1800 600 1200 400") # scroll down card
        time.sleep(2)
        adb_shell("input swipe 600 1800 600 1400 300") # center Option D
        time.sleep(1.5)
        # Tap Option D (around y=2100)
        adb_shell("input tap 500 2100")
        time.sleep(3)
        
        # Step 4: Switch to Tab 1 (Skill Graph) (25s - 33s)
        print("[4/5] Inspecting 20-skill DAG Graph and BKT mastery in Tab 1...")
        adb_shell("input tap 636 452") # Tab 1 Graph
        time.sleep(3)
        adb_shell("input swipe 600 2000 600 1200 400") # scroll down graph
        time.sleep(3)
        
        # Step 5: Switch to Tab 2 (Quiz Arena) & cycle questions (33s - 42s)
        print("[5/5] Testing Tab 2 Quiz Arena in Hindi & cycling questions...")
        adb_shell("input tap 1052 452") # Tab 2 Quiz
        time.sleep(3)
        # Tap Next Question
        adb_shell("input tap 1000 1800")
        time.sleep(3)
        
    finally:
        print("[*] Waiting for screenrecord to complete...")
        rec_proc.wait()
        time.sleep(2)
        
    print("[*] Pulling recorded MP4 from OnePlus 15...")
    script_dir = os.path.dirname(os.path.abspath(__file__))
    dest_path = os.path.abspath(os.path.join(script_dir, "..", "deliverables", "Bharat_AISoC_PS4_Demo.mp4"))
    pull_res = subprocess.run([
        "adb", "-s", DEVICE_ID, "pull",
        "/data/local/tmp/bharat_ps4_demo.mp4",
        dest_path
    ], capture_output=True, text=True)
    print(pull_res.stdout)
    
    # Check file size & duration with ffprobe
    if os.path.exists(dest_path):
        size_mb = os.path.getsize(dest_path) / (1024 * 1024)
        print(f"[✓] Successfully recorded and pulled demo video: {dest_path} ({size_mb:.2f} MB)")
        probe_res = subprocess.run([
            "ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", dest_path
        ], capture_output=True, text=True)
        print(f"[✓] Video Duration: {probe_res.stdout.strip()} seconds")

if __name__ == "__main__":
    main()
