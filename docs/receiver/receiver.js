// Cast receiver of Wavvy, the screen of the TV while a song plays from the phone

// Tokens
const SeekStepSeconds = 10;
const SeekHoldAccelerateEvery = 4;
const SeekMaxMultiplier = 6;
const ProgressTickMillis = 500;
const LocalSender = "wavvy-receiver";
const ControlNamespace = "urn:x-cast:com.wavvy.app.control";
const DefaultLanguage = "en";

// How long the mark of the remote stays after the last key
const RemoteIdleMillis = 5000;

// The words of the screen, in the languages of the app
const Strings = {
    en: { loading: "Loading player", heading: "Now Playing" },
    pt: { loading: "Carregando player", heading: "Tocando agora" }
};

const context = cast.framework.CastReceiverContext.getInstance();
const playerManager = context.getPlayerManager();
const messages = cast.framework.messages;

// Elements
const body = document.body;
const art = document.getElementById("art");
const cover = document.getElementById("cover");
const heading = document.getElementById("heading");
const loadingText = document.getElementById("loading-text");
const title = document.getElementById("title");
const artist = document.getElementById("artist");
const like = document.getElementById("like");
const bar = document.getElementById("bar");
const fill = document.getElementById("fill");
const current = document.getElementById("current");
const remaining = document.getElementById("remaining");
const controls = Array.from(document.querySelectorAll("#controls .control"));
const repeatButton = document.querySelector("#controls [data-action=repeat]");
const shuffleButton = document.querySelector("#controls [data-action=shuffle]");
const upNext = document.getElementById("up-next");
const nextCover = document.getElementById("next-cover");
const nextArtist = document.getElementById("next-artist");
const nextTitle = document.getElementById("next-title");

// The rows the remote walks over, from the top, the heart, the bar, the controls and the next song
const Rows = { like: 0, bar: 1, controls: 2, next: 3 };
let row = Rows.controls;
let column = controls.findIndex((control) => control.dataset.action === "toggle");
let holdCount = 0;
let remoteTimer = 0;

// The song on the screen and its length, kept when the player has nothing so the screen does not go blank
let shownId = "";
let shownDuration = 0;

// The language of the phone, the one of the TV until the phone says it
function showLanguage(language) {
    const words = Strings[language] || Strings[(navigator.language || DefaultLanguage).slice(0, 2)] || Strings[DefaultLanguage];
    loadingText.textContent = words.loading;
    heading.textContent = words.heading;
}
showLanguage();

// The cover shows when its picture is there, until then the place waits with its light
cover.addEventListener("load", () => art.classList.add("ready"));
cover.addEventListener("error", () => art.classList.remove("ready"));

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
        art.classList.remove("ready");
        if (image) cover.src = image;
    }
}

// The bar, the time that went and the time that is left
function showProgress() {
    const position = playerManager.getCurrentTimeSec();
    if (playerManager.getDurationSec() > 0) shownDuration = playerManager.getDurationSec();
    const duration = shownDuration;

    current.textContent = formatTime(position);
    remaining.textContent = "-" + formatTime(duration - position);
    fill.style.width = duration > 0 ? Math.min(100, (position / duration) * 100) + "%" : "0";
    const state = playerManager.getPlayerState();
    body.classList.toggle("playing", state === messages.PlayerState.PLAYING || state === messages.PlayerState.BUFFERING);

    // Nothing to show yet, the loading is alone until the first song arrives
    body.classList.toggle("loading", !shownId);
}

// Marks where the remote is
function showFocus() {
    like.classList.toggle("focused", row === Rows.like);
    bar.classList.toggle("focused", row === Rows.bar);
    controls.forEach((control, index) => control.classList.toggle("focused", row === Rows.controls && index === column));
    upNext.classList.toggle("focused", row === Rows.next);
}

// The phone owns the queue, the likes, the repeat and the shuffle, so these buttons are asked to it
function askPhone(action) {
    context.sendCustomMessage(ControlNamespace, undefined, { action });
}

// What the phone says, the repeat, the shuffle, the like of the song and the song that comes next
function applyModes(data) {
    const modes = typeof data === "string" ? JSON.parse(data) : data;
    if (!modes) return;

    showLanguage(modes.language);
    repeatButton.dataset.mode = modes.repeat;
    repeatButton.classList.toggle("off", modes.repeat === "off");
    shuffleButton.classList.toggle("off", !modes.shuffle);
    like.classList.toggle("on", modes.liked === true);

    const next = modes.next;
    upNext.classList.toggle("empty", !next);
    if (next) {
        nextArtist.textContent = next.artist || "";
        nextTitle.textContent = next.title || "";
        if (next.image && nextCover.src !== next.image) nextCover.src = next.image;
    }

    // The next song can go away, the remote then goes back to the controls
    if (!next && row === Rows.next) row = Rows.controls;
    showFocus();
}

// What a button does
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

// The keys of the remote, up and down change the row, left and right move in the controls or on the bar, OK presses
document.addEventListener("keydown", (event) => {
    // The mark of the remote shows while it is used and goes away a few seconds after
    body.classList.add("remote");
    clearTimeout(remoteTimer);
    remoteTimer = setTimeout(() => body.classList.remove("remote"), RemoteIdleMillis);

    const lastRow = upNext.classList.contains("empty") ? Rows.controls : Rows.next;
    if (event.key === "ArrowUp") {
        row = Math.max(Rows.like, row - 1);
    } else if (event.key === "ArrowDown") {
        row = Math.min(lastRow, row + 1);
    } else if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
        const direction = event.key === "ArrowRight" ? 1 : -1;
        if (row === Rows.bar) seekBy(direction);
        else if (row === Rows.controls) column = Math.max(0, Math.min(controls.length - 1, column + direction));
    } else if (event.key === "Enter") {
        if (row === Rows.like) activate("like");
        else if (row === Rows.controls) activate(controls[column].dataset.action);
        else if (row === Rows.next) activate("next");
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

setInterval(showProgress, ProgressTickMillis);
showFocus();

context.addCustomMessageListener(ControlNamespace, (event) => applyModes(event.data));

// The default screen of the receiver is not used, this page is the screen
const options = new cast.framework.CastReceiverOptions();
options.customNamespaces = { [ControlNamespace]: cast.framework.system.MessageType.JSON };
context.start(options);
askPhone("modes");
