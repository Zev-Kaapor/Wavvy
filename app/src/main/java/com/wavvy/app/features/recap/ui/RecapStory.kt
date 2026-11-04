package com.wavvy.app.features.recap.ui

// Android context, pictures and text utilities
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import java.io.File
// Drawing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
// Animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Time and numbers
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
// Lifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.recap.data.RecapPart
import com.wavvy.app.features.home.ui.components.ItemThumbnail
import com.wavvy.app.features.recap.data.RecapAudio
import com.wavvy.app.features.playlist.data.PlaylistPrivacy
import com.wavvy.app.features.playlist.data.PlaylistRepository
import com.wavvy.app.features.profile.ui.ProfileRanking
import com.wavvy.app.features.profile.ui.TopArtist
import com.wavvy.app.features.profile.ui.durationText
import com.wavvy.app.features.recap.data.DayPart
import com.wavvy.app.features.recap.data.Recap
import com.wavvy.app.features.recap.data.RecapRepository
import com.wavvy.app.features.recap.data.RecapReview
import com.wavvy.app.features.search.data.SearchRepository

// How many months a year has
private const val MonthsInYear = 12

// A whole turn in degrees
private const val FullTurnDegrees = 360f

// Milliseconds in a minute and an hour, and the hours in a day, and how many times a hundred makes a whole
private const val MillisPerMinute = 60_000L
private const val MillisPerHour = 3_600_000L
private const val HoursPerDay = 24
private const val Percent = 100

// The units the time with an artist is told in, from months to minutes, and how many of them are written at most
private const val MinutesPerHour = 60
private const val DaysPerWeek = 7
private const val DaysPerMonth = 30
private const val MaxDurationUnits = 4

// Where each step of the words about the time with the favorite artist begins, in minutes, from a first minute to more than a hundred thousand
private val ArtistMinuteSteps = intArrayOf(1, 60, 180, 600, 1500, 3000, 6000, 10000, 20000, 30000, 50000, 100000)

// Where each step of the words about the plays of the most played song begins, from one play to more than a thousand
private val SongPlaySteps = intArrayOf(1, 2, 3, 5, 10, 20, 30, 50, 75, 100, 150, 200, 300, 500, 750, 1000, 1001)

// How many places the lists of the story show, how many artists stand on the podium and how many genres a stack has, and how many releases that were missed are listed
private const val StoryTop = 10
private const val MissedListed = 5
private const val TimelineArtists = 5
private const val PodiumPlaces = 3

// The shapes of the podium are the ones the page of the third artist had, and how many pages the podium took the place of, after the first one
private const val PodiumVariant = 5
private const val PodiumMerged = 2
private const val TopArtistVariant = 2
private const val TimelineEmptyVariant = RecapDimens.DecorCalm

// The two pages that changed places keep the shapes each one had
private const val TopArtistsVariant = 8
private const val TimelineVariant = 6
// The page of the days in a row takes an arrangement with a shape low in the page, where there are no words
private const val DaysVariant = 0
// The arrangement the first kind of listener starts from, the next kinds take the ones after it
private const val ListenerFirstVariant = 9



private const val PodiumDots = 3
private const val StackGenres = 5

// Where the pictures to share are kept, how well they are saved, and the end of the name the file provider answers by
private const val ShareFolder = "recap"
private const val ShareQuality = 100
private const val FileAuthority = ".files"

// Shows every page that is made, the ones for no data too, so they can be checked one by one, to be turned off when the checking is done
private const val IsReviewing = true

// How many songs go to the playlist of the year
private const val PlaylistSongs = 100

// The base of the digits of the year
private const val DigitBase = 10

// The least songs that make a tease worth telling
private const val MinimumForTease = 2

// Where each step of the words about how much time was listened to begins, in minutes, from the first minute to months of music
private val MinutesSteps = longArrayOf(0, 60, 300, 1000, 3000, 6000, 10000, 20000, 40000, 70000, 100000, 200000)

// Where each step of the words about how many days had music begins, from none to every day of the year
private val DaysSteps = intArrayOf(0, 1, 2, 3, 5, 7, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 350, 365)

// Where each step of the words about how many artists begins: none, one, some, then from a hundred up to more than a hundred thousand
private val TeaseSteps = intArrayOf(
    0, 1, 2, 100, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000,
    20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000, 100001
)

// How a listener is told by the numbers: how many times each song comes back, how much of the time one artist takes, and how many artists tell an adventurer
private const val ReplayerPlays = 4f
private const val SpecialistShare = 0.3f
private const val AdventurerPlays = 2f
private const val AdventurerArtists = 30

// How many days in a row tell a streaker, how many years back the favorite music is for a nostalgic one and how near for a trendy one, and how many albums tell a collector
private const val StreakerDays = 30
private const val NostalgicYears = 15
private const val TrendyYears = 1
private const val CollectorAlbums = 10

// The pages of the story in the order they come, the ones whose numbers are missing are left out
private enum class RecapPage { Intro, ArtistsTease, TopArtist, Newcomer, Podium, Timeline, TimelineEmpty, TopArtists, TopArtistsEmpty, SongsTease, SongsTeaseEmpty, SongsNewcomer, TopSongsEmpty, Digging, AlbumsTease, AlbumsPodium, TopAlbums, AlbumsTimeline, AlbumsTeaseEmpty, AlbumsNewcomer, TopAlbumsEmpty, AlbumsTimelineEmpty, SongsPodium, SongsTimeline, SongsTimelineEmpty, TopSong, TopSongs, Genres, Album, Minutes, Comparison, BiggestDay, Months, Clock, Days, MusicAge, Missed, Listener, Outro }

// One page of the story
private class Step(val page: RecapPage, val listener: Listener? = null)

