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
const ControlNamespace = "urn:x-cast:com.wavvy.app.control";
const LoadingUntilSeconds = 1.5;
const DefaultLanguage = "en";

// The words of the screen, in the languages of the app
const Strings = {
    en: { loading: "Loading player" },
    pt: { loading: "Carregando player" }
};

const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();
const messages = cast.framework.messages;

// Elements
const body = document.body;
const backdrop = document.getElementById("backdrop");
const cover = document.getElementById("cover");
const coverBox = document.getElementById("cover-box");
const loadingText = document.getElementById("loading-text");
const title = document.getElementById("title");
const artist = document.getElementById("artist");
const bar = document.getElementById("bar");
const fill = document.getElementById("fill");
const current = document.getElementById("current");
const total = document.getElementById("total");
const controls = Array.from(document.querySelectorAll(".control"));
const repeatButton = document.querySelector("[data-action=repeat]");
const shuffleButton = document.querySelector("[data-action=shuffle]");

// The place of the remote, a control by its index or the bar
let focusIndex = controls.findIndex((control) => control.dataset.action === DefaultControl);
let onBar = false;
let holdCount = 0;

// The song on the screen and its length, kept when the player has nothing so the screen does not go blank
let shownId = "";
let shownDuration = 0;

// The language of the phone, the one of the TV until the phone says it
function showLanguage(language) {
    const words = Strings[language] || Strings[(navigator.language || DefaultLanguage).slice(0, 2)] || Strings[DefaultLanguage];
    loadingText.textContent = words.loading;
}
showLanguage();

// The cover shows when its picture is there, until then the square waits with its light
cover.addEventListener("load", () => coverBox.classList.add("ready"));
cover.addEventListener("error", () => coverBox.classList.remove("ready"));

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

    if (info.contentId !== shownId) {
        shownId = info.contentId;
        shownDuration = 0;
    }

    const metadata = info.metadata || {};
    const image = metadata.images && metadata.images.length ? metadata.images[0].url : "";

    title.textContent = metadata.title || "";
    artist.textContent = metadata.artist || "";
    if (image !== cover.dataset.shown) {
        cover.dataset.shown = image;
        coverBox.classList.remove("ready");
        if (image) cover.src = image;
    }
    backdrop.style.backgroundImage = image ? "url('" + image + "')" : "none";
}

// The bar and the times
function showProgress() {
    const position = playerManager.getCurrentTimeSec();
    if (playerManager.getDurationSec() > 0) shownDuration = playerManager.getDurationSec();
    const duration = shownDuration;

    current.textContent = formatTime(position);
    total.textContent = formatTime(duration);
    fill.style.width = duration > 0 ? Math.min(100, (position / duration) * 100) + "%" : "0";
    const state = playerManager.getPlayerState();
    body.classList.toggle("playing", state === messages.PlayerState.PLAYING || state === messages.PlayerState.BUFFERING);

    // Nothing to show yet, or a song that is still being fetched
    body.classList.toggle("loading", !shownId || (state === messages.PlayerState.BUFFERING && position < LoadingUntilSeconds));
}

// Marks where the remote is
function showFocus() {
    controls.forEach((control, index) => control.classList.toggle("focused", !onBar && index === focusIndex));
    bar.classList.toggle("focused", onBar);
}

// The phone owns the queue, the repeat and the shuffle, so these buttons are asked to it
function askPhone(action) {
    context.sendCustomMessage(ControlNamespace, undefined, { action });
}

// How the phone says the repeat and the shuffle are
function applyModes(data) {
    const modes = typeof data === "string" ? JSON.parse(data) : data;
    if (!modes) return;

    showLanguage(modes.language);
    repeatButton.dataset.mode = modes.repeat;
    repeatButton.classList.toggle("off", modes.repeat === "off");
    shuffleButton.classList.toggle("off", !modes.shuffle);
}

// What a control does
function activate(action) {
    if (action === "toggle") {
        const state = playerManager.getPlayerState();

        // A song that ended has nothing to resume, the phone plays it again
        if (state === messages.PlayerState.IDLE) askPhone("replay");
        else sendLocal(new messages.RequestData(state === messages.PlayerState.PLAYING ? messages.MessageType.PAUSE : messages.MessageType.PLAY));
    } else {
        askPhone(action);
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
const queueSummary = () => {
    const manager = playerManager.getQueueManager();
    const items = manager.getItems() || [];
    return items.map((item) => ((item.media && item.media.metadata && item.media.metadata.title) || "?").slice(0, 7)).join("|") + " cur=" + manager.getCurrentItemIndex();
};
["ENDED", "ERROR", "REQUEST_STOP", "REQUEST_LOAD", "REQUEST_QUEUE_UPDATE", "PLAYER_LOAD_COMPLETE", "CLIP_ENDED"].forEach((name) => {
    const type = cast.framework.events.EventType[name];
    if (type) playerManager.addEventListener(type, (event) => dbg("WVDBG event=" + name + " state=" + playerManager.getPlayerState() + " t=" + playerManager.getCurrentTimeSec() + " q=" + queueSummary() + " " + (event.requestData ? "sid=" + event.requestData.senderId + " rid=" + event.requestData.requestId : (event.detailedErrorCode || ""))));
});

setInterval(showProgress, ProgressTickMillis);
showFocus();

context.addCustomMessageListener(ControlNamespace, (event) => applyModes(event.data));

// The default screen of the receiver is not used, this page is the screen
const options = new cast.framework.CastReceiverOptions();
options.customNamespaces = { [ControlNamespace]: cast.framework.system.MessageType.JSON };
context.start(options);
askPhone("modes");
