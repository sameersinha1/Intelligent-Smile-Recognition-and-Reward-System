// ts/main.ts

// Assuming Socket.IO client types are installed: npm install @types/socket.io-client
import { io, Socket } from 'socket.io-client';

// Define the shape of the data expected from the server
interface PointUpdateData {
    face_id: string;
    points: number;
}

interface RewardData {
    face_id: string;
    message?: string; // Optional custom message
    points: number; // The points value after the reset (usually 0)
}

// Define the structure for the in-memory leaderboard
const points: Record<string, number> = {}; // face_id -> points

// Get DOM elements
const videoImg = document.getElementById('videoStream') as HTMLImageElement | null;
const connectedSpan = document.getElementById('connected') as HTMLSpanElement | null;
const pointsList = document.getElementById('pointsList') as HTMLUListElement | null;

if (!videoImg || !connectedSpan || !pointsList) {
    console.error("One or more required DOM elements are missing. Check index.html IDs.");
} else {
    // Initialize Socket.IO connection
    const socket: Socket = io();

    // --- Socket Event Handlers ---

    socket.on('connect', () => {
        connectedSpan.textContent = 'Yes';
        console.log('Connected to server');
    });

    socket.on('disconnect', () => {
        connectedSpan.textContent = 'No';
        console.log('Disconnected from server');
        // Optionally clear the board on disconnect
        pointsList.innerHTML = '<li>Disconnected.</li>';
    });

    // Receive frame (base64 JPEG)
    socket.on('frame', (data: { image: string }) => {
        // Set <img> src to data URL
        videoImg.src = 'data:image/jpeg;base64,' + data.image;
    });

    // Update points for a face
    socket.on('points_update', (data: PointUpdateData) => {
        const fid = data.face_id;
        points[fid] = data.points;
        renderPoints();
    });

    // Reward event received
    socket.on('reward', (data: RewardData) => {
        const message = data.message || `${data.face_id} earned a reward!`;
        showRewardToast(message);

        // Play audio with browser TTS
        if ('speechSynthesis' in window) {
            const utter = new SpeechSynthesisUtterance(message);
            utter.rate = 1;
            window.speechSynthesis.speak(utter);
        }

        // The server sends the new (reset) point value, so update the record
        points[data.face_id] = data.points;
        renderPoints();
    });

    // --- Utility Functions ---

    function renderPoints(): void {
        if (!pointsList) return;

        pointsList.innerHTML = '';
        
        // Convert to array, sort by points descending
        const items = Object.entries(points).sort(([, ptsA], [, ptsB]) => ptsB - ptsA);
        
        if (items.length === 0) {
            pointsList.innerHTML = '<li>Waiting for faces...</li>';
            return;
        }

        for (const [fid, pts] of items) {
            const li = document.createElement('li');
            li.textContent = `${fid}: ${pts} pts`;
            pointsList.appendChild(li);
        }
    }

    // small toast popup
    function showRewardToast(text: string): void {
        const toast = document.createElement('div');
        toast.className = 'reward-toast';
        toast.textContent = text;
        document.body.appendChild(toast);
        
        // Use requestAnimationFrame for safer DOM manipulation before transition
        requestAnimationFrame(() => {
            toast.classList.add('visible');
        });

        setTimeout(() => {
            toast.classList.remove('visible');
            setTimeout(() => document.body.removeChild(toast), 400); // Wait for CSS transition
        }, 2500);
    }
}