// The year in music as pages that rise one after the other, each one flat with its own colors and its words coming in
@Composable
fun RecapScreen(year: Int, part: RecapPart, onBack: () -> Unit, onDetails: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recap by produceState<Recap?>(initialValue = null, year, part) { value = RecapRepository.load(context, year, part).let { if (IsReviewing) RecapReview.fill(it) else it } }

    // The photos of the first artists, the ones already known show at once
    val artists by produceState(initialValue = emptyList<TopArtist>(), recap) {
        val first = recap?.artists?.take(StoryTop)?.let { ProfileRanking.withKnownPhotos(context, it) } ?: return@produceState
        value = first
        val looked = ProfileRanking.withPhotos(context, SearchRepository(context), first.filterNot { it.isLookedUp })
        value = first.map { artist -> looked.firstOrNull { it.name == artist.name } ?: artist }
    }

    val current = recap
    val steps = remember(current) { stepsOf(current) }
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val theme = RecapColors.Theme

    // Goes to a page with a smooth slide, from a scope of its own, so the effect that asks for it starting again halfway never cuts the slide short
    val goTo: (Int) -> Unit = { target ->
        if (target in steps.indices) scope.launch { pagerState.animateScrollToPage(target, animationSpec = tween(RecapDimens.PageMillis, easing = FastOutSlowInEasing)) }
    }

    // A tap goes to the page at once, with no slide, since what the user wants is to see it quickly
    val jumpTo: (Int) -> Unit = { target ->
        if (target in steps.indices) scope.launch { pagerState.scrollToPage(target) }
    }

    // The story goes on by itself, the bar of the page fills and then the next page comes, a finger held on the screen stops it
    val progress = remember { Animatable(0f) }
    // The time of the podium, which goes on while the story is stopped, since stopping only keeps the pages from changing by themselves
    val podiumTime = remember { Animatable(0f) }
    var isHeld by remember { mutableStateOf(false) }
    // The button of the top turns the going on by itself off, the pages then only change by a tap or a swipe
    var isStopped by rememberSaveable { mutableStateOf(false) }
    var shownPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pagerState.currentPage, isHeld, isStopped, current) {
        if (current == null) return@LaunchedEffect
        val page = pagerState.currentPage
        if (shownPage != page) {
            shownPage = page
            progress.snapTo(0f)
            podiumTime.snapTo(0f)
        }
        if (isHeld || isStopped || page >= steps.lastIndex) return@LaunchedEffect
        // The bar of the podium follows its own time, which may have gone on while the story was stopped
        if (isPodium(steps[page].page)) progress.snapTo(podiumTime.value / durationOf(steps[page].page, current))

        progress.animateTo(1f, tween(((1f - progress.value) * durationOf(steps[page].page, current)).toInt(), easing = LinearEasing))
        goTo(page + 1)
    }

    // The podium goes on by itself when it is the page on the screen, a finger held on the screen holds it, and stopping the story does not
    LaunchedEffect(pagerState.currentPage, shownPage, isHeld, current) {
        val page = steps.getOrNull(pagerState.currentPage)?.page
        if (current == null || page == null || !isPodium(page) || shownPage != pagerState.currentPage || isHeld) return@LaunchedEffect
        val total = durationOf(page, current).toFloat()
        podiumTime.animateTo(total, tween((total - podiumTime.value).toInt(), easing = LinearEasing))
    }

    // The picture of each page on the screen, kept to be shared
    val layers = remember { mutableMapOf<Int, GraphicsLayer>() }

    // The music: the song of the user is paused while the story plays and comes back when it ends, and the music stops when the app is left
    val audio = remember { RecapAudio(context) }
    // The last thing asked of the music by the podium, the next one waits for it
    val podiumSongs = remember { arrayOfNulls<Job>(1) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val wasPlaying = PlayerConnection.isPlaying.value
        if (wasPlaying) PlayerConnection.togglePlayPause()
        onDispose {
            audio.release()
            if (wasPlaying && !PlayerConnection.isPlaying.value) PlayerConnection.togglePlayPause()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) audio.pause() else if (event == Lifecycle.Event.ON_RESUME) audio.resume()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(isMuted) { audio.setMuted(isMuted) }

    // Each page asks for the song that fits it, and the song of the page after it is made ready while this one is on the screen
    // The artists are known by who they are, so a photo that is found later does not ask for the songs again
    LaunchedEffect(pagerState.currentPage, artists.map { it.id }, current) {
        if (current == null) return@LaunchedEffect
        val page = pagerState.currentPage
        when (val cue = cueOf(steps.getOrNull(page), artists, current)) {
            Cue.Ambient -> audio.playAmbient()
            is Cue.Artist -> audio.playArtist(cue.id)
            is Cue.Song -> audio.playSong(cue.id)
            Cue.Keep -> Unit
        }
        // The podium makes ready the songs it plays by itself
        val next = if (steps.getOrNull(page)?.page?.let { isPodium(it) } == true) Cue.Keep else cueOf(steps.getOrNull(page + 1), artists, current)
        when (next) {
            is Cue.Artist -> audio.prepareArtist(next.id)
            is Cue.Song -> audio.prepareSong(next.id)
            Cue.Ambient -> audio.prepareAmbient()
            Cue.Keep -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize().background(theme.background)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val step = steps[index]
            CompositionLocalProvider(LocalRecapTheme provides RecapColors.Theme) {
                StoryPage(
                    variant = variantOf(steps, index),
                    isCurrent = pagerState.currentPage == index,
                    // A quick tap on the end of the screen goes forward and on the start goes back, a long press only holds the page
                    onTap = { isEnd -> jumpTo(if (isEnd) index + 1 else index - 1) },
                    onHold = { isHeld = it },
                    onLayer = { layers[index] = it }
                ) { isOn ->
                    if (current != null) {
                        when (step.page) {
                            RecapPage.Intro -> IntroPage(current, isOn)
                            RecapPage.ArtistsTease -> ArtistsTeasePage(current, isOn)
                            RecapPage.TopArtist -> artists.firstOrNull()?.let { TopArtistPage(it, current, isOn) }
                            RecapPage.Newcomer -> NewcomerPage(isOn)
                            RecapPage.SongsNewcomer -> NewcomerPage(isOn, R.string.recap_songs_newcomer_title, R.string.recap_songs_newcomer_welcome, R.string.recap_songs_newcomer_next)
                            RecapPage.Podium, RecapPage.SongsPodium, RecapPage.AlbumsPodium -> {
                                // The same podium tells the artists, the songs and the albums, the songs and the albums have their covers and their own words
                                val isSongs = step.page != RecapPage.Podium
                                val entries = remember(step.page, artists, current) {
                                    when (step.page) {
                                        RecapPage.SongsPodium -> current.songs.take(PodiumPlaces).map { PodiumEntry(it.song.id, it.song.title, it.song.artworkUrl, it.total, current.songMonths[it.song.id].orEmpty()) }
                                        // The song of an album that was played the most is the one that plays when the album comes up
                                        RecapPage.AlbumsPodium -> current.albums.take(PodiumPlaces).map { album ->
                                            PodiumEntry(current.songs.firstOrNull { it.song.albumId == album.id }?.song?.id, album.name, album.cover, album.totalMs, current.albumMonths[album.id].orEmpty())
                                        }
                                        else -> artists.map { PodiumEntry(it.id, it.name, it.picture, it.totalMs, current.artistMonths[it.name].orEmpty()) }
                                    }
                                }
                                val places = minOf(
                                    PodiumPlaces,
                                    when (step.page) {
                                        RecapPage.SongsPodium -> current.songs.size
                                        RecapPage.AlbumsPodium -> current.albums.size
                                        else -> current.artists.size
                                    }
                                )
                                val total = podiumMillis(places).toFloat()
                                val isCurrent = pagerState.currentPage == index && shownPage == index
                                PodiumPage(
                                    entries = entries,
                                    count = places,
                                    isRound = !isSongs,
                                    texts = when (step.page) {
                                        RecapPage.SongsPodium -> SongsPodiumTexts
                                        RecapPage.AlbumsPodium -> AlbumsPodiumTexts
                                        else -> ArtistsPodiumTexts
                                    },
                                    isCurrent = isCurrent,
                                    // The time of the page is the share of the bar that is full, a page not shown yet has not begun and one that was shown before is all done
                                    elapsed = {
                                        when {
                                            !isOn -> 0f
                                            pagerState.currentPage == index -> if (shownPage == index) podiumTime.value else 0f
                                            else -> total
                                        }
                                    },
                                    // A song is made ready or played, one after the other so the two never use the same player
                                    onSong = { entry, isPlaying ->
                                        val before = podiumSongs[0]
                                        podiumSongs[0] = scope.launch {
                                            before?.join()
                                            entry.id?.let { id ->
                                                when {
                                                    isSongs && isPlaying -> audio.playSong(id)
                                                    isSongs -> audio.prepareSong(id)
                                                    isPlaying -> audio.playArtist(id)
                                                    else -> audio.prepareArtist(id)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            RecapPage.Timeline -> TimelinePage(current, isOn)
                            RecapPage.SongsTimeline -> SongsTimelinePage(current, isOn)
                            RecapPage.TimelineEmpty -> TimelineEmptyPage(isOn)
                            RecapPage.SongsTimelineEmpty -> TimelineEmptyPage(isOn, R.string.recap_songs_timeline_title, R.string.recap_songs_timeline_empty_caption, R.string.recap_songs_timeline_empty_footer)
                            RecapPage.TopArtists -> TopArtistsPage(artists, isOn)
                            RecapPage.TopArtistsEmpty -> TopArtistsEmptyPage(isOn)
                            RecapPage.TopSongsEmpty -> TopArtistsEmptyPage(isOn, R.string.recap_songs_title, R.string.recap_songs_empty_list_caption, R.string.recap_songs_empty_list_footer)
                            RecapPage.SongsTease -> {
                                // The numbers the most played song did not reach, one by one, up to the one it did, which the next page tells
                                val decoys = songDecoys(current.songs.firstOrNull()?.plays ?: 0)
                                val body = if (decoys.isEmpty()) {
                                    stringResource(R.string.recap_songs_tease_plain)
                                } else {
                                    val nots = decoys.map { stringResource(R.string.recap_songs_not, it) }
                                    stringResource(R.string.recap_songs_tease_body, nots.joinToString(", "))
                                }
                                val count = countUp(current.songs.size, isOn)
                                TeasePage(goldWords(stringResource(R.string.recap_songs_tease, count), count.toString()), body, isOn)
                            }
                            RecapPage.AlbumsTease -> {
                                // The same words of the songs for the albums, with the plays of the first album as the number it did reach
                                val decoys = songDecoys(current.albums.firstOrNull()?.plays ?: 0)
                                val body = if (decoys.isEmpty()) {
                                    stringResource(R.string.recap_albums_tease_plain)
                                } else {
                                    val nots = decoys.map { stringResource(R.string.recap_songs_not, it) }
                                    stringResource(R.string.recap_albums_tease_body, nots.joinToString(", "))
                                }
                                val count = countUp(current.albums.size, isOn)
                                TeasePage(goldWords(stringResource(R.string.recap_albums_tease, count), count.toString()), body, isOn)
                            }
                            RecapPage.Digging -> DiggingPage(isOn, R.string.recap_digging_title, R.string.recap_digging_body, R.string.recap_digging_footer)
                            RecapPage.TopAlbums -> TopAlbumsPage(current, isOn)
                            RecapPage.AlbumsTimeline -> AlbumsTimelinePage(current, isOn)
                            RecapPage.AlbumsTeaseEmpty -> TeasePage(goldWords(stringResource(R.string.recap_albums_empty_title), "0"), stringResource(R.string.recap_albums_empty_body), isOn)
                            RecapPage.AlbumsNewcomer -> NewcomerPage(isOn, R.string.recap_albums_newcomer_title, R.string.recap_albums_newcomer_welcome, R.string.recap_albums_newcomer_next)
                            RecapPage.TopAlbumsEmpty -> TopArtistsEmptyPage(isOn, R.string.recap_albums_title, R.string.recap_albums_empty_list_caption, R.string.recap_albums_empty_list_footer)
                            RecapPage.AlbumsTimelineEmpty -> TimelineEmptyPage(isOn, R.string.recap_albums_timeline_title, R.string.recap_albums_timeline_empty_caption, R.string.recap_albums_timeline_empty_footer)
                            RecapPage.SongsTeaseEmpty -> TeasePage(goldWords(stringResource(R.string.recap_songs_empty_title), "0"), stringResource(R.string.recap_songs_empty_body), isOn)
                            RecapPage.TopSong -> TopSongPage(current, isOn)
                            RecapPage.TopSongs -> TopSongsPage(current, isOn)
                            RecapPage.Genres -> GenresPage(current, isOn)
                            RecapPage.Album -> AlbumPage(current, isOn)
                            RecapPage.Minutes -> MinutesPage(current, isOn)
                            RecapPage.Comparison -> ComparisonPage(current, isOn)
                            RecapPage.BiggestDay -> BiggestDayPage(current, isOn)
                            RecapPage.Months -> MonthsPage(current, isOn)
                            RecapPage.Clock -> ClockPage(current, isOn)
                            RecapPage.Days -> DaysPage(current, isOn)
                            RecapPage.MusicAge -> MusicAgePage(current, isOn)
                            RecapPage.Missed -> MissedPage(current, isOn)
                            RecapPage.Listener -> ListenerPage(current, isOn, step.listener)
                            RecapPage.Outro -> OutroPage(current, isOn, onDetails)
                        }
                    }
                }
            }
        }

        // The bars on top show how far the story went, and the cross leaves it
        CompositionLocalProvider(LocalRecapTheme provides theme) {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(RecapDimens.ProgressGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RecapDimens.ProgressSide)
                        .padding(top = RecapDimens.ProgressTop)
                ) {
                    repeat(steps.size) { index ->
                        // The bars before the page are full, the one of the page fills as the time goes, the ones after are empty
                        val last = steps.lastIndex
                        ProgressBar(
                            fraction = { if (index < pagerState.currentPage || (index == last && pagerState.currentPage == last)) 1f else if (index == pagerState.currentPage) progress.value else 0f },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // The cross leaves on one side, and at the other the sound is turned off and on and the page is shared as a picture
                Row(modifier = Modifier.fillMaxWidth().padding(RecapDimens.ProgressSide), horizontalArrangement = Arrangement.SpaceBetween) {
                    RoundButton(icon = WavvyIcons.Close, description = stringResource(R.string.cd_close), onClick = onBack)
                    Row(horizontalArrangement = Arrangement.spacedBy(RecapDimens.ProgressSide)) {
                        RoundButton(
                            icon = if (isStopped) WavvyIcons.Play else WavvyIcons.Pause,
                            description = stringResource(if (isStopped) R.string.recap_resume else R.string.recap_pause),
                            onClick = { isStopped = !isStopped }
                        )
                        RoundButton(
                            icon = if (isMuted) WavvyIcons.VolumeOff else WavvyIcons.VolumeUp,
                            description = stringResource(if (isMuted) R.string.recap_sound_off else R.string.recap_sound_on),
                            onClick = { isMuted = !isMuted }
                        )
                        if (current != null && !current.isEmpty) {
                            RoundButton(
                                icon = WavvyIcons.Share,
                                description = stringResource(R.string.player_share),
                                onClick = { layers[pagerState.currentPage]?.let { layer -> scope.launch { shareLayer(context, layer, pagerState.currentPage) } } }
                            )
                        }
                    }
                }
            }
        }
    }
}

// The pages the numbers have something for, a year with nothing listened to has only the first one
private fun stepsOf(recap: Recap?): List<Step> {
    if (recap == null) return listOf(Step(RecapPage.Intro))
    // A year with nothing listened to has the first page, the one that says so, the one for who just arrived and the timeline with nothing in it, the pages of the artists are skipped
    if (recap.isEmpty) return listOf(Step(RecapPage.Intro), Step(RecapPage.ArtistsTease), Step(RecapPage.Newcomer), Step(RecapPage.TopArtistsEmpty), Step(RecapPage.TimelineEmpty), Step(RecapPage.SongsTeaseEmpty), Step(RecapPage.SongsNewcomer), Step(RecapPage.TopSongsEmpty), Step(RecapPage.SongsTimelineEmpty), Step(RecapPage.AlbumsTeaseEmpty), Step(RecapPage.AlbumsNewcomer), Step(RecapPage.TopAlbumsEmpty), Step(RecapPage.AlbumsTimelineEmpty), Step(RecapPage.Minutes), Step(RecapPage.Days), Step(RecapPage.Listener, Listener.Visitor), Step(RecapPage.Outro))

    return buildList {
        // While the pages are checked one by one, the ones made for no data come after the ones of its kind, the ones that are already there are not repeated
        fun review(vararg pages: RecapPage) {
            if (IsReviewing) pages.filter { page -> none { it.page == page } }.forEach { add(Step(it)) }
        }

        add(Step(RecapPage.Intro))
        add(Step(RecapPage.ArtistsTease))
        // The first three artists come up on a podium before the page of the first one, which has only one artist to show
        if (recap.artists.size > 1) add(Step(RecapPage.Podium))
        add(Step(RecapPage.TopArtist))
        if (recap.artists.size > 1) add(Step(RecapPage.TopArtists))
        // The timeline needs only one artist, who has none sees the one made for that, which is in the list of a year with nothing listened to
        if (recap.artistMonths.isNotEmpty()) add(Step(RecapPage.Timeline))
        review(RecapPage.Newcomer, RecapPage.TopArtistsEmpty, RecapPage.TimelineEmpty)
        // The songs go the way the artists went: the words that bring them, the podium, the first one, the list of ten and the months of the first ones
        if (recap.songs.size >= MinimumForTease) add(Step(RecapPage.SongsTease))
        if (recap.songs.size > 1) add(Step(RecapPage.SongsPodium))
        add(Step(RecapPage.TopSong))
        if (recap.songs.size > 1) add(Step(RecapPage.TopSongs))
        if (recap.songMonths.isNotEmpty()) add(Step(RecapPage.SongsTimeline))
        review(RecapPage.SongsTeaseEmpty, RecapPage.SongsNewcomer, RecapPage.TopSongsEmpty, RecapPage.SongsTimelineEmpty)
        // The albums go the same way, and the genres come after their months, as the last of these
        if (recap.albums.size >= MinimumForTease) add(Step(RecapPage.AlbumsTease))
        if (recap.albums.size > 1) add(Step(RecapPage.AlbumsPodium))
        if (recap.albums.isNotEmpty()) add(Step(RecapPage.Album))
        if (recap.albums.size > 1) add(Step(RecapPage.TopAlbums))
        if (recap.albumMonths.isNotEmpty()) add(Step(RecapPage.AlbumsTimeline))
        review(RecapPage.AlbumsTeaseEmpty, RecapPage.AlbumsNewcomer, RecapPage.TopAlbumsEmpty, RecapPage.AlbumsTimelineEmpty)
        if (recap.genres.isNotEmpty()) add(Step(RecapPage.Genres))
        // Where the albums or the genres are not known yet, which comes from a search that fills them little by little, one page says so instead of leaving a hole
        if (recap.albums.isEmpty() || recap.genres.isEmpty()) add(Step(RecapPage.Digging))
        review(RecapPage.Digging)
        add(Step(RecapPage.Minutes))
        if (recap.changeFromPrevious != null) add(Step(RecapPage.Comparison))
        if (recap.biggestDay != null) add(Step(RecapPage.BiggestDay))
        add(Step(RecapPage.Months))
        add(Step(RecapPage.Clock))
        add(Step(RecapPage.Days))
        if (recap.musicAge != null) add(Step(RecapPage.MusicAge))
        if (recap.missed.isNotEmpty()) add(Step(RecapPage.Missed))
        // While the pages are checked every kind of listener has its own page
        if (IsReviewing) Listener.entries.forEach { add(Step(RecapPage.Listener, it)) } else add(Step(RecapPage.Listener, listenerOf(recap)))
        add(Step(RecapPage.Outro))
    }
}

// What a page plays: a friendly song of the app, a song of the artist it names, the song it names, or what already plays
private sealed interface Cue {
    data object Ambient : Cue
    data object Keep : Cue
    data class Artist(val id: String) : Cue
    data class Song(val id: String) : Cue
}

private fun cueOf(step: Step?, artists: List<TopArtist>, recap: Recap): Cue = when (step?.page) {
    // An artist whose page is not known yet leaves what plays alone
    RecapPage.TopArtist, RecapPage.TopArtists, RecapPage.Timeline -> artists.firstOrNull()?.id?.let { Cue.Artist(it) } ?: Cue.Keep
    RecapPage.TopSong, RecapPage.TopSongs, RecapPage.SongsTimeline -> recap.songs.firstOrNull()?.let { Cue.Song(it.song.id) } ?: Cue.Keep
    RecapPage.Album, RecapPage.TopAlbums, RecapPage.AlbumsTimeline -> recap.songs.firstOrNull { it.song.albumId == recap.albums.firstOrNull()?.id }?.let { Cue.Song(it.song.id) } ?: Cue.Keep
    null -> Cue.Keep
    else -> Cue.Ambient
}

// The numbers of plays the most played song did not reach, the first time, the half and the one before the real one, from the least to the most
private fun songDecoys(plays: Int): List<Int> = listOf(1, plays / 2, plays - 1).filter { it in 1 until plays }.distinct().sorted()

// How long a page stays before the story goes on by itself, the podium has a time of its own
private fun durationOf(page: RecapPage, recap: Recap): Int = when (page) {
    RecapPage.Podium -> podiumMillis(minOf(PodiumPlaces, recap.artists.size))
    RecapPage.SongsPodium -> podiumMillis(minOf(PodiumPlaces, recap.songs.size))
    RecapPage.AlbumsPodium -> podiumMillis(minOf(PodiumPlaces, recap.albums.size))
    else -> RecapDimens.StoryMillis
}

// The pages that come up place by place on a podium, which go on by themselves even when the story is stopped
private fun isPodium(page: RecapPage): Boolean = page == RecapPage.Podium || page == RecapPage.SongsPodium || page == RecapPage.AlbumsPodium

// Which arrangement of shapes a page takes, the pages after the podium keep the ones they had when the three artists had a page each
private fun variantOf(steps: List<Step>, index: Int): Int {
    val podium = steps.indexOfFirst { it.page == RecapPage.Podium }
    return when {
        index == podium -> PodiumVariant
        steps[index].page == RecapPage.TopArtist -> TopArtistVariant
        steps[index].page == RecapPage.TimelineEmpty || steps[index].page == RecapPage.SongsTimelineEmpty || steps[index].page == RecapPage.AlbumsTimelineEmpty -> TimelineEmptyVariant
        steps[index].page == RecapPage.TopArtists -> TopArtistsVariant
        steps[index].page == RecapPage.TopArtistsEmpty || steps[index].page == RecapPage.TopSongsEmpty || steps[index].page == RecapPage.TopAlbumsEmpty || steps[index].page == RecapPage.Album ||
            steps[index].page == RecapPage.Digging -> RecapDimens.DecorCalm
        steps[index].page == RecapPage.SongsTease || steps[index].page == RecapPage.SongsTeaseEmpty || steps[index].page == RecapPage.AlbumsTease || steps[index].page == RecapPage.AlbumsTeaseEmpty -> RecapDimens.DecorSongsTease
        steps[index].page == RecapPage.TopSong || steps[index].page == RecapPage.SongsNewcomer || steps[index].page == RecapPage.AlbumsNewcomer -> RecapDimens.DecorSong
        steps[index].page == RecapPage.TopSongs || steps[index].page == RecapPage.TopAlbums || steps[index].page == RecapPage.Months -> RecapDimens.DecorTopSongs
        steps[index].page == RecapPage.Minutes -> RecapDimens.DecorMinutes
        steps[index].page == RecapPage.Missed -> RecapDimens.DecorMissed
        steps[index].page == RecapPage.BiggestDay -> RecapDimens.DecorDay
        steps[index].page == RecapPage.Genres -> RecapDimens.DecorGenres
        steps[index].page == RecapPage.Timeline || steps[index].page == RecapPage.SongsTimeline || steps[index].page == RecapPage.AlbumsTimeline || steps[index].page == RecapPage.Comparison -> TimelineVariant
        steps[index].page == RecapPage.Days -> DaysVariant
        steps[index].page == RecapPage.MusicAge -> RecapDimens.DecorAge
        steps[index].page == RecapPage.Listener -> listenerVariant(steps[index].listener)
        steps[index].page == RecapPage.SongsPodium || steps[index].page == RecapPage.AlbumsPodium -> PodiumVariant
        podium in 0 until index -> index + PodiumMerged
        else -> index
    }
}

// A page flat with its colors and its shapes, the words come in once it is the one on the screen
@Composable
private fun StoryPage(
    variant: Int,
    isCurrent: Boolean,
    onTap: (Boolean) -> Unit,
    onHold: (Boolean) -> Unit,
    onLayer: (GraphicsLayer) -> Unit,
    content: @Composable ColumnScope.(Boolean) -> Unit
) {
    val theme = LocalRecapTheme.current
    var isOn by remember { mutableStateOf(false) }
    LaunchedEffect(isCurrent) { if (isCurrent) isOn = true }

    // What the page draws is also kept in a layer, which is what becomes the picture to share
    val layer = rememberGraphicsLayer()
    SideEffect { onLayer(layer) }

    // The page is cut at its own edges, so what a shape pushes out of the screen never shows over the page beside it
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
            .background(theme.background)
            // A press that ends before the hold time is a tap, on the end side or on the start side, one that goes on only holds the page and does nothing when it ends
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isHold = true
                    val up = withTimeoutOrNull(RecapDimens.HoldMillis) {
                        waitForUpOrCancellation().also { isHold = false }
                    }
                    if (isHold) {
                        onHold(true)
                        waitForUpOrCancellation()
                        onHold(false)
                    } else if (up != null) {
                        onTap(down.position.x > size.width / 2f)
                    }
                }
            }
    ) {
        Decor(variant)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = RecapDimens.PageSide)
        ) {
            content(isOn)
        }
    }
}

// A round button over the page, with the color of its words
@Composable
private fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val ink = LocalRecapTheme.current.ink

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(RecapDimens.RoundButton)
            .clip(CircleShape)
            .background(ink.copy(alpha = RecapColors.ButtonAlpha))
            .clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = ink, modifier = Modifier.size(RecapDimens.RoundButtonIcon))
    }
}

