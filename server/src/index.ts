// The relay of the Listen Together rooms of Wavvy, it only passes messages and never plays or stores any audio

// The names the bindings of the Worker have
export interface Env {
  ROOMS: DurableObjectNamespace;
}

// The letters of a room code, without the ones that look alike, and how long the code is
const CodeAlphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const CodeLength = 6;
const CodeAttempts = 8;

// The limits of a room, the people in it, the size of a message, the songs in the queue and the letters of the names
const MaxPeople = 8;
const MaxMessageBytes = 32 * 1024;
const MaxQueue = 100;
const MaxPersonName = 24;
const MaxRoomName = 40;
const MaxText = 200;

// How long a room lives, and how long it waits for the host to come back after the connection was lost
const RoomLifeMs = 12 * 60 * 60 * 1000;
const HostGraceMs = 60 * 1000;

// What a guest can ask the host to do, and how long a guest waits before asking again
const RequestActions = new Set(["play", "pause", "next", "previous", "seek", "playTrack", "enqueue", "playNext"]);
const RequestCooldownMs = 10 * 1000;
const MaxRequestTracks = 20;

// How many messages a person can send in a window of time, and how long the window is
const RateLimit = 40;
const RateWindowMs = 10 * 1000;

// What the host says about the playback, and what the guests receive with the time of the server on it
interface Track {
  id: string;
  title: string;
  artist: string;
  thumbnail: string | null;
  durationMs: number;
}

interface Playback {
  track: Track | null;
  queue: Track[];
  isPlaying: boolean;
  positionMs: number;
  serverAt: number;
}

// What is kept of a room while nobody is connected
interface RoomRecord {
  name: string | null;
  hostName: string;
  hostToken: string;
  createdAt: number;
  playback: Playback;
}

// What is kept of each connection while the Durable Object sleeps
interface Person {
  id: string;
  name: string;
  isHost: boolean;
  windowStart: number;
  count: number;
  left?: boolean;
  lastRequestAt?: number;
}

const json = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const randomCode = (): string => {
  const bytes = crypto.getRandomValues(new Uint8Array(CodeLength));
  return Array.from(bytes, (byte) => CodeAlphabet[byte % CodeAlphabet.length]).join("");
};

const cleanText = (value: unknown, max: number): string =>
  typeof value === "string" ? value.replace(/[\u0000-\u001f\u007f]/g, "").trim().slice(0, max) : "";

const normalizeCode = (value: string | null): string => (value ?? "").toUpperCase().replace(/[^A-Z0-9]/g, "");

// The Worker, it makes the rooms and hands each connection to the room it asks for
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") return json({ ok: true });

    // A new room, the Worker tries codes until it finds one that is free
    if (url.pathname === "/rooms" && request.method === "POST") {
      const body = (await request.json().catch(() => ({}))) as { name?: unknown; hostName?: unknown };
      const hostName = cleanText(body.hostName, MaxPersonName);
      if (!hostName) return json({ error: "name_required" }, 400);

      for (let attempt = 0; attempt < CodeAttempts; attempt++) {
        const code = randomCode();
        const room = env.ROOMS.get(env.ROOMS.idFromName(code));
        const answer = await room.fetch("https://room/init", {
          method: "POST",
          body: JSON.stringify({ name: cleanText(body.name, MaxRoomName) || null, hostName }),
        });
        if (answer.status === 409) continue;
        if (!answer.ok) return json({ error: "unavailable" }, 503);
        const { hostToken } = (await answer.json()) as { hostToken: string };
        return json({ code, hostToken });
      }
      return json({ error: "unavailable" }, 503);
    }

    // A connection to a room, the host shows its token, a guest shows only the name
    if (url.pathname === "/ws") {
      if (request.headers.get("upgrade") !== "websocket") return json({ error: "websocket_required" }, 426);
      const code = normalizeCode(url.searchParams.get("code"));
      if (code.length !== CodeLength) return json({ error: "room_not_found" }, 404);
      return env.ROOMS.get(env.ROOMS.idFromName(code)).fetch(request);
    }

    return json({ error: "not_found" }, 404);
  },
};

