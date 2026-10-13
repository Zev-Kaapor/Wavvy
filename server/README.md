# Wavvy Listen Together server

A small relay for the Listen Together rooms of Wavvy. It runs on [Cloudflare Workers](https://workers.cloudflare.com/) with a Durable Object for each room, and it only passes messages. It never plays, receives or stores any audio: every phone plays the song by itself, and the host tells the others what is playing and where.

## Deploy

You need a free Cloudflare account and Node.js.

```bash
cd server
npm install

# Try it on your computer
npm run dev

# Log in to Cloudflare and publish
npx wrangler login
npm run deploy
```

The deploy prints the address of the Worker, such as `https://wavvy-listen-together.<your-name>.workers.dev`. The app talks to that address, with `wss://` for the connection of a room.

## How a room works

1. The host asks for a room with `POST /rooms` and gets a `code` of 6 letters and a `hostToken`.
2. The host opens the connection `wss://<address>/ws?code=<code>&token=<hostToken>`.
3. A guest opens `wss://<address>/ws?code=<code>&name=<name>`, the code works as the password of the room.
4. Only the host can say what is playing, the guests follow it.

Limits: 8 people in a room, 100 songs in the queue, 40 messages in 10 seconds for each person, a room lives 12 hours, closes at once when the host leaves on purpose and one minute after the connection of the host is lost.

## Messages

Everything is JSON over the WebSocket.

From the app to the server:

- `{"t":"ping","at":<time of the phone>}`, answered with `pong`, to compare the clock of the phone with the one of the server.
- `{"t":"playback","track":{...},"queue":[...],"isPlaying":true,"positionMs":12345}`, only from the host.
- `{"t":"request","requestId":"...","action":"next"}`, only from a guest, to ask the host for `play`, `pause`, `next`, `previous`, `seek` (with `fraction`), `playTrack` (with `track`), `enqueue` or `playNext` (with `tracks`, up to 20 songs). A guest can ask again after 10 seconds.
- `{"t":"decision","requestId":"...","to":"<id of the guest>","accepted":true}`, only from the host.
- `{"t":"leave"}`.

From the server to the app:

- `welcome` when the connection opens, with `you`, `isHost`, `code`, `name`, `people`, `playback` and `serverAt`.
- `people` with the list of the room whenever someone comes or goes.
- `playback` with what the host says, stamped with `serverAt`, the time of the server when it was said.
- `request` to the host, with `requestId`, `from`, `name`, `action` and the `track`, `tracks` or `fraction` that go with it.
- `decision` to the guest that asked, with `accepted`, and `denied` when a request is asked too soon (`cooldown`, with `waitMs`) or when there is no host (`no_host`).
- `pong` and `closed` (with `reason` as `host_left` or `expired`).

A guest knows where the host is in the song with `positionMs + (now of the server - serverAt)` while it is playing.

Errors come as an HTTP status before the connection opens: `404 room_not_found`, `403 room_full`, `400 name_required`.

## Privacy

The server sees the code of the room, the names people chose and the songs that play (their id, title and artist). Nothing is kept after the room closes. See the [Privacy Policy](../PRIVACY.md).
