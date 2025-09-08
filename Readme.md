# Intelligent Smile Recognition and Reward System

AI-powered system that detects smiles from a camera (or uploaded images/video) and triggers a configurable **reward** (points, coupons, badges, confetti, webhook, etc.). Built with a clean **frontend / backend** separation and MLOps-ready training pipeline.

---

## ✨ Key Features

- 🎯 **Real-time smile detection** (webcam or image/video upload)
- 🧠 **Deep learning models** (pretrained face detector + classifier; easy to retrain)
- 🏆 **Reward engine** (points, streaks, badges; pluggable rules)
- 👤 **User sessions** (optional auth for persistent scoring)
- 📊 **Analytics** (per-user totals, session stats, export CSV)
- ⚙️ **Config via `.env`** (thresholds, data paths, reward weights)
- 🧪 **Testable** (unit tests for critical flows)
- 🧭 **Branch strategy:**  
  - `frontend` — UI only  
  - `backend` — API/model only  
  - `main` — integration (optional)

---

## 🧱 Architecture

integrated/
├─ frontend/ # React / Next.js
├─ backend/ # FastAPI / Flask / Node.js
└─ README.md

---

## 🧰 Tech Stack (suggested)

- **Frontend**: React or Next.js, TailwindCSS, Webcam API  
- **Backend**: FastAPI (Python) or Node/Express  
- **Model**: OpenCV face detection + CNN/ResNet classifier (exportable to ONNX)  
- **Storage**: SQLite/Postgres (scores, rewards, users)  
- **Testing**: PyTest / Jest  

---

## 🚦 Branch Strategy

- `frontend` → only `frontend/` folder  
- `backend` → only `backend/` folder  
- `main` → integration branch (optional, both folders)  

---

## 🔐 Environment Variables

**Backend `.env` example**

MIT License

Copyright (c) 2025 Sameer Sinha

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.