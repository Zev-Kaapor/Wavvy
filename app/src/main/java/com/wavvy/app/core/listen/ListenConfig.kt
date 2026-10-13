package com.wavvy.app.core.listen

// The address of the relay of the Listen Together rooms, the Worker of the folder server of the project
const val ListenServerAddress = "https://wavvy-listen-together.wavvylistentogether.workers.dev"

// How the connection and the sync behave, how long the keepalive of the connection is, how often the clock is compared and how often the host says where it is
const val ListenKeepAliveSeconds = 25L
const val ListenClockSeconds = 8L
const val ListenHeartbeatSeconds = 5L

// A change of what plays waits this long so several changes together make one message, and a guest only follows when it is this far from the host
const val ListenDebounceMillis = 250L
const val ListenDriftMillis = 1_500L

// How long a guest waits before asking the host for something again, and how long the host has to answer before it counts as a no
const val ListenRequestCooldownMillis = 5_000L
const val ListenRequestTimeoutMillis = 15_000L

// How many times the app tries to come back to a room when the connection is lost, how long it waits before the first try and the longest it waits between two
const val ListenReconnectAttempts = 6
const val ListenReconnectFirstMillis = 2_000L
const val ListenReconnectLongestMillis = 15_000L

// How many songs a request to add to the queue can carry
const val ListenRequestTracks = 20

// How many songs after the current one the host tells the guests, and the length of a code of a room
const val ListenQueueShared = 50
const val ListenCodeLength = 6