// Takes room off the top and the bottom of what it wraps, for the empty space a font keeps around a big line, the line itself is not touched
private fun Modifier.trimmed(top: Dp, bottom: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val cutTop = top.roundToPx()
    val cutBottom = bottom.roundToPx()
    layout(placeable.width, (placeable.height - cutTop - cutBottom).coerceAtLeast(0)) { placeable.place(0, -cutTop) }
}

// A bar of the top of the story, filled up to the share it is given, which is read only while drawing
@Composable
private fun ProgressBar(fraction: () -> Float, modifier: Modifier = Modifier) {
    val ink = LocalRecapTheme.current.ink

    Box(
        modifier = modifier
            .height(RecapDimens.ProgressHeight)
            .clip(CircleShape)
            .background(ink.copy(alpha = RecapColors.TrackAlpha))
            .drawBehind { drawRect(ink, size = Size(size.width * fraction(), size.height)) }
    )
}

// Saves the page as a picture and opens the sharing sheet of the system with it
private suspend fun shareLayer(context: Context, layer: GraphicsLayer, index: Int) {
    val bitmap = layer.toImageBitmap().asAndroidBitmap()
    val uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, ShareFolder).apply { mkdirs() }
        val file = File(folder, "${context.getString(R.string.app_name).lowercase()}-recap-$index.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, ShareQuality, it) }
        FileProvider.getUriForFile(context, "${context.packageName}$FileAuthority", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

// Brings its words in rising, after the wait of its step
@Composable
private fun Reveal(isOn: Boolean, step: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = isOn,
        modifier = modifier,
        enter = fadeIn(tween(RecapDimens.RevealMillis, delayMillis = step * RecapDimens.RevealStep)) +
            slideInVertically(tween(RecapDimens.RevealMillis, delayMillis = step * RecapDimens.RevealStep)) { it / RecapDimens.RevealRise },
        exit = ExitTransition.None
    ) { content() }
}

// A number that counts up to its value once the page shows
@Composable
private fun countUp(target: Int, isOn: Boolean): Int {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isOn, target) {
        if (isOn) progress.animateTo(1f, tween(RecapDimens.CountMillis, delayMillis = RecapDimens.RevealStep, easing = FastOutSlowInEasing))
    }
    return (target * progress.value).toInt()
}

// The small line over the big words of a page
@Composable
private fun Lead(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontSize = RecapDimens.LeadSize, fontWeight = FontWeight.SemiBold), color = LocalRecapTheme.current.softInk, textAlign = TextAlign.Center)
}

// The line under the big words
@Composable
private fun Caption(text: String) = Caption(AnnotatedString(text))

// The same, with some words of it in gold
@Composable
private fun Caption(text: AnnotatedString) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold), color = LocalRecapTheme.current.softInk, textAlign = TextAlign.Center)
}

// A text with the given words in gold, the first time each one shows in it, the ones that are not in it are left out, so the numbers and the times that matter stand out
private fun goldWords(text: String, vararg words: String): AnnotatedString = buildAnnotatedString {
    append(text)
    words.forEach { word ->
        val start = text.indexOf(word)
        if (word.isNotEmpty() && start >= 0) addStyle(SpanStyle(color = RecapColors.Gold), start, start + word.length)
    }
}

// A word of the scale of a chart placed so its middle sits on the line at that part of the height, from the top line to the bottom one, which keep a margin the thickness of the line away from the edges
private fun Modifier.onScale(share: Float, margin: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val edge = margin.roundToPx() / 2
    val line = edge + ((constraints.maxHeight - edge * 2) * share).roundToInt()
    layout(placeable.width, constraints.maxHeight) { placeable.place(0, line - placeable.height / 2) }
}

// A name or a title, big
@Composable
private fun Name(text: String, isShining: Boolean = false, color: Color = LocalRecapTheme.current.ink) = Name(AnnotatedString(text), isShining, color)

// The same, with some words of it in gold
@Composable
private fun Name(text: AnnotatedString, isShining: Boolean = false, color: Color = LocalRecapTheme.current.ink) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.NameSize, lineHeight = RecapDimens.NameLine, fontWeight = FontWeight.ExtraBold),
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = if (isShining) Modifier.shine(RecapColors.Shine) else Modifier
    )
}

// A light that crosses what it covers slowly, with a long wait before the next time, as a reflection
@Composable
private fun Modifier.shine(color: Color): Modifier {
    val progress = rememberInfiniteTransition(label = "Shine").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RecapDimens.ShineSweepMillis, delayMillis = RecapDimens.ShinePauseMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shine"
    )
    val clear = color.copy(alpha = 0f)

    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()

        // The light only shows over the words, from before the start to past the end
        val band = size.width * RecapDimens.ShineBand
        val start = -band + (size.width + band) * progress.value
        drawRect(
            brush = Brush.linearGradient(listOf(clear, color, clear), start = Offset(start, 0f), end = Offset(start + band, band * RecapDimens.ShineTilt)),
            blendMode = BlendMode.SrcAtop
        )
    }
}

// A very big number
@Composable
private fun Big(text: String, color: Color = LocalRecapTheme.current.ink) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(fontSize = RecapDimens.BigSize, lineHeight = RecapDimens.BigLine, fontWeight = FontWeight.Black),
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

// A sentence that sets up the next page, bold and in the middle
@Composable
private fun TeasePage(text: String, caption: String, isOn: Boolean) = TeasePage(AnnotatedString(text), caption, isOn)

// The same, with some words of the sentence in gold
@Composable
private fun TeasePage(text: AnnotatedString, caption: String, isOn: Boolean) {
    Reveal(isOn) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.TeaseSize, lineHeight = RecapDimens.TeaseLine, fontWeight = FontWeight.ExtraBold),
            color = LocalRecapTheme.current.ink,
            textAlign = TextAlign.Center
        )
    }
    Reveal(isOn, step = 2) { Caption(caption) }
}

// The tease of the favorite artist, in words that change with how many artists were listened to: many, some, a few, one or none
@Composable
private fun ArtistsTeasePage(recap: Recap, isOn: Boolean) {
    val theme = LocalRecapTheme.current
    val count = recap.artistCount
    // The big sentence and the one under it, both from the step of the ladder that fits how many artists there were
    val step = TeaseSteps.indexOfLast { count >= it }.coerceAtLeast(0)
    val shown = countUp(count, isOn)
    val number = NumberFormat.getIntegerInstance().format(shown)
    val heading = goldWords(String.format(stringArrayResource(R.array.recap_tease_headings)[step], number), number)
    val body = stringArrayResource(R.array.recap_tease_bodies)[step]

    // The words go to the start of the page, a big sentence and then the one that goes with it, as two blocks
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.TeaseGap), modifier = Modifier.fillMaxWidth()) {
        Reveal(isOn) {
            Text(
                text = heading,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.TeaseSize, lineHeight = RecapDimens.TeaseLine, fontWeight = FontWeight.ExtraBold),
                color = theme.ink
            )
        }
        Reveal(isOn, step = 2) { Text(text = body, style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold), color = theme.softInk) }
    }
}

