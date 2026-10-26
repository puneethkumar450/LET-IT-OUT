import { SimliClient, LogLevel } from "simli-client";

const video = document.getElementById("avatar-video");
const canvas = document.getElementById("avatar-canvas");
const canvasContext = canvas.getContext("2d", { willReadFrequently: true });
const audio = document.getElementById("avatar-audio");
let client = null;
let generation = 0;
let pendingAudio = new Uint8Array(0);
let finishing = false;
let isSpeaking = false;
let hasSpoken = false;
let silentTimer = null;
let frameTimer = null;
let readyTimer = null;
let paintedReady = false;
let lastFrameDiagnostics = 0;

function stopPainting() {
  if (frameTimer !== null) clearInterval(frameTimer);
  frameTimer = null;
  if (readyTimer !== null) clearTimeout(readyTimer);
  readyTimer = null;
  paintedReady = false;
  lastFrameDiagnostics = 0;
  canvasContext?.clearRect(0, 0, canvas.width, canvas.height);
}

function frameAppearance() {
  const pixels = canvasContext.getImageData(0, 0, canvas.width, canvas.height).data;
  let minimum = 255;
  let maximum = 0;
  let dark = 0;
  let sampled = 0;
  for (let row = 24; row < canvas.height - 24; row += 6) {
    for (let column = 24; column < canvas.width - 24; column += 6) {
      const index = (row * canvas.width + column) * 4;
      const light = Math.round((pixels[index] * 299 + pixels[index + 1] * 587 + pixels[index + 2] * 114) / 1000);
      minimum = Math.min(minimum, light);
      maximum = Math.max(maximum, light);
      if (light < 90) dark += 1;
      sampled += 1;
    }
  }
  return { minimum, maximum, dark, sampled };
}

// Probe a small frame occasionally. The video element itself owns visible playback.
function paintFrame(current) {
  if (current !== generation || !client || !canvasContext) return;
  if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA && video.videoWidth && video.videoHeight) {
    try {
      canvasContext.drawImage(video, 0, 0, video.videoWidth, video.videoHeight, 0, 0, canvas.width, canvas.height);
      if (!paintedReady) {
        const appearance = frameAppearance();
        if (performance.now() - lastFrameDiagnostics > 1000) {
          lastFrameDiagnostics = performance.now();
          window.AndroidAvatar?.onFrameDiagnostics?.(
            `${video.videoWidth}x${video.videoHeight} luma=${appearance.minimum}-${appearance.maximum} dark=${appearance.dark}/${appearance.sampled}`
          );
        }
        if (appearance.maximum - appearance.minimum < 70 || appearance.dark < appearance.sampled / 100) return;
        paintedReady = true;
        if (readyTimer !== null) clearTimeout(readyTimer);
        readyTimer = null;
      }
      window.AndroidAvatar?.onFrame?.(canvas.toDataURL("image/jpeg", 0.75).split(",")[1]);
    } catch (_) {
      notify("onFailure");
    }
  }
}

function cancelSilentTimer() {
  if (silentTimer !== null) clearTimeout(silentTimer);
  silentTimer = null;
}

function maybeFinish() {
  if (!finishing || !hasSpoken || isSpeaking || pendingAudio.length !== 0 || silentTimer !== null) return;
  silentTimer = setTimeout(() => {
    silentTimer = null;
    if (finishing && !isSpeaking && pendingAudio.length === 0) {
      finishing = false;
      notify("onSilent");
    }
  }, 400);
}

// Android TTS may synthesize faster than playback. Feed the renderer at audio speed.
setInterval(() => {
  if (!client || pendingAudio.length === 0) return;
  const count = Math.min(3200, pendingAudio.length); // 100 ms of 16 kHz PCM16
  client.sendAudioData(pendingAudio.slice(0, count));
  pendingAudio = pendingAudio.slice(count);
  maybeFinish();
}, 100);

function notify(method) {
  window.AndroidAvatar?.[method]?.();
}

async function stop() {
  generation += 1;
  stopPainting();
  pendingAudio = new Uint8Array(0);
  finishing = false;
  isSpeaking = false;
  hasSpoken = false;
  cancelSilentTimer();
  const previous = client;
  client = null;
  if (previous) {
    try { await previous.stop(); } catch (_) { /* Session is already closed. */ }
  }
  video.srcObject = null;
  audio.srcObject = null;
}

video.addEventListener("loadedmetadata", () => video.play().catch(() => {}));
audio.addEventListener("loadedmetadata", () => audio.play().catch(() => {}));

window.avatarBridge = {
  async start(sessionToken) {
    await stop();
    const current = generation;
    try {
      const next = new SimliClient(sessionToken, video, audio, null, LogLevel.INFO, "livekit");
      client = next;
      next.on("start", () => {
        if (current !== generation || frameTimer !== null) return;
        frameTimer = setInterval(() => paintFrame(current), 125);
        paintFrame(current);
      });
      next.on("speaking", () => {
        if (current !== generation) return;
        isSpeaking = true;
        hasSpoken = true;
        cancelSilentTimer();
        notify("onSpeaking");
      });
      next.on("silent", () => {
        if (current !== generation) return;
        isSpeaking = false;
        maybeFinish();
      });
      next.on("stop", () => { if (current === generation) notify("onFailure"); });
      next.on("error", () => { if (current === generation) notify("onFailure"); });
      next.on("startup_error", () => { if (current === generation) notify("onFailure"); });
      readyTimer = setTimeout(() => {
        readyTimer = null;
        if (current === generation && !paintedReady) notify("onFailure");
      }, 20000);
      await next.start();
      if (current !== generation) await next.stop();
    } catch (_) {
      if (current === generation) notify("onFailure");
    }
  },
  sendAudio(encoded) {
    if (!client) return;
    const raw = atob(encoded);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    const combined = new Uint8Array(pendingAudio.length + bytes.length);
    combined.set(pendingAudio);
    combined.set(bytes, pendingAudio.length);
    pendingAudio = combined;
    cancelSilentTimer();
  },
  finishAudio() { finishing = true; maybeFinish(); },
  clear() {
    pendingAudio = new Uint8Array(0);
    finishing = false;
    isSpeaking = false;
    hasSpoken = false;
    cancelSilentTimer();
    client?.ClearBuffer();
  },
  stop
};
