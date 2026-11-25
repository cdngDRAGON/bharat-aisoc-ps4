#!/bin/bash
SERIAL="${1:-$(adb get-serialno 2>/dev/null)}"
if [ -z "$SERIAL" ] || [ "$SERIAL" = "unknown" ]; then
    echo "Error: No connected ADB device found."
    exit 1
fi

echo "=== Preparing fresh clean state for OnePlus 15 Demo Video ==="
adb -s $SERIAL shell am force-stop com.example.executorchllamademo
adb -s $SERIAL shell rm -f /sdcard/bharat_ps4_demo.mp4
adb -s $SERIAL shell am start -n com.example.executorchllamademo/.MainActivity
sleep 3.5

echo "=== Starting screenrecord (40s limit) ==="
adb -s $SERIAL shell screenrecord --bit-rate 8000000 --time-limit 40 /sdcard/bharat_ps4_demo.mp4 &
RECORD_PID=$!

sleep 2.5

# Step 1: Tap Tamil language chip (x=420, y=570)
echo "1. Tapping Tamil language chip..."
adb -s $SERIAL shell input tap 420 570
sleep 3.5

# Step 2: Tap SpinQuant INT4 chip (x=180, y=710)
echo "2. Tapping SpinQuant INT4 chip..."
adb -s $SERIAL shell input tap 180 710
sleep 6.0

# Step 3: Scroll down to reveal the diagnostic quiz
echo "3. Scrolling down to diagnostic quiz..."
adb -s $SERIAL shell input swipe 600 2200 600 1300 400
sleep 2.5

# Step 4: Tap Option A for instant <10ms inline grading
echo "4. Tapping Quiz Option A for instant inline grading..."
adb -s $SERIAL shell input tap 400 2080
sleep 3.5

# Step 5: Scroll back up to top tabs
echo "5. Scrolling up to tabs..."
adb -s $SERIAL shell input swipe 600 1200 600 2200 300
sleep 1.5

# Step 6: Tap Tab 1 (திறன் வரைபடம் / Skill Graph)
echo "6. Tapping Tab 1 (Skill Graph)..."
adb -s $SERIAL shell input tap 500 430
sleep 2.5

# Step 7: Scroll Tab 1 down to show 20 skills & DAG badges
echo "7. Scrolling through 20-skill DAG graph..."
adb -s $SERIAL shell input swipe 600 2000 600 1100 400
sleep 3.0

# Step 8: Tap Tab 2 (வினாடி வினா / Quiz Arena)
echo "8. Tapping Tab 2 (Quiz Arena)..."
adb -s $SERIAL shell input tap 850 430
sleep 2.0

# Step 9: Scroll Tab 2 down to question
echo "9. Scrolling Tab 2 to question..."
adb -s $SERIAL shell input swipe 600 2000 600 1200 300
sleep 2.0

# Step 10: Tap Next Question button (x=710, y=1660) to cycle question pool
echo "10. Cycling to Next Question in 3-question pool..."
adb -s $SERIAL shell input tap 710 1660
sleep 3.5

echo "Waiting for screenrecord to complete..."
wait $RECORD_PID 2>/dev/null
sleep 2
echo "=== Screen recording complete ==="

