import cv2
import time
import pyttsx3

# Initialize text-to-speech engine
engine = pyttsx3.init()
engine.setProperty('rate', 150)  # speaking rate

# Initialize Haar Cascades for face and smile
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
smile_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_smile.xml")

# Reward system
points_per_smile = 10
reward_threshold = 100

# In-memory points storage
users = {}  # {"User1": points, "User2": points, ...}

# Face tracking
face_id_counter = 0
face_positions = {}  # face_id -> last position
reward_display_time = 2  # seconds
reward_timers = {}  # face_id -> reward start time

# Start camera
cap = cv2.VideoCapture(0)
last_call_time = 0
call_interval = 5  # seconds between "Please smile" messages

while True:
    ret, frame = cap.read()
    if not ret:
        break

    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.3, 5)
    smile_detected = False

    for (x, y, w, h) in faces:
        cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
        roi_gray = gray[y:y+h, x:x+w]
        roi_color = frame[y:y+h, x:x+w]

        # Assign a face ID based on closest previous position
        assigned_id = None
        for fid, pos in face_positions.items():
            fx, fy, fw, fh = pos
            if abs(fx - x) < 50 and abs(fy - y) < 50:
                assigned_id = fid
                break
        if assigned_id is None:
            face_id_counter += 1
            assigned_id = f"User{face_id_counter}"
        face_positions[assigned_id] = (x, y, w, h)

        # Detect smiles
        smiles = smile_cascade.detectMultiScale(roi_gray, 1.8, 20)
        if len(smiles) > 0:
            smile_detected = True
            for (sx, sy, sw, sh) in smiles:
                cv2.rectangle(roi_color, (sx, sy), (sx + sw, sy + sh), (0, 255, 0), 2)
                users[assigned_id] = users.get(assigned_id, 0) + points_per_smile

                # Reward
                if users[assigned_id] >= reward_threshold and assigned_id not in reward_timers:
                    reward_timers[assigned_id] = time.time()
                    print(f"{assigned_id} earned a reward! 💰 Total points: {users[assigned_id]}")
                    users[assigned_id] = 0  # reset points

    # Call "Please smile" if no smile detected recently
    current_time = time.time()
    if not smile_detected and current_time - last_call_time > call_interval:
        engine.say("Please smile")
        engine.runAndWait()
        last_call_time = current_time

    # Display points and rewards
    for fid, pts in users.items():
        pos = face_positions.get(fid)
        if pos:
            fx, fy, fw, fh = pos
            cv2.putText(frame, f"{fid}: {pts}", (fx, fy-10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 255), 2)

    # Display reward animation
    for fid, start_time in list(reward_timers.items()):
        if fid in face_positions:
            fx, fy, fw, fh = face_positions[fid]
            cv2.putText(frame, "🎉 REWARD! 🎉", (fx, fy + h + 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
        if current_time - start_time > reward_display_time:
            reward_timers.pop(fid)

    cv2.imshow("Smart Smile Recognition & Reward", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
engine.stop()