// The year with its digits rolling down like a roulette, fast at first and slower until they stop on the right ones
@Composable
private fun RollingYear(year: Int, isOn: Boolean, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isOn) {
        if (isOn) progress.animateTo(1f, tween(RecapDimens.YearRollMillis, delayMillis = RecapDimens.RevealStep, easing = LinearOutSlowInEasing))
    }

    // Every digit rolls, the ones on the right go by more digits than the ones on the left, and only what is inside the trimmed room of the number shows
    val digits = year.toString().map { it.digitToInt() }
    // The year fades in while the digits start to roll, so the zeros do not show up all at once
    Row(modifier = Modifier.graphicsLayer { alpha = (progress.value / RecapDimens.YearFadeShare).coerceAtMost(1f) }.clipToBounds().then(modifier)) {
        digits.forEachIndexed { place, digit ->
            // Every digit starts on zero and goes round whole turns before it stops on its own, the places to the right go round more
            val steps = (RecapDimens.YearRollTurns + place * RecapDimens.YearRollMore) * DigitBase + digit
            // The digits go in a column with the one that stays on top, which slides down from the oldest to it
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val strip = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                    val row = strip.height / (steps + 1)
                    layout(strip.width, row) {
                        strip.placeWithLayer(0, 0) { translationY = -(1f - progress.value) * steps * row }
                    }
                }
            ) {
                Column {
                    repeat(steps + 1) { back ->
                        Text(text = Math.floorMod(digit - back, DigitBase).toString(), style = style)
                    }
                }
            }
        }
    }
}

// The first page, with the year
@Composable
private fun ColumnScope.IntroPage(recap: Recap, isOn: Boolean) {
    val theme = LocalRecapTheme.current
    val lead = MaterialTheme.typography.titleMedium.copy(fontSize = RecapDimens.LeadSize, fontWeight = FontWeight.SemiBold)
    val soft = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold)

    // The room the font keeps above and below a line is cut, so the three lines of the sentence sit close as one
    val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

    // The words stay in the middle, and the arrow that shows the way goes to the foot of the page
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.IntroBlockGap, Alignment.CenterVertically), modifier = Modifier.fillMaxWidth().weight(1f)) {
        // The year and what comes before and after it read as one sentence, so they stay together
        Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.IntroGap)) {
            Reveal(isOn) { Text(text = stringResource(R.string.recap_intro_lead), style = lead.copy(lineHeightStyle = tight), color = theme.ink) }
            Reveal(isOn, step = 1) {
                RollingYear(
                    year = recap.year,
                    isOn = isOn,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = RecapDimens.YearSize, lineHeight = RecapDimens.YearLine, fontWeight = FontWeight.Black, lineHeightStyle = tight, color = RecapColors.Gold),
                    modifier = Modifier.trimmed(top = RecapDimens.YearTrimTop, bottom = RecapDimens.YearTrimBottom).shine(RecapColors.Shine)
                )
            }
            // A stretch of the year says which one it is under the year
            if (recap.part != RecapPart.Year) Reveal(isOn, step = 2) { Text(text = stringResource(recap.part.labelRes), style = lead.copy(lineHeightStyle = tight), color = theme.ink) }
            Reveal(isOn, step = 2) {
                Text(
                    text = stringResource(R.string.recap_intro_gone),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.NameSize, lineHeight = RecapDimens.NameLine, fontWeight = FontWeight.ExtraBold, lineHeightStyle = tight),
                    color = theme.ink
                )
            }
        }
        // What the page tells and how to move through it, the second block under the sentence
        Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap)) {
            Reveal(isOn, step = 3) { Text(text = stringResource(if (recap.isEmpty) R.string.recap_empty_story else R.string.recap_intro_caption), style = soft, color = theme.softInk) }
            if (!recap.isEmpty) Reveal(isOn, step = 4) { Text(text = stringResource(R.string.recap_intro_hint), style = soft, color = theme.softInk) }
        }
    }
    if (!recap.isEmpty) Reveal(isOn, step = 5) { SwipeHint() }
}

// An arrow that rises again and again and fades as it goes, to show that the story goes on by dragging a finger up
@Composable
private fun SwipeHint() {
    val ink = LocalRecapTheme.current.ink
    val rise = rememberInfiniteTransition(label = "SwipeHint").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RecapDimens.HintMillis, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "SwipeHint"
    )

    Canvas(
        modifier = Modifier
            .padding(bottom = RecapDimens.FooterGap + RecapDimens.HintTravel)
            .size(RecapDimens.HintWidth, RecapDimens.HintHeight)
            .graphicsLayer {
                translationY = -RecapDimens.HintTravel.toPx() * rise.value
                // It shows quickly, and fades out as it nears the top
                alpha = kotlin.math.sin(rise.value * Math.PI.toFloat())
            }
    ) {
        val stroke = RecapDimens.HintStroke.toPx()
        val head = size.width / 2f
        // The stem and the two sides of the head of the arrow
        drawLine(ink, Offset(head, size.height), Offset(head, stroke / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(head, stroke / 2f), Offset(stroke / 2f, head), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ink, Offset(head, stroke / 2f), Offset(size.width - stroke / 2f, head), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

// A cover with a record coming out from behind it on the end side, turning slowly, the way a song is played
@Composable
private fun VinylFrame(url: String?) {
    val turn = rememberInfiniteTransition(label = "Vinyl").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RecapDimens.VinylMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "Vinyl"
    )

    Box(modifier = Modifier.size(RecapDimens.Cover + RecapDimens.VinylOut, RecapDimens.Cover)) {
        // The record, black with its grooves, a label in the middle and the light on it, which turns with it
        Canvas(modifier = Modifier.align(Alignment.CenterEnd).size(RecapDimens.VinylSize).graphicsLayer { rotationZ = turn.value * FullTurnDegrees }) {
            val radius = size.minDimension / 2f
            drawCircle(RecapColors.Vinyl, radius)
            repeat(RecapDimens.VinylGrooves) { groove ->
                drawCircle(Color.White.copy(alpha = RecapColors.VinylGrooveAlpha), radius * (RecapDimens.VinylGrooveFirst - groove * RecapDimens.VinylGrooveStep), style = Stroke(width = RecapDimens.VinylGrooveStroke.toPx()))
            }
            drawCircle(RecapColors.Gold, radius * RecapDimens.VinylLabel)
            drawCircle(RecapColors.Vinyl, radius * RecapDimens.VinylHole)
            val glare = radius * RecapDimens.VinylGlare
            for (start in listOf(RecapDimens.VinylGlareStart, RecapDimens.VinylGlareStart + 180f)) {
                drawArc(
                    color = Color.White.copy(alpha = RecapColors.VinylGlareAlpha),
                    startAngle = start,
                    sweepAngle = RecapDimens.VinylGlareSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius + glare / 2f, center.y - radius + glare / 2f),
                    size = Size((radius - glare / 2f) * 2f, (radius - glare / 2f) * 2f),
                    style = Stroke(width = glare)
                )
            }
        }
        ItemThumbnail(url = url, shape = RoundedCornerShape(RecapDimens.RowCorner), modifier = Modifier.align(Alignment.CenterStart).size(RecapDimens.Cover), requestSize = RecapDimens.CoverRequest)
    }
}

// The ring of the colors of Wavvy that goes around a round photo, the same of the photo of the profile, with its colors going around
@Composable
private fun PhotoRing(thickness: Dp, modifier: Modifier = Modifier) {
    val colors = WavvyTheme.colors
    val turn = rememberInfiniteTransition(label = "PhotoRing").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RecapDimens.PhotoRingMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "PhotoRing"
    )

    // The last color is the first again, so the ring shows no seam as it turns
    Canvas(modifier = modifier) {
        val stroke = thickness.toPx()
        rotate(turn.value * FullTurnDegrees) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(colors.gradientStart, colors.gradientMiddle, colors.gradientEnd, colors.gradientStart)),
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke)
            )
        }
    }
}

// The round photo of an artist inside its ring
@Composable
private fun RingedPhoto(url: String?) {
    val edge = RecapDimens.PhotoRing + RecapDimens.PhotoRingGap

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(RecapDimens.Cover + edge * 2)) {
        PhotoRing(RecapDimens.PhotoRing, Modifier.fillMaxSize())
        ItemThumbnail(url = url, shape = CircleShape, modifier = Modifier.size(RecapDimens.Cover), requestSize = RecapDimens.CoverRequest, isProfile = true)
    }
}

// The artist listened to the most, with its photo, the time together and how much of the year it took
@Composable
private fun ColumnScope.TopArtistPage(artist: TopArtist, recap: Recap, isOn: Boolean) {
    val share = (recap.topArtistShare * Percent).toInt()

    val minutes = (artist.totalMs / MillisPerMinute).toInt()
    val time = durationWords(artist.totalMs)
    val together = if (share > 0) stringResource(R.string.recap_artist_together, time, share) else stringResource(R.string.recap_artist_together_small, time)
    // The words under the time, from the step that fits how long the two were together
    val reaction = stringArrayResource(R.array.recap_artist_reactions)[ArtistMinuteSteps.indexOfLast { minutes >= it }.coerceAtLeast(0)]

    // The note sits just over the tiles at the bottom of the page, which the bar of the system may already cover part of
    val covered = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val noteBottom = (RecapDimens.CheckerHeight + RecapDimens.ArtistNoteGap - covered).coerceAtLeast(0.dp)

    // The crown leans to the left, so it slides along the round ring by the same angle, to sit on it instead of floating beside
    val ringRadius = RecapDimens.Cover / 2 + RecapDimens.PhotoRing + RecapDimens.PhotoRingGap
    val tilt = Math.toRadians(RecapDimens.CrownTilt.toDouble())
    val crownSlide = ringRadius * Math.sin(tilt).toFloat()
    val crownDrop = ringRadius * (1f - Math.cos(tilt).toFloat())

    // The crown, the photo and the name stay in the middle and the note goes down
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
        modifier = Modifier.weight(1f)
    ) {
        // The crown sits on the top of the ring, over it, the photo is made first so the crown is drawn above
        Box(contentAlignment = Alignment.TopCenter) {
            Reveal(isOn, step = 1) { Box(modifier = Modifier.padding(top = RecapDimens.CrownRoom + RecapDimens.CrownHeight - RecapDimens.CrownLap)) { RingedPhoto(artist.picture) } }
            // The slide is outside of the reveal, which cuts at its own edges while it shows what it holds
            Box(modifier = Modifier.offset(x = -crownSlide, y = crownDrop)) {
                Reveal(isOn, step = 1) {
                    Crown(
                        RecapColors.Gold,
                        Modifier.size(RecapDimens.CrownWidth + RecapDimens.CrownRoom * 2, RecapDimens.CrownHeight + RecapDimens.CrownRoom * 2).shine(RecapColors.Shine)
                    )
                }
            }
        }
        Reveal(isOn, step = 2) { Name(artist.name, isShining = true, color = RecapColors.Gold) }
    }
    Reveal(isOn, step = 3) {
        Text(
            text = goldWords("$together $reaction", time, "$share%"),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.ArtistNoteSize, fontWeight = FontWeight.SemiBold),
            color = LocalRecapTheme.current.softInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = noteBottom)
        )
    }
}

// A time written in months, weeks, days, hours and minutes, leaving out what is zero, with the biggest units first
@Composable
private fun durationWords(totalMs: Long): String {
    val minutes = totalMs / MillisPerMinute
    val hours = minutes / MinutesPerHour
    val totalDays = hours / HoursPerDay
    val months = (totalDays / DaysPerMonth).toInt()
    val weeks = (totalDays % DaysPerMonth / DaysPerWeek).toInt()
    val days = (totalDays % DaysPerWeek).toInt()
    val hoursLeft = (hours % HoursPerDay).toInt()
    val minutesLeft = (minutes % MinutesPerHour).toInt()

    val parts = buildList {
        if (months > 0) add(pluralStringResource(R.plurals.recap_unit_months, months, months))
        if (weeks > 0) add(pluralStringResource(R.plurals.recap_unit_weeks, weeks, weeks))
        if (days > 0) add(pluralStringResource(R.plurals.recap_unit_days, days, days))
        if (hoursLeft > 0) add(stringResource(R.string.recap_unit_hours, hoursLeft))
        // Minutes only count when there is less than a day
        if (totalDays == 0L && minutesLeft > 0) add(stringResource(R.string.recap_unit_minutes, minutesLeft))
    }.take(MaxDurationUnits).ifEmpty { listOf(stringResource(R.string.recap_unit_minutes, 0)) }

    return if (parts.size == 1) parts.first() else stringResource(R.string.recap_list_last, parts.dropLast(1).joinToString(", "), parts.last())
}

