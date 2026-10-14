# Privacy Policy

Wavvy is a free and open-source music app for Android. This page says what the app does with your data.

**In short:** Wavvy has no account of its own, no ads and no analytics. The only server it has is a small relay for the rooms of Listen Together, which does not keep anything after a room ends. What the app keeps stays on your phone, or in your own Google Drive if you turn the backup on.

## What stays on your device

- Your history of what you listened to, the songs you pinned, the lyrics you picked, your searches and your settings.
- The songs you downloaded.
- If you sign in, your name, your user name, your profile photo and the address of your channel banner, so they show without a connection.

None of this is sent to the developer or to any server owned by the developer.

## Services the app talks to

Like any client of other services, the app connects to them to work. Only what each one needs is sent.

- **YouTube Music**: to search, play and show songs, albums, artists and playlists. If you sign in, your requests carry your account, as in the official site.
- **Google sign in**: the sign in happens in a page of Google shown inside the app. The app does not see your password.
- **Lyrics services**: the title and the artist of the song, to find its lyrics.
- **Google Translate**: the text of the lyrics, only when you turn the translation on.
- **SponsorBlock**: a short code made from the song, to find the parts that people marked, only when you turn it on.
- **Radios.com.br and the stations**: when you open the traditional radios, the app reads the lists and the address of the sound of each station from the site, in a hidden page of the browser of the system, as an ordinary visitor would, and the site may keep its own cookies in that page on your phone. When a station plays, its own server receives your address on the internet, as with any radio on the internet.
- **GitHub**: to look for a newer version of the app and download it, only when you allow it.
- **The Listen Together relay**: only when you make or join a room, see the next part.

## Listen Together

Listen Together is optional and only works while you are in a room. Each phone plays the songs by itself, and the relay only passes small messages between the phones. It never receives or stores any audio.

- **What the relay sees while the room is open:** the code of the room, the name of the room, the names the people in it chose, and what the host is playing (the id, the title, the artist and the link of the cover of the songs, the place in the song and the queue). It also passes on what a guest asks the host to do.
- **What it keeps:** the room, with its name, the host and what is playing, is kept only until the room ends, which happens when the host leaves, when the connection of the host is lost for a minute or when 12 hours pass. After that everything is deleted.
- **Who runs it:** a small program written for Wavvy, in the [`server`](server) folder of the project, running on [Cloudflare Workers](https://workers.cloudflare.com/). Cloudflare, as the company that runs the servers, may keep its own technical records, such as the address of the connection, as described in its privacy policy. The developer does not read the rooms.
- **The name you give** is only the name other people in the room see. A signed in user's name is the name of the account, and a guest types one.
- Anyone who has the code can join the room, so share it only with the people you want to listen with.

## Google Drive backup

The backup is optional and off until you connect it.

- When you connect, Google asks you to allow one permission, `drive.appdata`. It gives the app access to a **private folder of the app inside your own Drive**. The app cannot see, read or change any other file of your Drive.
- The app saves a single file with the data listed in "What stays on your device", except the downloaded songs and your sign in. You can lock it with a password that only you know. A backup made by itself has no password, so it is protected only by your Google account.
- The file is stored in your Drive, with your space, and is read only to put your data back on your phone. It is never sent anywhere else, and the developer has no access to it.
- You can disconnect at any time in the settings of the backup, and you can delete the file in the settings of your Google Drive.

Wavvy's use and transfer of information received from Google APIs adheres to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.

## Discord activity

The Discord activity is optional and off until you connect it, after a warning about the risk.

- When you connect, Discord asks you to allow one permission, `sdk.social_layer_presence`, which only lets the app set your activity. The login goes through your browser, and the keys stay on your phone, left out of the backups.
- While a song plays, the app sends Discord what you chose to show, the title, the artist, the time, the address of the cover and the buttons. Nothing is sent when nothing plays or when you turn it off.
- Discord is not an official integration, so using it can go against its rules. You use it at your own risk.
- You can disconnect at any time in the Discord settings of the app, which also revokes the keys.

## Children

Wavvy is not made for children under 13 and does not knowingly collect data from them.

## Changes

If this policy changes, the new version is published on this page.

## Contact

Open an issue at https://github.com/Zev-Kaapor/Wavvy/issues.
