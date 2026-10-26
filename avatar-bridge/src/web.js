import { SimliClient, LogLevel } from "simli-client";

// Plays Ari's recorded greeting through one short Simli session, so the recorder can capture it as video.
window.AriLive = {
  async start({ sessionToken, video, audio, greeting, onLive, onEnd }) {
    const client = new SimliClient(sessionToken, video, audio, null, LogLevel.ERROR, "livekit");
    let ended = false;
    let feeding = null;
    let finishTimer = null;
    let hasSpoken = false;

    async function end(reason) {
      if (ended) return;
      ended = true;
      clearInterval(feeding);
      clearTimeout(finishTimer);
      try { await client.stop(); } catch (_) { /* Session is already closed. */ }
      video.srcObject = null;
      audio.srcObject = null;
      onEnd(reason);
    }

    client.on("start", () => {
      onLive();
      let offset = 0;
      // Feed the renderer at playback speed: 100 ms of 16 kHz PCM16 per tick.
      feeding = setInterval(() => {
        if (offset >= greeting.length) {
          clearInterval(feeding);
          feeding = null;
          return;
        }
        client.sendAudioData(greeting.slice(offset, offset + 3200));
        offset += 3200;
      }, 100);
    });
    client.on("speaking", () => {
      hasSpoken = true;
      clearTimeout(finishTimer);
    });
    // Pauses between sentences also report silence, so finish only after the whole greeting was sent.
    client.on("silent", () => {
      if (!hasSpoken || feeding !== null) return;
      finishTimer = setTimeout(() => end("done"), 1200);
    });
    client.on("stop", () => end("done"));
    client.on("error", () => end("error"));
    client.on("startup_error", () => end("error"));

    video.addEventListener("loadedmetadata", () => video.play().catch(() => {}), { once: true });
    audio.addEventListener("loadedmetadata", () => audio.play().catch(() => {}), { once: true });
    try {
      await client.start();
    } catch (_) {
      await end("error");
    }
    return () => end("stopped");
  }
};