// The page for who has not listened to anything yet, a warm welcome instead of numbers
@Composable
private fun NewcomerPage(
    isOn: Boolean,
    title: Int = R.string.recap_newcomer_title,
    first: Int = R.string.recap_newcomer_welcome,
    second: Int = R.string.recap_newcomer_next
) {
    val theme = LocalRecapTheme.current
    val body = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold)

    Column(modifier = Modifier.fillMaxWidth()) {
        Reveal(isOn) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.TeaseSize, lineHeight = RecapDimens.TeaseLine, fontWeight = FontWeight.ExtraBold),
                color = theme.ink
            )
        }
        Spacer(Modifier.height(RecapDimens.NewcomerTitleGap))
        Reveal(isOn, step = 2) { Text(text = stringResource(first), style = body, color = theme.softInk) }
        Spacer(Modifier.height(RecapDimens.NewcomerBodyGap))
        Reveal(isOn, step = 3) { Text(text = stringResource(second), style = body, color = theme.softInk) }
    }
}

// What a place of the podium shows, an artist or a song: who it is, its picture, how long it was listened to and the time of each month
private class PodiumEntry(val id: String?, val name: String, val picture: String?, val totalMs: Long, val months: List<Long>)

// The words that bring each place of the podium, for the artists, the songs and the albums, with the month in them or without it, the title that holds the page while it builds up and the congratulation that takes its place at the end
private class PodiumTexts(val third: Int, val thirdBare: Int, val second: Int, val secondBare: Int, val first: Int, val firstBare: Int, val title: Int, val done: Int)

// The words of the podium of the artists, the one of the albums and the one of the songs
private val ArtistsPodiumTexts = PodiumTexts(
    R.string.recap_podium_third, R.string.recap_podium_third_bare,
    R.string.recap_podium_second, R.string.recap_podium_second_bare,
    R.string.recap_podium_first, R.string.recap_podium_first_bare,
    R.string.recap_podium_title, R.string.recap_podium_done
)
private val AlbumsPodiumTexts = PodiumTexts(
    R.string.recap_albums_podium_third, R.string.recap_podium_third_bare,
    R.string.recap_albums_podium_second, R.string.recap_podium_second_bare,
    R.string.recap_podium_first, R.string.recap_podium_first_bare,
    R.string.recap_podium_drums, R.string.recap_podium_done
)
private val SongsPodiumTexts = PodiumTexts(
    R.string.recap_song_podium_third, R.string.recap_podium_third_bare,
    R.string.recap_song_podium_second, R.string.recap_podium_second_bare,
    R.string.recap_podium_first, R.string.recap_podium_first_bare,
    R.string.recap_podium_drums, R.string.recap_songs_podium_done
)

// The three artists listened to the most on a podium, and the three songs the same way, each one comes up in its own time, from the last place to the first, and the music of each one plays as it comes
// What is on the screen follows the time of the page, so holding the page or stopping the story holds the podium too
@Composable
private fun PodiumPage(
    entries: List<PodiumEntry>,
    count: Int,
    isRound: Boolean,
    texts: PodiumTexts,
    isCurrent: Boolean,
    elapsed: () -> Float,
    onSong: (entry: PodiumEntry, isPlaying: Boolean) -> Unit
) {
    val theme = LocalRecapTheme.current
    val locale = Locale.getDefault()
    // What the states below read is the newest time of the page, not the one of the composition that made them
    val time by rememberUpdatedState(elapsed)

    // The artists in the order they come up, with the month each one was listened to the most
    val slots = remember(entries, count) {
        entries.take(count).mapIndexed { index, entry ->
            val peak = entry.months.indexOf(entry.months.maxOrNull()).takeIf { entry.months.any { it > 0L } }
            PodiumSlot(
                place = index + 1,
                entry = entry,
                fullMonth = peak?.let { Month.of(it + 1).getDisplayName(TextStyle.FULL, locale) },
                start = podiumStart(count, index + 1)
            )
        }.reversed()
    }

    // The words that bring each artist, and the name that follows once its photo is on the step
    val teases = slots.map { slot ->
        val month = slot.fullMonth
        when {
            slot.place == 1 && month != null -> stringResource(texts.first, month)
            slot.place == 1 -> stringResource(texts.firstBare)
            slot.place == 2 && month != null -> stringResource(texts.second, month)
            slot.place == 2 -> stringResource(texts.secondBare)
            month != null -> stringResource(texts.third, month)
            else -> stringResource(texts.thirdBare)
        }
    }
    val names = slots.map { stringResource(R.string.recap_podium_name, it.entry.name) }

    // The artist that is coming, the one whose words are on the screen or were the last to be
    val coming by remember(slots) { derivedStateOf { slots.indexOfLast { time() >= it.start } } }
    val speaking by remember(slots) { derivedStateOf { slots.indexOfLast { time() >= it.teaseAt } } }

    // The song of an artist is made ready when its step starts to rise and only plays once its photo shows, so it never gives the artist away before
    val stamped by remember(slots) { derivedStateOf { slots.indexOfLast { time() >= it.stampAt } } }
    LaunchedEffect(coming, isCurrent) {
        if (isCurrent) slots.getOrNull(coming.coerceAtLeast(0))?.let { onSong(it.entry, false) }
    }
    LaunchedEffect(stamped, isCurrent) {
        if (isCurrent && stamped >= 0) onSong(slots[stamped].entry, true)
    }

    // The dots at the end of the title grow from one to three and start again, as if loading
    val dots by remember { derivedStateOf { 1 + (time() / RecapDimens.PodiumDotMillis).toInt() % PodiumDots } }
    val titleStyle = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.TeaseSize, lineHeight = RecapDimens.TeaseLine, fontWeight = FontWeight.ExtraBold)
    // The words and the dots are one block, centered with all the dots in it, the ones that are not on yet are only room kept, so the page never looks empty while it builds up
    val title = buildAnnotatedString {
        append(stringResource(texts.title))
        withStyle(SpanStyle(color = theme.ink)) { append(".".repeat(dots)) }
        withStyle(SpanStyle(color = Color.Transparent)) { append(".".repeat(PodiumDots - dots)) }
    }
    // When every place has shown, the title that waited for them gives way to a congratulation, so it is not left hanging
    val finishedAt = slots.maxOf { it.end }
    val isFinished by remember(slots) { derivedStateOf { time() >= finishedAt } }
    Crossfade(targetState = isFinished, animationSpec = tween(RecapDimens.PodiumFadeMillis), label = "PodiumTitle") { finished ->
        Text(
            text = if (finished) AnnotatedString(stringResource(texts.done)) else title,
            style = titleStyle,
            color = theme.ink,
            textAlign = TextAlign.Center
        )
    }
    // The first place is in the middle, the second on its start side and the third on its end side
    Row(horizontalArrangement = Arrangement.spacedBy(RecapDimens.PodiumGap), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        listOf(2, 1, 3).mapNotNull { place -> slots.firstOrNull { it.place == place } }.forEach { slot ->
            Box(modifier = Modifier.weight(1f)) { PodiumStep(slot, isRound, elapsed) }
        }
    }
    // The words fade out as the stamp lands, and the name comes like the stamp, at the same time as the photo
    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth().height(RecapDimens.PodiumPhraseHeight)) {
        val slot = slots.getOrNull(speaking)
        if (slot != null) {
            val phraseStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold)
            Text(
                text = teases[speaking],
                style = phraseStyle,
                color = theme.softInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    val now = time()
                    alpha = span(now, slot.teaseAt, RecapDimens.PodiumFadeMillis) * (1f - span(now, slot.stampAt, RecapDimens.PodiumStampMillis))
                }
            )
            Text(
                text = names[speaking],
                style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.PodiumCallSize, fontWeight = FontWeight.ExtraBold),
                color = theme.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    val now = time()
                    val fall = span(now, slot.stampAt, RecapDimens.PodiumStampMillis)
                    val drop = 1f - fall * fall
                    alpha = (if (fall > 0f) 1f else 0f) * (1f - span(now, slot.end - RecapDimens.PodiumFadeMillis, RecapDimens.PodiumFadeMillis))
                    scaleX = 1f + (RecapDimens.PodiumCallScale - 1f) * drop
                    scaleY = scaleX
                    rotationZ = RecapDimens.PodiumStampTilt * drop
                }
            )
        }
    }
}

// One place of the podium with the moments it comes up in
private class PodiumSlot(val place: Int, val entry: PodiumEntry, val fullMonth: String?, val start: Float) {
    // The words about the artist show once the step is up and the month is there, and go on until the stamp lands
    val teaseAt: Float get() = start + RecapDimens.PodiumRiseMillis
    val stampAt: Float get() = teaseAt + RecapDimens.PodiumTimeMillis + podiumTeaseMillis(place)
    val end: Float get() = start + podiumSegmentMillis(place)
}

// How long the wait before the artist shows is, the first place waits longer to build the suspense
private fun podiumTeaseMillis(place: Int): Int = if (place == 1) RecapDimens.PodiumFirstTeaseMillis else RecapDimens.PodiumTeaseMillis

// How long one artist takes on the podium, from the step rising to the words about it going away
private fun podiumSegmentMillis(place: Int): Int =
    RecapDimens.PodiumRiseMillis + RecapDimens.PodiumTimeMillis + podiumTeaseMillis(place) + RecapDimens.PodiumStampMillis + RecapDimens.PodiumNameMillis + RecapDimens.PodiumShowMillis

// When the step of a place starts, after the title and the places that come before it, from the last to the first
private fun podiumStart(count: Int, place: Int): Float =
    (RecapDimens.PodiumIntroMillis + (count downTo place + 1).sumOf { podiumSegmentMillis(it) }).toFloat()

// How long the whole page takes, it has a time of its own since it tells more than the others
private fun podiumMillis(count: Int): Int =
    RecapDimens.PodiumIntroMillis + (count downTo 1).sumOf { podiumSegmentMillis(it) } + RecapDimens.PodiumEndMillis

// How far a moment has gone, from zero before it starts to one when its time is over
private fun span(now: Float, from: Float, length: Int): Float = ((now - from) / length).coerceIn(0f, 1f)

// One step of the podium: the block rises, the time listened shows, and then the photo comes down like a stamp and the name fades in
@Composable
private fun PodiumStep(slot: PodiumSlot, isRound: Boolean, elapsed: () -> Float) {
    val theme = LocalRecapTheme.current
    // The photo of an artist is round and the cover of a song is a rounded square
    val pictureShape = if (isRound) CircleShape else RoundedCornerShape(RecapDimens.RowCorner)
    val photo = if (slot.place == 1) RecapDimens.PodiumPhotoFirst else RecapDimens.PodiumPhoto
    val height = when (slot.place) {
        1 -> RecapDimens.PodiumStepFirst
        2 -> RecapDimens.PodiumStepSecond
        else -> RecapDimens.PodiumStepThird
    }
    val shape = RoundedCornerShape(topStart = RecapDimens.PodiumCorner, topEnd = RecapDimens.PodiumCorner)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(photo)) {
            // A ring goes out from the photo where the stamp lands
            Canvas(modifier = Modifier.requiredSize(photo * 2)) {
                val landed = span(elapsed(), slot.stampAt + RecapDimens.PodiumStampMillis, RecapDimens.PodiumImpactMillis)
                if (landed > 0f && landed < 1f) {
                    drawCircle(
                        color = theme.accentAlt.copy(alpha = 1f - landed),
                        radius = photo.toPx() / 2f * (1f + RecapDimens.PodiumImpactGrow * landed),
                        style = Stroke(width = RecapDimens.PodiumImpactStroke.toPx())
                    )
                }
            }
            // The photo and its ring start big and come down fast, getting smaller and straight until they sit on their place, the ring is only for the round photos of the artists
            val edge = if (isRound) RecapDimens.PodiumRing + RecapDimens.PodiumRingGap else 0.dp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(photo).graphicsLayer {
                    val fall = span(elapsed(), slot.stampAt, RecapDimens.PodiumStampMillis)
                    val drop = 1f - fall * fall
                    alpha = if (fall > 0f) 1f else 0f
                    scaleX = 1f + (RecapDimens.PodiumStampScale - 1f) * drop
                    scaleY = scaleX
                    rotationZ = RecapDimens.PodiumStampTilt * drop
                }
            ) {
                if (isRound) PhotoRing(RecapDimens.PodiumRing, Modifier.fillMaxSize())
                ItemThumbnail(
                    url = slot.entry.picture,
                    shape = pictureShape,
                    modifier = Modifier.size(photo - edge * 2),
                    requestSize = RecapDimens.CoverRequest,
                    isProfile = isRound
                )
            }
        }
        Spacer(Modifier.height(RecapDimens.PodiumNameGap))
        Text(
            text = slot.entry.name,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = RecapDimens.PodiumNameSize, fontWeight = FontWeight.ExtraBold),
            color = theme.ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.graphicsLayer { alpha = span(elapsed(), slot.stampAt + RecapDimens.PodiumStampMillis, RecapDimens.PodiumNameMillis) }
        )
        Spacer(Modifier.height(RecapDimens.PodiumNameGap))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(height)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleY = FastOutSlowInEasing.transform(span(elapsed(), slot.start, RecapDimens.PodiumRiseMillis))
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(shape)
                    .background(theme.accentAlt)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = span(elapsed(), slot.teaseAt, RecapDimens.PodiumTimeMillis) }
            ) {
                Text(
                    text = slot.place.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.PodiumRankSize, fontWeight = FontWeight.Black),
                    color = theme.background
                )
                // How long it was listened to, in a short way that fits the step
                Text(
                    text = durationText(slot.entry.totalMs),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = RecapDimens.PodiumTimeSize, fontWeight = FontWeight.Bold),
                    color = theme.background,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// The five artists listened to the most
