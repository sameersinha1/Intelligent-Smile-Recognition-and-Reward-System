# main.py
import base64
import cv2
import time
import threading
from flask import Flask, render_template
from flask_socketio import SocketIO

# --- Flask + SocketIO setup ---
app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*", async_mode='eventlet')  # Use eventlet for concurrency

# --- OpenCV Haar cascades ---
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
smile_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_smile.xml")

# --- In-memory state ---
users_points = {}
face_positions = {}
last_smile_time = {}
face_id_counter = 0
face_id_lock = threading.Lock()
camera_thread_running = True

# --- Configuration ---
FRAME_EMIT_INTERVAL = 0.03  # ~30 FPS
POINTS_PER_SMILE = 10
REWARD_THRESHOLD = 100
SMILE_DEBOUNCE = 2.0

# --- Routes ---
@app.route('/')
def index():
    return render_template('m.html')  # your HTML template

# --- Camera loop ---
def camera_loop():
    global face_id_counter, camera_thread_running

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("ERROR: Cannot open camera")
        return

    last_emit = 0
    while camera_thread_running:
        ret, frame = cap.read()
        if not ret:
            continue

        frame = cv2.flip(frame, 1)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        faces = face_cascade.detectMultiScale(gray, 1.3, 5)
        assigned_this_frame = set()

        for (x, y, w, h) in faces:
            assigned_id = None
            # Match existing face
            for fid, pos in face_positions.items():
                fx, fy, fw, fh = pos
                if abs(fx - x) < max(50, fw*0.5) and abs(fy - y) < max(50, fh*0.5):
                    assigned_id = fid
                    break
            # New face
            if assigned_id is None:
                with face_id_lock:
                    face_id_counter += 1
                    assigned_id = f"User{face_id_counter}"

            face_positions[assigned_id] = (x, y, w, h)
            assigned_this_frame.add(assigned_id)

            # Draw rectangle and ID
            cv2.rectangle(frame, (x, y), (x+w, y+h), (255, 0, 0), 2)
            cv2.putText(frame, assigned_id, (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255,255,255), 2)

            # Smile detection
            roi_gray = gray[y:y+h, x:x+w]
            smiles = smile_cascade.detectMultiScale(roi_gray, 1.8, 20)
            if len(smiles) > 0:
                now = time.time()
                last_time = last_smile_time.get(assigned_id, 0)
                if now - last_time > SMILE_DEBOUNCE:
                    users_points[assigned_id] = users_points.get(assigned_id, 0) + POINTS_PER_SMILE
                    last_smile_time[assigned_id] = now
                    socketio.emit('points_update', {'face_id': assigned_id, 'points': users_points[assigned_id]})
                    if users_points[assigned_id] >= REWARD_THRESHOLD:
                        socketio.emit('reward', {'face_id': assigned_id,
                                                  'points': users_points[assigned_id],
                                                  'message': f"{assigned_id} earned a reward!"})
                        users_points[assigned_id] = 0
                        socketio.emit('points_update', {'face_id': assigned_id, 'points': 0})

        # Emit frame at ~30 FPS
        now = time.time()
        if now - last_emit >= FRAME_EMIT_INTERVAL:
            last_emit = now
            _, buffer = cv2.imencode('.jpg', frame)
            jpg_as_text = base64.b64encode(buffer).decode('utf-8')
            socketio.emit('frame', {'image': jpg_as_text})

        socketio.sleep(0.01)  # allow SocketIO to handle events

    cap.release()

# --- Start camera thread ---
def start_camera_thread():
    t = threading.Thread(target=camera_loop, daemon=True)
    t.start()
    return t

# --- SocketIO handlers ---
@socketio.on('connect')
def client_connect():
    print("Client connected")

@socketio.on('disconnect')
def client_disconnect():
    print("Client disconnected")

# --- Run server ---
if __name__ == '__main__':
    print("Starting camera thread and Flask app")
    start_camera_thread()
    socketio.run(app, host='0.0.0.0', port=5000, debug=True)
