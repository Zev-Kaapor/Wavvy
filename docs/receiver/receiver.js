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

// The size of the tiny copy of the cover that makes the backdrop, the page stretches it over the screen
const BackdropWidth = 64;
const BackdropHeight = 36;

// A name that does not fit rolls, how fast and how far apart the two copies of it are, in percent of the height of the screen
const MarqueeVelocityVh = 9;
const MarqueeGapVh = 8;

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
const artistName = document.getElementById("artist-name");
const bar = document.getElementById("bar");
const fill = document.getElementById("fill");
const knob = document.getElementById("knob");
const current = document.getElementById("current");
const total = document.getElementById("total");
const controls = Array.from(document.querySelectorAll("#controls .control"));
const repeatButton = document.querySelector("#controls [data-action=repeat]");
const shuffleButton = document.querySelector("#controls [data-action=shuffle]");

// The rows the remote walks over, the bar on top and the controls under it
const Rows = { bar: 0, controls: 1 };
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

// Draws the cover small, filling the whole canvas, the screen stretches it and so it looks blurred
function paintBackdrop(picture) {
    const paint = backdrop.getContext("2d");
    const scale = Math.max(BackdropWidth / picture.naturalWidth, BackdropHeight / picture.naturalHeight);
    const width = picture.naturalWidth * scale;
    const height = picture.naturalHeight * scale;

    paint.imageSmoothingQuality = "high";
    paint.drawImage(picture, (BackdropWidth - width) / 2, (BackdropHeight - height) / 2, width, height);
}

// Puts a name in its place, and when it is wider than the room it has, it rolls to the side, one copy after the other
function setMarquee(element, text) {
    if (element.dataset.text === text) return;
    element.dataset.text = text;

    const run = element.firstElementChild;
    run.classList.remove("scroll");
    run.textContent = text;

    requestAnimationFrame(() => {
        if (run.scrollWidth <= element.clientWidth) return;

        const unit = window.innerHeight / 100;
        run.textContent = "";
        for (let copy = 0; copy < 2; copy++) {
            const part = document.createElement("span");
            part.textContent = text;
            part.style.paddingRight = MarqueeGapVh * unit + "px";
            run.appendChild(part);
        }

        const shift = run.firstElementChild.offsetWidth;
        run.style.setProperty("--shift", shift + "px");
        run.style.setProperty("--marquee-duration", shift / (MarqueeVelocityVh * unit) + "s");
        run.classList.add("scroll");
    });
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

    setMarquee(title, metadata.title || "");
    setMarquee(artistName, metadata.artist || "");
    if (image !== cover.dataset.shown) {
        cover.dataset.shown = image;
        coverBox.classList.remove("ready");
        if (image) cover.src = image;

        // The backdrop changes when the new picture is there, so it never goes empty between two songs
        if (image) {
            const preload = new Image();
            preload.onload = () => { if (cover.dataset.shown === image) paintBackdrop(preload); };
            preload.src = image;
        }
    }
}

// The bar with its knob, the time that went and the length of the song
function showProgress() {
    const position = playerManager.getCurrentTimeSec();
    if (playerManager.getDurationSec() > 0) shownDuration = playerManager.getDurationSec();
    const duration = shownDuration;

    current.textContent = formatTime(position);
    total.textContent = formatTime(duration);
    const share = duration > 0 ? Math.min(100, (position / duration) * 100) : 0;
    fill.style.width = share + "%";
    knob.style.left = share + "%";
    const state = playerManager.getPlayerState();
    body.classList.toggle("playing", state === messages.PlayerState.PLAYING || state === messages.PlayerState.BUFFERING);

    // Nothing to show yet, the loading is alone until the first song arrives
    body.classList.toggle("loading", !shownId);
}

// Marks where the remote is
function showFocus() {
    bar.classList.toggle("focused", row === Rows.bar);
    controls.forEach((control, index) => control.classList.toggle("focused", row === Rows.controls && index === column));
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
    repeatButton.toggleAttribute("data-on", modes.repeat !== "off");
    shuffleButton.toggleAttribute("data-on", modes.shuffle === true);
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

// The keys of the remote, up goes to the bar and down to the controls, left and right move in the controls or on the bar, OK presses
document.addEventListener("keydown", (event) => {
    // The mark of the remote shows while it is used and goes away a few seconds after
    body.classList.add("remote");
    clearTimeout(remoteTimer);
    remoteTimer = setTimeout(() => body.classList.remove("remote"), RemoteIdleMillis);

    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
        row = event.key === "ArrowUp" ? Rows.bar : Rows.controls;
    } else if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
        const direction = event.key === "ArrowRight" ? 1 : -1;
        if (row === Rows.bar) seekBy(direction); else column = Math.max(0, Math.min(controls.length - 1, column + direction));
    } else if (event.key === "Enter" && row === Rows.controls) {
        activate(controls[column].dataset.action);
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
