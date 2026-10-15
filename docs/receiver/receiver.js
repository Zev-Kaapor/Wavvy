// Cast receiver of Wavvy, the screen of the TV while a song plays from the phone

// Temporary, a few lines of log on the screen, the TV has no console
const debugLines = [];
const dbg = (text) => {
    debugLines.unshift(text);
    debugLines.length = Math.min(debugLines.length, 10);
    let box = document.getElementById("dbg");
    if (!box) {
        box = document.createElement("pre");
        box.id = "dbg";
        box.style.cssText = "position:fixed;left:8px;top:8px;margin:0;font:16px monospace;color:#0f0;background:rgba(0,0,0,.6);z-index:9;max-width:60vw";
        document.body.appendChild(box);
    }
    box.textContent = debugLines.join("\n");
};
window.addEventListener("error", (event) => dbg("ERR " + event.message + " @" + event.lineno));

// Tokens
const SeekStepSeconds = 10;
const SeekHoldAccelerateEvery = 4;
const SeekMaxMultiplier = 6;
const ProgressTickMillis = 500;
const DefaultControl = "toggle";
const LocalSender = "wavvy-receiver";

const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();
const messages = cast.framework.messages;

// Elements
const body = document.body;
const backdrop = document.getElementById("backdrop");
const cover = document.getElementById("cover");
const title = document.getElementById("title");
const artist = document.getElementById("artist");
const bar = document.getElementById("bar");
const fill = document.getElementById("fill");
const current = document.getElementById("current");
const total = document.getElementById("total");
const controls = Array.from(document.querySelectorAll(".control"));

// The place of the remote, a control by its index or the bar
let focusIndex = controls.findIndex((control) => control.dataset.action === DefaultControl);
let onBar = false;
let holdCount = 0;

// Time as m:ss
function formatTime(seconds) {
    const whole = Math.max(0, Math.floor(seconds || 0));
    return Math.floor(whole / 60) + ":" + String(whole % 60).padStart(2, "0");
}

// The system of the TV also turns the arrows and the OK key into requests, they are dropped because this page already acts on those keys
[messages.MessageType.PLAY, messages.MessageType.PAUSE, messages.MessageType.SEEK].forEach((type) => {
    playerManager.setMessageInterceptor(type, (request) => (request.senderId || request.requestId ? request : null));
});

// A request of this page, it has a sender so it is not dropped
function sendLocal(request) {
    request.senderId = LocalSender;
    playerManager.sendLocalMediaRequest(request);
}

// What the screen says of the song that plays, the last one stays when the player has nothing
function showSong() {
    const info = playerManager.getMediaInformation();
    if (!info) return;

    const metadata = info.metadata || {};
    const image = metadata.images && metadata.images.length ? metadata.images[0].url : "";

    title.textContent = metadata.title || "";
    artist.textContent = metadata.artist || "";
    cover.hidden = !image;
    if (image) cover.src = image;
    backdrop.style.backgroundImage = image ? "url('" + image + "')" : "none";
}

// The bar and the times
function showProgress() {
    const duration = playerManager.getDurationSec();
    const position = playerManager.getCurrentTimeSec();

    current.textContent = formatTime(position);
    total.textContent = formatTime(duration);
    fill.style.width = duration > 0 ? Math.min(100, (position / duration) * 100) + "%" : "0";
    body.classList.toggle("playing", playerManager.getPlayerState() === cast.framework.messages.PlayerState.PLAYING);
}

// Marks where the remote is
function showFocus() {
    controls.forEach((control, index) => control.classList.toggle("focused", !onBar && index === focusIndex));
    bar.classList.toggle("focused", onBar);
}

// A jump to the next or the previous song of the queue
function jump(step) {
    const request = new messages.QueueUpdateRequestData();
    request.jump = step;
    sendLocal(request);
}

// What a control does
function activate(action) {
    if (action === "toggle") {
        const isPlaying = playerManager.getPlayerState() === messages.PlayerState.PLAYING;
        sendLocal(new messages.RequestData(isPlaying ? messages.MessageType.PAUSE : messages.MessageType.PLAY));
    } else if (action === "next") {
        jump(1);
    } else if (action === "previous") {
        jump(-1);
    }
}

// A jump on the bar, longer the longer the key is held
function seekBy(direction) {
    const multiplier = Math.min(1 + Math.floor(holdCount / SeekHoldAccelerateEvery), SeekMaxMultiplier);
    const duration = playerManager.getDurationSec();
    const target = playerManager.getCurrentTimeSec() + direction * SeekStepSeconds * multiplier;

    const request = new messages.SeekRequestData();
    request.currentTime = Math.max(0, duration > 0 ? Math.min(duration, target) : target);
    sendLocal(request);
    holdCount++;
    showProgress();
}

// The keys of the remote
document.addEventListener("keydown", (event) => {
    dbg("WVDBG key=" + event.key + " code=" + event.keyCode + " repeat=" + event.repeat + " t=" + Math.round(event.timeStamp));
    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
        onBar = event.key === "ArrowDown";
    } else if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
        const direction = event.key === "ArrowRight" ? 1 : -1;
        if (onBar) seekBy(direction); else focusIndex = Math.max(0, Math.min(controls.length - 1, focusIndex + direction));
    } else if (event.key === "Enter" && !onBar) {
        activate(controls[focusIndex].dataset.action);
    } else {
        return;
    }

    event.preventDefault();
    showFocus();
});

document.addEventListener("keyup", () => {
    holdCount = 0;
});

// The song changed or the state of the player did
const onPlayerChange = () => {
    showSong();
    showProgress();
};
playerManager.addEventListener(cast.framework.events.EventType.MEDIA_STATUS, onPlayerChange);
playerManager.addEventListener(cast.framework.events.EventType.MEDIA_INFORMATION_CHANGED, onPlayerChange);

// Temporary, writes what the player does to the log
["PLAYING", "PAUSE", "SEEKING", "SEEKED", "ENDED", "ERROR", "REQUEST_SEEK", "REQUEST_PLAY", "REQUEST_PAUSE", "REQUEST_STOP", "REQUEST_LOAD", "REQUEST_QUEUE_UPDATE", "PLAYER_LOAD_COMPLETE", "CLIP_ENDED", "BUFFERING"].forEach((name) => {
    const type = cast.framework.events.EventType[name];
    if (type) playerManager.addEventListener(type, (event) => dbg("WVDBG event=" + name + " state=" + playerManager.getPlayerState() + " t=" + playerManager.getCurrentTimeSec() + " " + (event.requestData ? "sid=" + event.requestData.senderId + " rid=" + event.requestData.requestId : (event.detailedErrorCode || ""))));
});

setInterval(showProgress, ProgressTickMillis);
showFocus();

// The default screen of the receiver is not used, this page is the screen
context.start();
