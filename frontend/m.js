// static/main.js
(() => {
  const socket = io();

  const videoImg = document.getElementById('videoStream');
  const connectedSpan = document.getElementById('connected');
  const pointsList = document.getElementById('pointsList');

  // In-memory points dictionary mirrored on frontend
  const points = {}; // face_id -> points

  socket.on('connect', () => {
    connectedSpan.textContent = 'Yes';
    console.log('Connected to server');
  });

  socket.on('disconnect', () => {
    connectedSpan.textContent = 'No';
    console.log('Disconnected from server');
  });

  // Receive frame (base64 JPEG)
  socket.on('frame', (data) => {
    // set <img> src to data URL
    videoImg.src = 'data:image/jpeg;base64,' + data.image;
  });

  // Update points for a face
  socket.on('points_update', (data) => {
    const fid = data.face_id;
    points[fid] = data.points;
    renderPoints();
  });

  // Reward event received
  socket.on('reward', (data) => {
    // show popup, play audio with browser TTS
    const message = data.message || `${data.face_id} earned a reward!`;
    showRewardToast(message);
    if ('speechSynthesis' in window) {
      const utter = new SpeechSynthesisUtterance(message);
      utter.rate = 1;
      window.speechSynthesis.speak(utter);
    }
    // ensure points list updates (server also sends reset points_update)
    points[data.face_id] = data.points || 0;
    renderPoints();
  });

  function renderPoints() {
    pointsList.innerHTML = '';
    // Sort by points descending
    const items = Object.entries(points).sort((a,b) => b[1] - a[1]);
    for (const [fid, pts] of items) {
      const li = document.createElement('li');
      li.textContent = `${fid}: ${pts}`;
      pointsList.appendChild(li);
    }
  }

  // small toast popup
  function showRewardToast(text) {
    const toast = document.createElement('div');
    toast.className = 'reward-toast';
    toast.textContent = text;
    document.body.appendChild(toast);
    setTimeout(() => {
      toast.classList.add('visible');
    }, 50);
    setTimeout(() => {
      toast.classList.remove('visible');
      setTimeout(() => document.body.removeChild(toast), 400);
    }, 2500);
  }

})();