@Composable
private fun TopArtistsPage(artists: List<TopArtist>, isOn: Boolean) {
    Reveal(isOn) { Name(stringResource(R.string.recap_artists_title)) }
    Reveal(isOn) { Caption(stringResource(R.string.recap_artists_caption)) }
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.ListGap)) {
        artists.take(StoryTop).forEachIndexed { index, artist ->
            Reveal(isOn, step = index + 1) {
                PlaceRow(place = index + 1, title = artist.name, subtitle = durationText(artist.totalMs), cover = artist.picture, isProfile = true, isCompact = true)
            }
        }
    }
}

// The top 10 for who has no artists yet, the places and their numbers are there with nobody on them, and a faint word at the foot of the page
@Composable
private fun ColumnScope.TopArtistsEmptyPage(
    isOn: Boolean,
    title: Int = R.string.recap_artists_title,
    caption: Int = R.string.recap_artists_empty_caption,
    footer: Int = R.string.recap_artists_empty_footer
) {
    val theme = LocalRecapTheme.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
        modifier = Modifier.weight(1f)
    ) {
        Reveal(isOn) { Name(stringResource(title)) }
        Reveal(isOn, step = 1) { Caption(stringResource(caption)) }
        Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.ListGap)) {
            repeat(RecapDimens.PlaceholderRows) { index ->
                Reveal(isOn, step = index + 2) { PlaceholderRow(place = index + 1, fade = 1f - index * RecapDimens.PlaceholderFade) }
            }
        }
    }
    Reveal(isOn, step = RecapDimens.PlaceholderRows + 2) {
        Text(
            text = stringResource(footer),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = theme.ink.copy(alpha = RecapColors.FooterAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = RecapDimens.FooterGap)
        )
    }
}

// A place of the list with nobody on it, its number, a square with a question mark and two lines where the name would be
@Composable
private fun PlaceholderRow(place: Int, fade: Float) {
    val theme = LocalRecapTheme.current

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = fade }) {
        Text(
            text = place.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = RecapDimens.PlaceSizeSmall, fontWeight = FontWeight.Black),
            color = RecapColors.PlaceColors.getOrNull(place - 1) ?: theme.ink,
            modifier = Modifier.width(RecapDimens.RowPlaceSmall)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(RecapDimens.RowCoverSmall).clip(RoundedCornerShape(RecapDimens.RowCorner)).background(theme.ink.copy(alpha = RecapColors.PlaceholderAlpha))
        ) {
            Text(text = "?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = theme.ink.copy(alpha = RecapColors.SoftAlpha))
        }
        Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.PlaceholderLineGap), modifier = Modifier.weight(1f).padding(start = RecapDimens.RowGap)) {
            Box(modifier = Modifier.fillMaxWidth(RecapDimens.PlaceholderLong).height(RecapDimens.PlaceholderLine).clip(CircleShape).background(theme.ink.copy(alpha = RecapColors.PlaceholderAlpha)))
            Box(modifier = Modifier.fillMaxWidth(RecapDimens.PlaceholderShort).height(RecapDimens.PlaceholderLine).clip(CircleShape).background(theme.ink.copy(alpha = RecapColors.PlaceholderAlpha / 2f)))
        }
    }
}

// The song listened to the most, with its cover
@Composable
private fun TopSongPage(recap: Recap, isOn: Boolean) {
    val top = recap.songs.first()

    // The plays count up, and the words under the song say what that many plays are equal to
    val shown = countUp(top.plays, isOn)
    val equivalent = stringArrayResource(R.array.recap_song_equivalents)[SongPlaySteps.indexOfLast { top.plays >= it }.coerceAtLeast(0)]

    Reveal(isOn) { Name(pluralStringResource(R.plurals.recap_song_plays_title, top.plays, shown)) }
    Reveal(isOn, step = 1) { VinylFrame(top.song.artworkUrl) }
    Reveal(isOn, step = 2) { SongTitle(top.song.title, color = RecapColors.Gold) }
    top.song.artist?.let { artist ->
        Reveal(isOn, step = 3, modifier = ByLineRaise) { ByLine(artist) }
    }
    // The words go down by an offset, which moves only them, the page is centered and room added would lift everything else
    Reveal(isOn, step = 4, modifier = Modifier.offset(y = RecapDimens.SongNoteDrop)) { Box(modifier = Modifier.padding(top = RecapDimens.SongNoteGap)) { Caption(equivalent) } }
}

// The title of a song in one line, which scrolls along when it does not fit
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongTitle(text: String, isShining: Boolean = false, color: Color = LocalRecapTheme.current.ink) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(fontSize = RecapDimens.SongTitleSize, lineHeight = RecapDimens.SongTitleLine, fontWeight = FontWeight.ExtraBold),
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = RecapDimens.MarqueeDelayMillis)
            .then(if (isShining) Modifier.shine(RecapColors.Shine) else Modifier)
    )
}

// The page for what is not known yet, the albums or the genres of the songs, which a search fills little by little: it says it is being dug up, and the words at the foot are faint
@Composable
private fun ColumnScope.DiggingPage(isOn: Boolean, title: Int, body: Int, footer: Int) {
    val theme = LocalRecapTheme.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
        modifier = Modifier.weight(1f)
    ) {
        Reveal(isOn) { Name(stringResource(title)) }
        Reveal(isOn, step = 1) { Caption(stringResource(body)) }
    }
    Reveal(isOn, step = 2) {
        Text(
            text = stringResource(footer),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = theme.ink.copy(alpha = RecapColors.FooterAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = RecapDimens.FooterGap)
        )
    }
}

// The ten albums listened to the most
@Composable
private fun TopAlbumsPage(recap: Recap, isOn: Boolean) {
    Reveal(isOn) { Name(stringResource(R.string.recap_albums_title)) }
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.ListGap)) {
        recap.albums.take(StoryTop).forEachIndexed { index, album ->
            Reveal(isOn, step = index + 1) {
                PlaceRow(place = index + 1, title = album.name, subtitle = album.artist.orEmpty(), cover = album.cover, isCompact = true)
            }
        }
    }
}

// The first albums as lines through the year, the same chart of the artists and of the songs
@Composable
private fun AlbumsTimelinePage(recap: Recap, isOn: Boolean) {
    val albums = recap.albums.take(TimelineArtists).filter { it.id in recap.albumMonths }
    TimelineChart(R.string.recap_albums_timeline_title, R.string.recap_albums_timeline_caption, albums.map { it.name }, albums.map { recap.albumMonths[it.id].orEmpty() }, isOn, monthsSoFar(recap))
}

// The five songs listened to the most
@Composable
private fun TopSongsPage(recap: Recap, isOn: Boolean) {
    Reveal(isOn) { Name(stringResource(R.string.recap_songs_title)) }
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.ListGap)) {
        recap.songs.take(StoryTop).forEachIndexed { index, top ->
            Reveal(isOn, step = index + 1) {
                PlaceRow(place = index + 1, title = top.song.title, subtitle = top.song.artist.orEmpty(), cover = top.song.artworkUrl, isCompact = true)
            }
        }
    }
}

// The genres as a stack, each band as tall as the time listened to it
@Composable
private fun GenresPage(recap: Recap, isOn: Boolean) {
    val shown = recap.genres.take(StackGenres)
    val sum = shown.sumOf { it.totalMs }.coerceAtLeast(1L)

    // The title tells how many genres there were, with the number in gold, and under it the words that lead to the stack
    val count = recap.genres.size
    Reveal(isOn) { Name(goldWords(pluralStringResource(R.plurals.recap_genres_count, count, count), count.toString())) }
    Reveal(isOn, step = 1) { Caption(stringResource(R.string.recap_genres_title)) }
    Reveal(isOn, step = 2) {
        Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.StackGap), modifier = Modifier.width(RecapDimens.Stack).height(RecapDimens.StackHeight)) {
            shown.forEachIndexed { index, genre ->
                val top = if (index == 0) RecapDimens.StackEnd else RecapDimens.StackGap
                val bottom = if (index == shown.lastIndex) RecapDimens.StackEnd else RecapDimens.StackGap
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight((genre.totalMs.toFloat() / sum).coerceAtLeast(MinimumBand))
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                        .background(RecapColors.Bands[index % RecapColors.Bands.size])
                ) {
                    Text(
                        text = genre.name.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.StackLabel, fontWeight = FontWeight.Bold),
                        color = RecapColors.BandInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = RecapDimens.StackGap * 3)
                    )
                }
            }
        }
    }
}

// The album listened to the most
@Composable
private fun ColumnScope.AlbumPage(recap: Recap, isOn: Boolean) {
    val album = recap.albums.first()

    // The cover in its case, the name that shines and the artist under it stay in the middle, and the time goes to the foot of the page
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
        modifier = Modifier.weight(1f)
    ) {
        Reveal(isOn) { CaseCover(album.cover) }
        Reveal(isOn, step = 1) { SongTitle(album.name, isShining = true, color = RecapColors.Gold) }
        album.artist?.let { artist -> Reveal(isOn, step = 2, modifier = ByLineRaise) { ByLine(artist) } }
    }
    Reveal(isOn, step = 3) {
        Text(
            text = stringResource(R.string.recap_album_time, durationText(album.totalMs)),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.ArtistNoteSize, fontWeight = FontWeight.SemiBold),
            color = LocalRecapTheme.current.softInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = RecapDimens.FooterGap)
        )
    }
}

// The cover of an album in a case of a disc: the spine with its hinges on the start side, the edge of the plastic around it and the light on the plastic
@Composable
private fun CaseCover(url: String?) {
    val cover = RecapDimens.Cover

    Box(modifier = Modifier.size(cover + RecapDimens.CaseSpine, cover).shine(RecapColors.Shine)) {
        ItemThumbnail(url = url, shape = RoundedCornerShape(RecapDimens.RowCorner), modifier = Modifier.align(Alignment.CenterEnd).size(cover), requestSize = RecapDimens.CoverRequest)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spine = RecapDimens.CaseSpine.toPx()
            val corner = androidx.compose.ui.geometry.CornerRadius(RecapDimens.CaseCorner.toPx())
            val frame = RecapDimens.CaseFrame.toPx()

            // The spine and the two hinges on it
            drawRoundRect(RecapColors.Vinyl, Offset.Zero, Size(spine, size.height), corner)
            listOf(RecapDimens.CaseHingeTop, 1f - RecapDimens.CaseHingeTop - RecapDimens.CaseHingeHeight).forEach { top ->
                drawRoundRect(
                    color = RecapColors.Gold.copy(alpha = RecapColors.CaseHingeAlpha),
                    topLeft = Offset(spine * RecapDimens.CaseHingeInset, size.height * top),
                    size = Size(spine * (1f - RecapDimens.CaseHingeInset * 2f), size.height * RecapDimens.CaseHingeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(spine / 4f)
                )
            }

            // The edge of the plastic around the whole case
            drawRoundRect(RecapColors.Gold.copy(alpha = RecapColors.CaseFrameAlpha), Offset(frame / 2f, frame / 2f), Size(size.width - frame, size.height - frame), corner, style = Stroke(width = frame))

            // Two bands of light across the cover, leaning, the way the plastic shows a window
            val left = spine
            val width = size.width - spine
            listOf(RecapDimens.CaseGlareFirst to RecapDimens.CaseGlareWide, RecapDimens.CaseGlareSecond to RecapDimens.CaseGlareThin).forEach { (from, wide) ->
                val glare = Path().apply {
                    moveTo(left + width * (from + wide), 0f)
                    lineTo(left + width * (from + wide * 2f), 0f)
                    lineTo(left + width * (from - RecapDimens.CaseGlareLean + wide), size.height)
                    lineTo(left + width * (from - RecapDimens.CaseGlareLean), size.height)
                    close()
                }
                drawPath(glare, Color.White.copy(alpha = RecapColors.CaseGlareAlpha))
            }
        }
    }
}

// The line with the artist under the name of a song or of an album
@Composable
private fun ByLine(artist: String) {
    Text(
        text = stringResource(R.string.recap_song_by, artist),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.SongArtistSize, fontWeight = FontWeight.SemiBold),
        color = LocalRecapTheme.current.ink.copy(alpha = RecapColors.SongArtistAlpha),
        textAlign = TextAlign.Center
    )
}

// The line with the artist goes up a little, moved with the box that brings it in, since the fade draws only what is inside that box
private val ByLineRaise = Modifier.offset(y = -RecapDimens.SongArtistRaise)

