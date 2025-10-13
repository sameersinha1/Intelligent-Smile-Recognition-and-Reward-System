import base64
import cv2
import time
import threading
from flask import Flask, render_template
from flask_socketio import SocketIO

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*")

face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
smile_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_smile.xml")

users_points = {}
face_positions = {}
last_smile_time = {}
face_id_counter = 0
face_id_lock = threading.Lock()
camera_thread_running = True

FRAME_EMIT_INTERVAL = 0.08
POINTS_PER_SMILE = 10
REWARD_THRESHOLD = 100
SMILE_DEBOUNCE = 2.0
NEW_FRAME_WIDTH = 320
NEW_FRAME_HEIGHT = 240

@app.route('/')
def index():
    return render_template('index.html')

def camera_loop():
    global face_id_counter, camera_thread_running

    cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
    if not cap.isOpened():
        print("ERROR: Could not open camera.")
        return

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 30)

    last_emit = 0
    while camera_thread_running:
        ret, frame = cap.read()
        if not ret:
            print("WARNING: Failed to read frame.")
            break

        frame = cv2.resize(frame, (NEW_FRAME_WIDTH, NEW_FRAME_HEIGHT))
        frame = cv2.flip(frame, 1)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        faces = face_cascade.detectMultiScale(gray, 1.3, 5)
        for (x, y, w, h) in faces:
            assigned_id = None
            for fid, pos in face_positions.items():
                fx, fy, fw, fh = pos
                if abs(fx - x) < w and abs(fy - y) < h:
                    assigned_id = fid
                    break
            if assigned_id is None:
                with face_id_lock:
                    face_id_counter += 1
                    assigned_id = f"User{face_id_counter}"

            face_positions[assigned_id] = (x, y, w, h)

            cv2.rectangle(frame, (x, y), (x+w, y+h), (255, 0, 0), 2)
            cv2.putText(frame, assigned_id, (x, y-10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255,255,255), 1)

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
                        socketio.emit('reward', {'face_id': assigned_id, 'points': users_points[assigned_id], 'message': f"{assigned_id} earned a reward!"})
                        users_points[assigned_id] = 0
                        socketio.emit('points_update', {'face_id': assigned_id, 'points': 0})

        now = time.time()
        if now - last_emit >= FRAME_EMIT_INTERVAL:
            last_emit = now
            success, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 60])
            if success:
                jpg_as_text = base64.b64encode(buffer).decode('utf-8')
                socketio.emit('frame', {'image': jpg_as_text})
                print("Emitting frame to client")

        time.sleep(0.001)

    cap.release()
    print("Camera loop finished.")

def start_camera_thread():
    t = threading.Thread(target=camera_loop, daemon=True)
    t.start()

@socketio.on('connect')
def client_connect():
    print("Client connected")

@socketio.on('disconnect')
def client_disconnect():
    print("Client disconnected")

if __name__ == '__main__':
    print("Starting camera thread and Flask app")
    start_camera_thread()
    socketio.run(app, host='127.0.0.1', port=5000, debug=True, allow_unsafe_werkzeug=True)