// A room, which keeps who is in it and the playback of the host and tells everyone when something changes
export class Room implements DurableObject {
  constructor(private readonly ctx: DurableObjectState, private readonly env: Env) {}

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);

    // The Worker makes the room once, a code that is in use answers with a conflict
    if (url.pathname === "/init") {
      if (await this.ctx.storage.get<RoomRecord>("room")) return new Response(null, { status: 409 });
      const data = (await request.json()) as { name: string | null; hostName: string };
      const hostToken = crypto.randomUUID();
      const room: RoomRecord = {
        name: data.name,
        hostName: data.hostName,
        hostToken,
        createdAt: Date.now(),
        playback: { track: null, queue: [], isPlaying: false, positionMs: 0, serverAt: Date.now() },
      };
      await this.ctx.storage.put("room", room);
      await this.ctx.storage.setAlarm(Date.now() + RoomLifeMs);
      return json({ hostToken });
    }

    const room = await this.ctx.storage.get<RoomRecord>("room");
    if (!room) return json({ error: "room_not_found" }, 404);

    const token = url.searchParams.get("token");
    const isHost = token !== null && token === room.hostToken;
    const name = isHost ? room.hostName : cleanText(url.searchParams.get("name"), MaxPersonName);
    if (!name) return json({ error: "name_required" }, 400);

    // The host that comes back takes the place of the old connection
    if (isHost) {
      for (const socket of this.ctx.getWebSockets()) {
        if (this.person(socket)?.isHost) socket.close(1000, "replaced");
      }
    } else if (this.ctx.getWebSockets().length >= MaxPeople) {
      return json({ error: "room_full" }, 403);
    }

    const pair = new WebSocketPair();
    const [client, server] = [pair[0], pair[1]];
    this.ctx.acceptWebSocket(server);

    const person: Person = { id: crypto.randomUUID().slice(0, 8), name, isHost, windowStart: Date.now(), count: 0 };
    server.serializeAttachment(person);

    // The host is back, so the room does not close and lives its whole time again
    if (isHost) await this.ctx.storage.setAlarm(room.createdAt + RoomLifeMs);

    server.send(JSON.stringify({
      t: "welcome",
      you: person.id,
      isHost,
      code: normalizeCode(url.searchParams.get("code")),
      name: room.name,
      people: this.people(),
      playback: room.playback,
      serverAt: Date.now(),
    }));
    this.broadcast({ t: "people", people: this.people() }, server);

    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(socket: WebSocket, message: string | ArrayBuffer): Promise<void> {
    const person = this.person(socket);
    if (!person) return;

    if (typeof message !== "string" || message.length > MaxMessageBytes) {
      socket.close(1009, "message_too_big");
      return;
    }

    // Too many messages in a short time close the connection
    const now = Date.now();
    if (now - person.windowStart > RateWindowMs) {
      person.windowStart = now;
      person.count = 0;
    }
    person.count++;
    socket.serializeAttachment(person);
    if (person.count > RateLimit) {
      socket.close(1008, "too_many_messages");
      return;
    }

    let data: any;
    try {
      data = JSON.parse(message);
    } catch {
      return;
    }

    switch (data?.t) {
      // The clock of the guest is compared with the one of the server to follow the host with no delay
      case "ping":
        socket.send(JSON.stringify({ t: "pong", at: typeof data.at === "number" ? data.at : 0, serverAt: now }));
        break;

      // Only the host says what is playing
      case "playback": {
        if (!person.isHost) return;
        const playback = this.readPlayback(data, now);
        if (!playback) return;
        const room = await this.ctx.storage.get<RoomRecord>("room");
        if (!room) return;
        room.playback = playback;
        await this.ctx.storage.put("room", room);
        this.broadcast({ t: "playback", playback }, socket);
        break;
      }

      // A guest asks the host to do something, one request at a time, and the host answers it
      case "request": {
        if (person.isHost) return;
        const action = typeof data.action === "string" ? data.action : "";
        const requestId = cleanText(data.requestId, 40);
        if (!RequestActions.has(action) || !requestId) return;

        const wait = (person.lastRequestAt ?? 0) + RequestCooldownMs - now;
        if (wait > 0) {
          socket.send(JSON.stringify({ t: "denied", requestId, reason: "cooldown", waitMs: wait }));
          return;
        }

        const host = this.ctx.getWebSockets().find((other) => this.person(other)?.isHost);
        if (!host) {
          socket.send(JSON.stringify({ t: "denied", requestId, reason: "no_host" }));
          return;
        }

        person.lastRequestAt = now;
        socket.serializeAttachment(person);
        const fraction = Number(data.fraction);
        host.send(JSON.stringify({
          t: "request",
          requestId,
          from: person.id,
          name: person.name,
          action,
          fraction: Number.isFinite(fraction) ? Math.min(Math.max(fraction, 0), 1) : null,
          track: this.readTrack(data.track),
          tracks: Array.isArray(data.tracks)
            ? data.tracks.slice(0, MaxRequestTracks).map((value: any) => this.readTrack(value)).filter((track: Track | null): track is Track => track !== null)
            : [],
        }));
        break;
      }

      // The host says yes or no to a request, and only the guest that asked hears it
      case "decision": {
        if (!person.isHost) return;
        const target = this.ctx.getWebSockets().find((other) => this.person(other)?.id === data.to);
        target?.send(JSON.stringify({ t: "decision", requestId: cleanText(data.requestId, 40), accepted: data.accepted === true }));
        break;
      }

      case "leave":
        person.left = true;
        socket.serializeAttachment(person);
        socket.close(1000, "left");
        break;
    }
  }

  async webSocketClose(socket: WebSocket): Promise<void> {
    await this.left(socket);
  }

  async webSocketError(socket: WebSocket): Promise<void> {
    await this.left(socket);
  }

  // The room closes when it is old, or when the host did not come back in time
  async alarm(): Promise<void> {
    const room = await this.ctx.storage.get<RoomRecord>("room");
    const hasHost = this.ctx.getWebSockets().some((socket) => this.person(socket)?.isHost);
    const isOld = room ? Date.now() >= room.createdAt + RoomLifeMs : true;

    // A host that is there keeps the room, which goes on until the end of its life
    if (hasHost && !isOld) {
      if (room) await this.ctx.storage.setAlarm(room.createdAt + RoomLifeMs);
      return;
    }
    this.broadcast({ t: "closed", reason: isOld && hasHost ? "expired" : "host_left" });
    for (const socket of this.ctx.getWebSockets()) socket.close(1000, "closed");
    await this.ctx.storage.deleteAll();
  }

  // Tells the others that someone left, and gives the host a little time to come back
  private async left(socket: WebSocket): Promise<void> {
    const person = this.person(socket);
    if (!person) return;
    // A host that left on purpose closes the room at once, one that lost the connection is waited for
    if (person.isHost) await this.ctx.storage.setAlarm(Date.now() + (person.left ? 1000 : HostGraceMs));
    this.broadcast({ t: "people", people: this.people(socket) });
  }

  private person(socket: WebSocket): Person | null {
    return socket.deserializeAttachment() as Person | null;
  }

  // The people in the room, without the connection that is leaving
  private people(except?: WebSocket) {
    return this.ctx.getWebSockets()
      .filter((socket) => socket !== except)
      .map((socket) => this.person(socket))
      .filter((person): person is Person => person !== null)
      .map((person) => ({ id: person.id, name: person.name, isHost: person.isHost }));
  }

  private broadcast(message: unknown, except?: WebSocket): void {
    const text = JSON.stringify(message);
    for (const socket of this.ctx.getWebSockets()) {
      if (socket !== except) {
        try {
          socket.send(text);
        } catch {
          // A connection that is already closing is left alone
        }
      }
    }
  }

  // Reads a song and keeps only what is valid, nothing else reaches the others
  private readTrack(value: any): Track | null {
    const id = cleanText(value?.id, MaxText);
    if (!id) return null;
    const duration = Number(value?.durationMs);
    return {
      id,
      title: cleanText(value?.title, MaxText),
      artist: cleanText(value?.artist, MaxText),
      thumbnail: typeof value?.thumbnail === "string" && value.thumbnail.startsWith("https://") ? value.thumbnail.slice(0, 500) : null,
      durationMs: Number.isFinite(duration) && duration > 0 ? Math.floor(duration) : 0,
    };
  }

  // Reads what the host sent about the playback
  private readPlayback(data: any, now: number): Playback | null {
    const position = Number(data?.positionMs);
    if (!Number.isFinite(position) || position < 0) return null;

    const queue = Array.isArray(data?.queue) ? data.queue.slice(0, MaxQueue).map((value: any) => this.readTrack(value)).filter((track: Track | null): track is Track => track !== null) : [];
    return {
      track: this.readTrack(data?.track),
      queue,
      isPlaying: data?.isPlaying === true,
      positionMs: Math.floor(position),
      serverAt: now,
    };
  }
}