// The minutes of the year counting up, in gold, and under them what they count and what that much time is, in words that change with how long it was
@Composable
private fun MinutesPage(recap: Recap, isOn: Boolean) {
    val total = recap.totalMs / MillisPerMinute
    val minutes = countUp(total.toInt(), isOn)
    val time = durationWords(recap.totalMs)
    val unit = stringResource(R.string.recap_minutes_unit)
    val note = if (recap.isEmpty) stringResource(R.string.recap_minutes_empty_caption) else String.format(stringArrayResource(R.array.recap_minutes_notes)[MinutesSteps.indexOfLast { total >= it }.coerceAtLeast(0)], time)

    Reveal(isOn) { Name(stringResource(if (recap.isEmpty) R.string.recap_minutes_empty_lead else R.string.recap_minutes_lead)) }
    Reveal(isOn, step = 1) { Big(NumberFormat.getIntegerInstance().format(minutes), color = RecapColors.Gold) }
    Reveal(isOn, step = 2) { Caption(goldWords("$unit $note", time)) }
}

// The day the most was listened to, and how long
@Composable
private fun BiggestDayPage(recap: Recap, isOn: Boolean) {
    val context = LocalContext.current
    val day = recap.biggestDay ?: return
    val minutes = countUp((recap.biggestDayMs / MillisPerMinute).toInt(), isOn)
    // The day written as the phone writes it, with no year
    val date = DateUtils.formatDateTime(context, day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR)

    // The date and the minutes in gold, and under them that it was the record of the year
    Reveal(isOn) { Name(goldWords(stringResource(R.string.recap_day_title, date), date)) }
    Reveal(isOn, step = 1) { Big(NumberFormat.getIntegerInstance().format(minutes), color = RecapColors.Gold) }
    Reveal(isOn, step = 2) { Caption(pluralStringResource(R.plurals.recap_day_minutes, minutes.coerceAtLeast(1))) }
}

// The months as bars that rise, the biggest one named
@Composable
private fun MonthsPage(recap: Recap, isOn: Boolean) {
    val theme = LocalRecapTheme.current
    val locale = Locale.getDefault()
    val peak = recap.months.max().coerceAtLeast(1L)
    val peakIndex = recap.months.indexOf(recap.months.max())
    val rise = remember { Animatable(0f) }
    LaunchedEffect(isOn) { if (isOn) rise.animateTo(1f, tween(RecapDimens.CountMillis, delayMillis = RecapDimens.RevealStep, easing = FastOutSlowInEasing)) }

    Reveal(isOn) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap)) {
            Name(stringResource(R.string.recap_months_story))
            Caption(stringResource(R.string.recap_months_story_caption))
        }
    }
    Row(modifier = Modifier.fillMaxWidth().height(RecapDimens.StoryChart), verticalAlignment = Alignment.Bottom) {
        recap.months.forEachIndexed { index, total ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight((total.toFloat() / peak * rise.value).coerceAtLeast(RecapDimens.BarMinFraction))
                            .fillMaxWidth(RecapDimens.BarWidthFraction)
                            .clip(RoundedCornerShape(RecapDimens.BarCorner))
                            .background(if (index == peakIndex) theme.accent else theme.ink)
                    )
                }
                Text(
                    text = monthLabel(index + 1, locale),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = RecapDimens.MonthLabelSize, fontWeight = FontWeight.Bold),
                    color = theme.ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = RecapDimens.BarLabelGap)
                )
            }
        }
    }
    Reveal(isOn, step = 4) {
        val time = durationText(peak)
        val month = Month.of(peakIndex + 1).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
        Caption(goldWords(stringResource(R.string.recap_peak, month, time), month, time))
    }
}

// The parts of the day as bars, the one with the most named
@Composable
private fun ClockPage(recap: Recap, isOn: Boolean) {
    val theme = LocalRecapTheme.current
    val parts = DayPart.entries
    val sums = parts.indices.map { index -> recap.hours.subList(index * HoursPerDay / parts.size, (index + 1) * HoursPerDay / parts.size).sum() }
    val peak = sums.max().coerceAtLeast(1L)
    val total = sums.sum().coerceAtLeast(1L)
    val lead = recap.dayPart

    Reveal(isOn) { Name(stringResource(partTitle(lead))) }
    Reveal(isOn, step = 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(RecapDimens.ClockGap), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().height(RecapDimens.ClockHeight)) {
            parts.forEachIndexed { index, part ->
                val fraction = (sums[index].toFloat() / peak).coerceAtLeast(RecapDimens.BarMinFraction)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(text = "${sums[index] * Percent / total}%", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (part == lead) RecapColors.Gold else theme.ink)
                    Box(
                        modifier = Modifier
                            .padding(top = RecapDimens.BarLabelGap)
                            .fillMaxWidth()
                            .height((RecapDimens.ClockHeight - RecapDimens.ClockLabel) * fraction)
                            .clip(RoundedCornerShape(RecapDimens.ClockBarCorner))
                            .background(if (part == lead) theme.accent else theme.ink)
                    )
                    Text(text = stringResource(partName(part)), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = theme.ink, modifier = Modifier.padding(top = RecapDimens.BarLabelGap))
                }
            }
        }
    }
    val share = (sums[parts.indexOf(lead)] * Percent / total).toInt()
    Reveal(isOn, step = 3) {
        Text(
            text = goldWords(stringResource(R.string.recap_clock_caption, share), "$share%"),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = RecapDimens.CaptionSize, fontWeight = FontWeight.SemiBold),
            color = theme.softInk,
            textAlign = TextAlign.Center
        )
    }
}

// The longest run of days one right after the other with music, in gold, and under it the words that follow how long the run was
@Composable
private fun DaysPage(recap: Recap, isOn: Boolean) {
    val streak = recap.longestStreak
    val shown = countUp(streak, isOn)
    val note = stringArrayResource(R.array.recap_days_notes)[DaysSteps.indexOfLast { streak >= it }.coerceAtLeast(0)]

    Reveal(isOn) { Name(stringResource(R.string.recap_days_title)) }
    Reveal(isOn, step = 1) { Big(shown.toString(), color = RecapColors.Gold) }
    Reveal(isOn, step = 2) { Caption("${pluralStringResource(R.plurals.recap_days_caption, streak)} $note") }
}

// The five favorite artists as lines through the year, so the months each one rose and fell can be seen side by side
@Composable
private fun TimelinePage(recap: Recap, isOn: Boolean) {
    val names = recap.artists.take(TimelineArtists).map { it.name }.filter { it in recap.artistMonths }
    TimelineChart(R.string.recap_timeline_title, R.string.recap_timeline_caption, names, names.map { recap.artistMonths[it].orEmpty() }, isOn, monthsSoFar(recap))
}

// The first songs as lines through the year, the same chart of the artists
@Composable
private fun SongsTimelinePage(recap: Recap, isOn: Boolean) {
    val songs = recap.songs.take(TimelineArtists).filter { it.song.id in recap.songMonths }
    TimelineChart(R.string.recap_songs_timeline_title, R.string.recap_songs_timeline_caption, songs.map { it.song.title }, songs.map { recap.songMonths[it.song.id].orEmpty() }, isOn, monthsSoFar(recap))
}

// Lines through the year, one for each name, with the months under them and the names under that
@Composable
private fun TimelineChart(title: Int, caption: Int, names: List<String>, series: List<List<Long>>, isOn: Boolean, months: Int) {
    val theme = LocalRecapTheme.current
    val draw = remember { Animatable(0f) }
    LaunchedEffect(isOn) { if (isOn) draw.animateTo(1f, tween(RecapDimens.CountMillis, delayMillis = RecapDimens.RevealStep, easing = FastOutSlowInEasing)) }

    Reveal(isOn) { Name(stringResource(title)) }
    Reveal(isOn, step = 1) { Caption(stringResource(caption)) }
    // Each line adds up the time of the months, so it ends on the whole time listened, and the top of the chart stands for the most listened of all of them
    val cumulative = series.map { values -> values.runningFold(0L) { sum, value -> sum + value }.drop(1) }
    val peak = cumulative.maxOfOrNull { it.lastOrNull() ?: 0L }?.coerceAtLeast(1L) ?: 1L
    val axisStyle = MaterialTheme.typography.labelSmall.copy(fontSize = RecapDimens.MonthLabelSize, fontWeight = FontWeight.Bold)
    Reveal(isOn, step = 2) {
        Row {
            // The scale on the start side, with the middle of each word on its line: the most listened to at the top, half of it in the middle and nothing at the bottom
            Box(modifier = Modifier.width(RecapDimens.AxisWidth).height(RecapDimens.StoryChart)) {
                Text(text = durationText(peak), style = axisStyle, color = theme.softInk, modifier = Modifier.onScale(0f, RecapDimens.TimelineStroke * 2))
                Text(text = durationText(peak / 2), style = axisStyle, color = theme.softInk, modifier = Modifier.onScale(0.5f, RecapDimens.TimelineStroke * 2))
                Text(text = durationText(0L), style = axisStyle, color = theme.softInk, modifier = Modifier.onScale(1f, RecapDimens.TimelineStroke * 2))
            }
            Column(modifier = Modifier.weight(1f)) {
                // The lines, each one in its own color, drawn from the first month to the last as the page shows
                Canvas(modifier = Modifier.fillMaxWidth().height(RecapDimens.StoryChart)) {
                    // The line keeps its own thickness of room above and below, so a month with nothing is never cut by the edge of the chart
                    val edge = RecapDimens.TimelineStroke.toPx()
                    val inset = RecapDimens.TimelineInset.toPx()
                    val step = (size.width - inset * 2f) / (MonthsInYear - 1)
                    val height = size.height - edge * 2f
                    // Faint lines at the three heights of the scale, so what a line stands for can be read
                    listOf(0f, 0.5f, 1f).forEach { share ->
                        val y = edge + height * share
                        drawLine(theme.ink.copy(alpha = RecapColors.GridAlpha), Offset(0f, y), Offset(size.width, y), strokeWidth = RecapDimens.AxisGridStroke.toPx())
                    }
                    // A line stops at the month that is going, the months still to come have nothing to tell yet
                    cumulative.forEachIndexed { index, values ->
                        val line = Path()
                        values.take(months).forEachIndexed { month, value ->
                            val x = inset + month * step
                            val y = edge + height - value.toFloat() / peak * height
                            if (month == 0) {
                                line.moveTo(x, y)
                            } else {
                                val previousY = edge + height - values[month - 1].toFloat() / peak * height
                                line.cubicTo(x - step / 2f, previousY, x - step / 2f, y, x, y)
                            }
                        }
                        val measure = PathMeasure().apply { setPath(line, false) }
                        val shown = Path()
                        measure.getSegment(0f, measure.length * draw.value, shown, true)
                        drawPath(shown, RecapColors.Lines[index % RecapColors.Lines.size], style = Stroke(width = RecapDimens.TimelineStroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                MonthLabels(RecapDimens.TimelineInset, Modifier.fillMaxWidth().padding(top = RecapDimens.BarLabelGap))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.LegendGap)) {
        names.forEachIndexed { index, name ->
            Reveal(isOn, step = index + 3) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(RecapDimens.LegendDot).clip(CircleShape).background(RecapColors.Lines[index % RecapColors.Lines.size]))
                    Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = theme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = RecapDimens.LegendGap))
                    // How long it was listened to in the whole year, where its line ends
                    Text(text = durationText(cumulative[index].lastOrNull() ?: 0L), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = theme.softInk, modifier = Modifier.padding(start = RecapDimens.LegendGap))
                }
            }
        }
    }
}

// The timeline for who has no artists yet, a flat line through the year and a warm word instead of the curves
@Composable
private fun ColumnScope.TimelineEmptyPage(
    isOn: Boolean,
    title: Int = R.string.recap_timeline_title,
    caption: Int = R.string.recap_timeline_empty_caption,
    footer: Int = R.string.recap_timeline_empty_footer
) {
    val theme = LocalRecapTheme.current
    val draw = remember { Animatable(0f) }
    LaunchedEffect(isOn) { if (isOn) draw.animateTo(1f, tween(RecapDimens.CountMillis, delayMillis = RecapDimens.RevealStep, easing = FastOutSlowInEasing)) }

    // The title, the line and what it says stay in the middle, and a faint word goes to the foot of the page
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
        modifier = Modifier.weight(1f)
    ) {
        Reveal(isOn) { Name(stringResource(title)) }
        Reveal(isOn, step = 1) { Caption(stringResource(caption)) }
        Reveal(isOn, step = 2) {
            Column {
                // A straight line along the middle, drawn from the first month to the last as the page shows
                Canvas(modifier = Modifier.fillMaxWidth().height(RecapDimens.StoryChart)) {
                    val edge = RecapDimens.TimelineStroke.toPx()
                    val inset = RecapDimens.TimelineInset.toPx()
                    // The line lies on the bottom of the chart, where a month with nothing sits in the lines with data
                    val y = size.height - edge
                    drawLine(theme.ink, Offset(inset, y), Offset(inset + (size.width - inset * 2f) * draw.value, y), strokeWidth = edge, cap = StrokeCap.Round)
                }
                MonthLabels(RecapDimens.TimelineInset, Modifier.fillMaxWidth().padding(top = RecapDimens.BarLabelGap))
            }
        }
    }
    Reveal(isOn, step = 3) {
        Text(
            text = stringResource(footer),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = theme.ink.copy(alpha = RecapColors.FooterAlpha),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = RecapDimens.FooterGap)
        )
    }
}

