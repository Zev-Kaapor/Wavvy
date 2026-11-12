// Cast receiver of Wavvy, the screen of the TV while a song plays from the phone

// Tokens
const SeekStepSeconds = 10;
const SeekHoldAccelerateEvery = 4;
const SeekMaxMultiplier = 6;
const ProgressTickMillis = 500;
const DefaultControl = "toggle";
const LocalSender = "wavvy-receiver";
const ControlNamespace = "urn:x-cast:com.wavvy.app.control";
const DefaultLanguage = "en";

// The color taken from the cover, the size the cover is looked at, how much of each channel there is and what keeps the color readable on the dark screen
const AccentSample = 32;
const ChannelMax = 255;
const AccentMinSaturation = 0.45;
const AccentMinLightness = 0.5;
const AccentMaxLightness = 0.68;
const SoftSaturation = 40;
const SoftLightness = 80;
const SoftAlpha = 0.7;

// A name longer than this is shown smaller so it does not take the room of the bar
const LongTitleLength = 36;

// How long the mark of the remote stays after the last key
const RemoteIdleMillis = 5000;

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
    return String(Math.floor(whole / 60)).padStart(2, "0") + ":" + String(whole % 60).padStart(2, "0");
}

// Hue, saturation and lightness of a color given in channels from 0 to 1
function toHsl(r, g, b) {
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const lightness = (max + min) / 2;
    if (max === min) return { h: 0, s: 0, l: lightness };

    const delta = max - min;
    const saturation = lightness > 0.5 ? delta / (2 - max - min) : delta / (max + min);
    let hue;
    if (max === r) hue = (g - b) / delta + (g < b ? 6 : 0);
    else if (max === g) hue = (b - r) / delta + 2;
    else hue = (r - g) / delta + 4;

    return { h: hue * 60, s: saturation, l: lightness };
}

// The color of the cover that stands out most, the bar, the play button and the tint of the backdrop take it, the default one stays when it cannot be read
let accentFor = "";
function showAccent(url) {
    accentFor = url;
    const reset = () => {
        document.documentElement.style.removeProperty("--accent");
        document.documentElement.style.removeProperty("--accent-soft");
    };
    if (!url) { reset(); return; }

    // The cover is read by its own image, so a server that does not allow it only costs the color and not the picture
    const sample = new Image();
    sample.crossOrigin = "anonymous";
    sample.onerror = reset;
    sample.onload = () => {
        if (accentFor !== url) return;
        try {
            const canvas = document.createElement("canvas");
            canvas.width = AccentSample;
            canvas.height = AccentSample;
            const paint = canvas.getContext("2d");
            paint.drawImage(sample, 0, 0, AccentSample, AccentSample);
            const pixels = paint.getImageData(0, 0, AccentSample, AccentSample).data;

            // The vivid pixels weigh more than the gray ones, so a colored cover gives a colored accent
            let red = 0, green = 0, blue = 0, weight = 0;
            for (let i = 0; i < pixels.length; i += 4) {
                const hsl = toHsl(pixels[i] / ChannelMax, pixels[i + 1] / ChannelMax, pixels[i + 2] / ChannelMax);
                const share = hsl.s * (1 - Math.abs(2 * hsl.l - 1));
                red += pixels[i] * share;
                green += pixels[i + 1] * share;
                blue += pixels[i + 2] * share;
                weight += share;
            }
            if (weight <= 0) { reset(); return; }

            const mean = toHsl(red / weight / ChannelMax, green / weight / ChannelMax, blue / weight / ChannelMax);
            const saturation = Math.max(mean.s, AccentMinSaturation);
            const lightness = Math.min(Math.max(mean.l, AccentMinLightness), AccentMaxLightness);
            document.documentElement.style.setProperty("--accent", "hsl(" + Math.round(mean.h) + ", " + Math.round(saturation * 100) + "%, " + Math.round(lightness * 100) + "%)");

            // The artist and the controls take a pale tone of the same color
            document.documentElement.style.setProperty("--accent-soft", "hsla(" + Math.round(mean.h) + ", " + SoftSaturation + "%, " + SoftLightness + "%, " + SoftAlpha + ")");
        } catch (error) {
            reset();
        }
    };
    sample.src = url;
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
    title.classList.toggle("long", title.textContent.length > LongTitleLength);
    artist.textContent = metadata.artist || "";
    if (image !== cover.dataset.shown) {
        cover.dataset.shown = image;
        coverBox.classList.remove("ready");
        if (image) cover.src = image;
        showAccent(image);
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

    // Nothing to show yet, the loading is alone until the first song arrives
    body.classList.toggle("loading", !shownId);
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
let remoteTimer = 0;
document.addEventListener("keydown", (event) => {
    // The mark of the remote shows while it is used and goes away a few seconds after
    body.classList.add("remote");
    clearTimeout(remoteTimer);
    remoteTimer = setTimeout(() => body.classList.remove("remote"), RemoteIdleMillis);

    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
        onBar = event.key === "ArrowUp";
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

setInterval(showProgress, ProgressTickMillis);
showFocus();

context.addCustomMessageListener(ControlNamespace, (event) => applyModes(event.data));

// The default screen of the receiver is not used, this page is the screen
const options = new cast.framework.CastReceiverOptions();
options.customNamespaces = { [ControlNamespace]: cast.framework.system.MessageType.JSON };
context.start(options);
askPhone("modes");
