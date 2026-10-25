import { SimliClient, LogLevel } from "simli-client";

const video = document.getElementById("avatar-video");
const audio = document.getElementById("avatar-audio");
let client = null;
let generation = 0;
let pendingAudio = new Uint8Array(0);
let finishing = false;
let isSpeaking = false;
let hasSpoken = false;
let silentTimer = null;

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
      next.on("start", () => { if (current === generation) notify("onReady"); });
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