// The months under a chart, each one centered under its point, with the first and the last one inset from the edges as the points are
@Composable
private fun MonthLabels(inset: Dp, modifier: Modifier = Modifier) {
    val theme = LocalRecapTheme.current
    val locale = Locale.getDefault()
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = RecapDimens.MonthLabelSize, fontWeight = FontWeight.Bold)

    Layout(
        content = { repeat(MonthsInYear) { month -> Text(text = monthLabel(month + 1, locale), style = style, color = theme.ink, maxLines = 1, softWrap = false) } },
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val edge = inset.roundToPx()
        val step = (constraints.maxWidth - edge * 2).toFloat() / (MonthsInYear - 1)
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { month, placeable -> placeable.place((edge + step * month - placeable.width / 2f).roundToInt(), 0) }
        }
    }
}

// How many months of the year of the recap have come, all of them for a year that is over
private fun monthsSoFar(recap: Recap): Int {
    val today = LocalDate.now()
    return if (recap.year == today.year) today.monthValue else MonthsInYear
}

// The short name of a month in the language of the phone, with its first letter in capital and no dot
private fun monthLabel(month: Int, locale: Locale): String =
    Month.of(month).getDisplayName(TextStyle.SHORT, locale).removeSuffix(".").replaceFirstChar { it.titlecase(locale) }

// How much more or less was listened to than in the year before
@Composable
private fun ComparisonPage(recap: Recap, isOn: Boolean) {
    val change = recap.changeFromPrevious ?: return
    val previous = recap.previousTotalMs ?: return
    val shown = countUp(kotlin.math.abs(change), isOn)
    val sign = if (change > 0) "+" else if (change < 0) "-" else ""
    val difference = durationText(kotlin.math.abs(recap.totalMs - previous))

    // The sentence goes on from the title through the percent in gold, and how much time that is follows it, in gold too
    val said = stringResource(if (change > 0) R.string.recap_change_more else if (change < 0) R.string.recap_change_less else R.string.recap_change_same)
    val much = if (change == 0) "" else " " + stringResource(if (change > 0) R.string.recap_change_more_caption else R.string.recap_change_less_caption, difference)

    Reveal(isOn) { Name(stringResource(R.string.recap_change_lead, recap.year - 1)) }
    Reveal(isOn, step = 1) { Big("$sign$shown%", color = RecapColors.Gold) }
    Reveal(isOn, step = 2) { Caption(goldWords(said + much, difference)) }
}

// The releases of the year by artists the user follows that were never played
@Composable
private fun MissedPage(recap: Recap, isOn: Boolean) {
    Reveal(isOn) { Name(stringResource(R.string.recap_missed_title)) }
    Reveal(isOn, step = 1) { Caption(stringResource(R.string.recap_missed_caption, recap.year)) }
    Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.ListGap)) {
        recap.missed.take(MissedListed).forEachIndexed { index, release ->
            Reveal(isOn, step = index + 2) {
                PlaceRow(place = null, title = release.title, subtitle = release.artist, cover = release.cover)
            }
        }
    }
}

// The age of the music, by the year that weighs the most in what was listened to
@Composable
private fun MusicAgePage(recap: Recap, isOn: Boolean) {
    val age = recap.musicAge ?: return
    val counted = countUp(age, isOn)
    // The year that weighs the most follows the age in the same words, in gold
    val musicYear = recap.musicYear
    val year = musicYear?.toString()
    val unit = pluralStringResource(R.plurals.recap_age_unit, age)
    val caption = if (musicYear != null) "$unit " + stringResource(R.string.recap_age_caption, musicYear) else unit

    Reveal(isOn) { Name(stringResource(R.string.recap_age_title)) }
    Reveal(isOn, step = 1) { Big(counted.toString(), color = RecapColors.Gold) }
    Reveal(isOn, step = 2) { Caption(if (year != null) goldWords(caption, year) else AnnotatedString(caption)) }
}

// The kind of listener the year says, on a card leaning over two others
@Composable
private fun ListenerPage(recap: Recap, isOn: Boolean, forced: Listener? = null) {
    val theme = LocalRecapTheme.current
    val kind = forced ?: listenerOf(recap)

    Reveal(isOn) { Lead(stringResource(R.string.recap_listener_title)) }
    Reveal(isOn, step = 1) {
        // The box holds the leaning cards whole, since the fade draws only what is inside it
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(RecapDimens.CardWidth + RecapDimens.CardRoom, RecapDimens.CardHeight + RecapDimens.CardRoom)) {
            Box(modifier = Modifier.size(RecapDimens.CardWidth, RecapDimens.CardHeight).graphicsLayer { rotationZ = -RecapDimens.CardLean }.clip(RoundedCornerShape(RecapDimens.CardCorner)).background(theme.accent))
            Box(modifier = Modifier.size(RecapDimens.CardWidth, RecapDimens.CardHeight).graphicsLayer { rotationZ = RecapDimens.CardLean }.clip(RoundedCornerShape(RecapDimens.CardCorner)).background(theme.accentAlt))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RecapDimens.PageGap, Alignment.CenterVertically),
                modifier = Modifier
                    .size(RecapDimens.CardWidth, RecapDimens.CardHeight)
                    .clip(RoundedCornerShape(RecapDimens.CardCorner))
                    .background(RecapColors.CardFace)
                    .padding(RecapDimens.CardPadding)
            ) {
                ListenerArt(kind, Modifier.size(RecapDimens.CardShape))
                Text(text = stringResource(listenerTitle(kind)), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = RecapColors.CardInk, textAlign = TextAlign.Center)
                Text(text = stringResource(listenerText(kind)), style = MaterialTheme.typography.bodyMedium, color = RecapColors.CardInk, textAlign = TextAlign.Center)
            }
        }
    }
}

// The last page, which leads to all the numbers
@Composable
private fun OutroPage(recap: Recap, isOn: Boolean, onDetails: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCreating by remember { mutableStateOf(false) }

    Reveal(isOn) { Name(goldWords(stringResource(R.string.recap_outro_title, recap.year), recap.year.toString())) }
    Reveal(isOn, step = 1) { Caption(stringResource(R.string.recap_outro_caption)) }
    // The buttons are moved down only on the screen, outside the box that rises them in, so they keep away from the words while the title and the words stay where they are
    Reveal(isOn, step = 2, modifier = Modifier.offset(y = RecapDimens.OutroButtonsDrop)) { PillButton(text = stringResource(R.string.recap_details), isFilled = true, onClick = onDetails) }
    // A year with no songs has nothing to make a playlist of
    if (recap.songs.isNotEmpty()) Reveal(isOn, step = 3, modifier = Modifier.offset(y = RecapDimens.OutroButtonsDrop)) {
        PillButton(
            text = stringResource(R.string.recap_playlist),
            isFilled = false,
            onClick = {
                if (!isCreating) {
                    isCreating = true
                    scope.launch {
                        // The songs of the year, the most listened first, go to a new private playlist of the account
                        val result = PlaylistRepository(context).create(context.getString(R.string.recap_playlist_title, recap.year), PlaylistPrivacy.Private, recap.songs.take(PlaylistSongs).map { it.song.id })
                        Toasts.show(context.getString(if (result.isSuccess) R.string.recap_playlist_done else R.string.recap_playlist_error), isError = result.isFailure)
                        isCreating = false
                    }
                }
            }
        )
    }
}

// A button as long as its words, filled with the color of the words or only with a line of it around
@Composable
private fun PillButton(text: String, isFilled: Boolean, onClick: () -> Unit) {
    val theme = LocalRecapTheme.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(RecapDimens.ButtonHeight)
            .clip(RoundedCornerShape(RecapDimens.ButtonCorner))
            .then(if (isFilled) Modifier.background(theme.ink) else Modifier.border(RecapDimens.ButtonLine, theme.ink, RoundedCornerShape(RecapDimens.ButtonCorner)))
            .clickable(onClick = onClick)
            .padding(horizontal = RecapDimens.PageSide)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (isFilled) theme.background else theme.ink)
    }
}

// A place of a list, its number, its cover, its name and the line under it
@Composable
private fun PlaceRow(
    place: Int?,
    title: String,
    subtitle: String,
    cover: String?,
    isProfile: Boolean = false,
    isCompact: Boolean = false,
    shape: Shape = RoundedCornerShape(RecapDimens.RowCorner)
) {
    val theme = LocalRecapTheme.current
    val coverSize = if (isCompact) RecapDimens.RowCoverSmall else RecapDimens.RowCover

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        place?.let {
            Text(
                text = it.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (isCompact) RecapDimens.PlaceSizeSmall else RecapDimens.PlaceSize, fontWeight = FontWeight.Black),
                color = RecapColors.PlaceColors.getOrNull(it - 1) ?: theme.ink,
                modifier = Modifier.width(if (isCompact) RecapDimens.RowPlaceSmall else RecapDimens.RowPlace)
            )
        }
        ItemThumbnail(url = cover, shape = shape, modifier = Modifier.size(coverSize), requestSize = RecapDimens.CoverRequest, isProfile = isProfile)
        Column(modifier = Modifier.weight(1f).padding(start = RecapDimens.RowGap)) {
            Text(text = title, style = (if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge).copy(fontWeight = FontWeight.Bold), color = theme.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = theme.softInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// The shapes of each kind of listener, the ones each one had while the pages were checked one after the other, with the few that were tuned by hand
private fun listenerVariant(kind: Listener?): Int = when (kind) {
    Listener.Replayer, Listener.Eclectic -> RecapDimens.DecorListener
    Listener.Trendy -> RecapDimens.DecorTuned
    Listener.EarlyBird -> RecapDimens.DecorEarly
    null -> ListenerFirstVariant
    else -> (ListenerFirstVariant + kind.ordinal) % RecapDimens.DecorVariants
}

// What the numbers say the listener is
private fun listenerOf(recap: Recap): Listener = when {
    recap.isEmpty -> Listener.Visitor
    recap.playsPerSong >= ReplayerPlays -> Listener.Replayer
    recap.topArtistShare >= SpecialistShare -> Listener.Specialist
    recap.playsPerSong <= AdventurerPlays && recap.artists.size >= AdventurerArtists -> Listener.Adventurer
    recap.longestStreak >= StreakerDays -> Listener.Streaker
    recap.musicYear?.let { recap.year - it >= NostalgicYears } == true -> Listener.Nostalgic
    recap.musicYear?.let { recap.year - it <= TrendyYears } == true -> Listener.Trendy
    recap.albums.size >= CollectorAlbums -> Listener.Collector
    recap.dayPart == DayPart.Night -> Listener.Nocturnal
    recap.dayPart == DayPart.Morning -> Listener.EarlyBird
    else -> Listener.Eclectic
}

private fun listenerTitle(kind: Listener): Int = when (kind) {
    Listener.Replayer -> R.string.recap_type_replayer
    Listener.Specialist -> R.string.recap_type_specialist
    Listener.Adventurer -> R.string.recap_type_adventurer
    Listener.Streaker -> R.string.recap_type_streaker
    Listener.Nostalgic -> R.string.recap_type_nostalgic
    Listener.Trendy -> R.string.recap_type_trendy
    Listener.Collector -> R.string.recap_type_collector
    Listener.Nocturnal -> R.string.recap_type_nocturnal
    Listener.EarlyBird -> R.string.recap_type_early_bird
    Listener.Visitor -> R.string.recap_type_visitor
    Listener.Eclectic -> R.string.recap_type_eclectic
}

private fun listenerText(kind: Listener): Int = when (kind) {
    Listener.Replayer -> R.string.recap_type_replayer_text
    Listener.Specialist -> R.string.recap_type_specialist_text
    Listener.Adventurer -> R.string.recap_type_adventurer_text
    Listener.Streaker -> R.string.recap_type_streaker_text
    Listener.Nostalgic -> R.string.recap_type_nostalgic_text
    Listener.Trendy -> R.string.recap_type_trendy_text
    Listener.Collector -> R.string.recap_type_collector_text
    Listener.Nocturnal -> R.string.recap_type_nocturnal_text
    Listener.EarlyBird -> R.string.recap_type_early_bird_text
    Listener.Visitor -> R.string.recap_type_visitor_text
    Listener.Eclectic -> R.string.recap_type_eclectic_text
}

// The words for the part of the day that leads, and for each part
private fun partTitle(part: DayPart): Int = when (part) {
    DayPart.Night -> R.string.recap_part_night_title
    DayPart.Morning -> R.string.recap_part_morning_title
    DayPart.Afternoon -> R.string.recap_part_afternoon_title
    DayPart.Evening -> R.string.recap_part_evening_title
}

private fun partName(part: DayPart): Int = when (part) {
    DayPart.Night -> R.string.recap_part_night
    DayPart.Morning -> R.string.recap_part_morning
    DayPart.Afternoon -> R.string.recap_part_afternoon
    DayPart.Evening -> R.string.recap_part_evening
}

// How thin a band of the stack can get so its name still fits
private const val MinimumBand = 0.12f
