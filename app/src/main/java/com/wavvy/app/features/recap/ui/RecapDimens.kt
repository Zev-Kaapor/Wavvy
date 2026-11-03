package com.wavvy.app.features.recap.ui

// UI styling and utilities
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes of the retrospective
object RecapDimens {
    // How many places a list shows at first and after it opens
    const val PreviewCount = 10
    const val FullCount = 50

    // The room between the parts and at the end of the screen
    val SectionGap = 8.dp
    val ScreenBottom = 24.dp

    // The hours on top of the list of numbers, their size and line, and the room around them
    val HeroNumberSize = 72.sp
    val HeroNumberLine = 76.sp
    val HeroTop = 16.dp
    val HeroCaptionGap = 4.dp

    // The numbers under the hours, and the room between them and over them
    val StatTop = 24.dp
    val StatGap = 16.dp

    // The bars of the months, the height of the chart, how thin a bar can get, how wide it is of its place and the gap to its name
    val ChartHeight = 140.dp
    val BarCorner = 6.dp
    const val BarMinFraction = 0.02f
    const val BarWidthFraction = 0.72f
    val BarLabelGap = 6.dp
    const val BarAlpha = 0.35f

    // The line that names the biggest month
    val PeakTop = 12.dp

    // A genre, the height of its bar, the room between the bar and its name and between two genres
    val GenreBar = 8.dp
    val GenreBarCorner = 4.dp
    val GenreBarGap = 6.dp
    val GenreGap = 14.dp
    const val GenreTrackAlpha = 0.12f

    // The button that opens the rest of a list
    val MoreTop = 4.dp

    // The pages of the story, the room around their words, and the bars on top that show where the story is
    val PageSide = 28.dp
    val PageGap = 14.dp
    val IntroGap = 4.dp
    val IntroBlockGap = 36.dp
    val ProgressTop = 8.dp
    val ProgressHeight = 3.dp
    val ProgressGap = 4.dp
    val ProgressSide = 12.dp
    val RoundButton = 36.dp
    val RoundButtonIcon = 20.dp

    // The words of the pages: the small line over, the big one and the one under, and the year of the first page
    val LeadSize = 20.sp
    val BigSize = 96.sp
    val BigLine = 96.sp
    val NameSize = 40.sp
    val NameLine = 42.sp
    val CaptionSize = 17.sp
    val YearSize = 120.sp
    val YearLine = 84.sp
    val YearTrimTop = 14.dp
    val YearTrimBottom = 21.dp
    // How long the digits of the year roll, how many digits go by on the last one before it stops and how many less on each place to the left
    const val YearRollMillis = 2600
    const val YearRollSteps = 13
    const val YearRollDrop = 3
    val TeaseGap = 20.dp
    // The crown over the favorite artist, and the light that crosses the name: how long it takes, how long it waits, how wide it is and how much it leans
    val ArtistNoteSize = 14.sp
    val ArtistNoteGap = 32.dp
    // The names of the months under the timeline, and the room under the words at the foot of a page
    val MonthLabelSize = 10.sp
    val FooterGap = 24.dp
    // The places with nobody on them: how many, how much each one fades from the one before, and the two lines where the name would be
    const val PlaceholderRows = 5
    const val PlaceholderFade = 0.15f
    val PlaceholderLine = 12.dp
    val PlaceholderLineGap = 8.dp
    const val PlaceholderLong = 0.55f
    const val PlaceholderShort = 0.3f
    // The page of the most played song: its title in one line, how long before it starts to scroll when it does not fit, the line of the artist and how far it is lifted, and the space above the words about the plays and how far they are dropped, which does not move the rest
    val SongTitleSize = 28.sp
    val SongTitleLine = 34.sp
    const val MarqueeDelayMillis = 1200
    val SongArtistSize = 20.sp
    val SongArtistRaise = 10.dp
    val SongNoteGap = 90.dp
    val SongNoteDrop = 48.dp
    // The case of the disc around the cover of the album: the spine, how round the corners are, the edge of the plastic, the hinges on the spine and the two bands of light over the cover
    val CaseSpine = 24.dp
    val CaseCorner = 6.dp
    val CaseFrame = 2.dp
    const val CaseHingeTop = 0.1f
    const val CaseHingeHeight = 0.08f
    const val CaseHingeInset = 0.28f
    const val CaseGlareFirst = 0.3f
    const val CaseGlareWide = 0.16f
    const val CaseGlareSecond = 0.7f
    const val CaseGlareThin = 0.06f
    const val CaseGlareLean = 0.35f
    // The record behind the cover of the most played song: its size, how far it comes out of the cover, how long a turn takes, the grooves, the label, the hole and the light on it
    val VinylSize = 200.dp
    val VinylOut = 96.dp
    const val VinylMillis = 14000
    const val VinylGrooves = 5
    const val VinylGrooveFirst = 0.92f
    const val VinylGrooveStep = 0.1f
    val VinylGrooveStroke = 1.dp
    const val VinylLabel = 0.32f
    const val VinylHole = 0.05f
    const val VinylGlare = 0.12f
    const val VinylGlareStart = 200f
    const val VinylGlareSweep = 40f
    // The arrow that shows the way on the first page: its size, how thick it is, how far it rises and how long that takes
    val HintWidth = 18.dp
    val HintHeight = 30.dp
    val HintStroke = 3.dp
    val HintTravel = 20.dp
    const val HintMillis = 1600
    // The ring of the photo of the artist: how thick it is, how far from the photo, how long a turn takes, and how much the crown sits over it
    val PhotoRing = 8.dp
    val PhotoRingGap = 6.dp
    const val PhotoRingMillis = 6000
    val CrownLap = 14.dp
    const val CrownTilt = 16f
    val CrownRoom = 14.dp
    val NewcomerTitleGap = 30.dp
    val NewcomerBodyGap = 10.dp
    val CrownWidth = 96.dp
    val CrownHeight = 72.dp
    const val ShineSweepMillis = 2600
    const val ShinePauseMillis = 5000
    const val ShineBand = 0.45f
    const val ShineTilt = 0.3f
    val TeaseSize = 32.sp
    val TeaseLine = 36.sp

    // How a word comes in: how long it takes, how far it rises and how long to wait between two words
    const val RevealMillis = 650
    const val RevealRise = 6
    const val RevealStep = 220
    const val CountMillis = 1800

    // The cover of a song, the photo of an artist and the small ones of the lists
    val Cover = 220.dp
    val RowCover = 60.dp
    val RowCorner = 4.dp
    val RowPlace = 40.dp
    val RowGap = 14.dp
    val PlaceSize = 36.sp
    const val CoverRequest = 600

    // The same for the lists of ten, smaller so all of them fit, and the room between two rows
    val RowCoverSmall = 44.dp
    val RowPlaceSmall = 32.dp
    val PlaceSizeSmall = 24.sp
    val ListGap = 8.dp

    // The lines of the artists through the year: how thick they are and the legend under them
    val TimelineStroke = 4.dp
    val LegendDot = 12.dp
    val LegendGap = 8.dp

    // The star and the flower: how many points, how deep they are cut, how many petals, how big a petal is and how long a turn takes
    const val StarPoints = 14
    const val StarInner = 0.8f
    const val PetalCount = 8
    const val PetalSize = 0.2f
    const val StarLapMillis = 120000

    // The shapes in the corners of a page, how many arrangements there are, their sizes, how far out of the screen they sit and how far from the top the ones that avoid the buttons start
    const val DecorVariants = 10
    const val DecorCalm = -1
    const val DecorSong = -2
    const val DecorSongsTease = -3
    const val DecorTopSongs = -4
    const val DecorMinutes = -5
    const val DecorMissed = -6
    val SparklesSmall = 100.dp
    const val RingsOut = 0.45f
    val DotsHigh = 108.dp
    const val DecorSink = 0.55f
    // The regular shapes of the calm arrangement: the sides of the one in the corner, how thick a line of them is, and the little ones low in the page, how big they are, the space between and how far from the bottom
    const val PolygonCornerSides = 7
    val PolygonStroke = 6.dp
    val PolygonStrip = 34.dp
    val PolygonStripGap = 18.dp
    val PolygonStripBottom = 150.dp
    val DecorBig = 260.dp
    val DecorSmall = 160.dp
    val DecorWide = 220.dp
    val DecorTop = 140.dp
    const val DecorOut = 0.3f
    const val DecorRise = 0.1f
    const val ZigzagSteps = 8
    val ZigzagThickness = 10.dp
    val ZigzagWidth = 180.dp
    val ZigzagHeight = 60.dp
    val CheckerCell = 28.dp
    val CheckerWidth = 420.dp
    val CheckerHeight = 84.dp

    // How the shapes move: how long a lap of the slow ones, of the breathing ones, of the ones that flow and of the ones that slide takes, how deep the breath is, and the swell of the dots
    const val SlowLapMillis = 50000
    const val PulseMillis = 7000
    const val ShapeLoopMillis = 9000
    const val SlideMillis = 12000
    const val PulseDepth = 0.04f
    const val DotSwell = 0.25f
    const val DotSwellStep = 0.5f
    const val SparkleLeast = 0.35f
    const val SparkleOffset = 1.7f
    const val ArchStep = 0.6f
    val ZigzagBob = 8.dp
    val WaveTop = 84.dp
    val WaveSink = 44.dp

    // The other shapes: the wavy round one, the dots, the lines of rings, the stars with four points, the waves, the rhombuses, the steps and the rays
    const val BlobBase = 0.85f
    const val BlobWobble = 0.12f
    const val BlobWobbleAlt = 0.07f
    const val BlobLobes = 3
    const val BlobPoints = 72
    val DotCell = 22.dp
    const val DotMax = 0.45f
    val DotsWidth = 200.dp
    val DotsHeight = 110.dp
    const val RingCount = 5
    val RingStroke = 6.dp
    val SparklesBox = 160.dp
    const val SparkleSize = 0.16f
    const val SparkleShrink = 0.2f
    val SparklePlaces = listOf(Offset(0.2f, 0.25f), Offset(0.7f, 0.15f), Offset(0.5f, 0.6f), Offset(0.85f, 0.75f))
    const val WaveCount = 4
    const val WaveSteps = 60
    const val WaveAmplitude = 0.08f
    const val WaveTurns = 2f
    val WaveThickness = 12.dp
    val WaveWidth = 220.dp
    val WaveHeight = 140.dp
    val DiamondCell = 40.dp
    val DiamondHeight = 80.dp
    val StairStep = 36.dp
    val StairHeight = 180.dp
    const val ArchCount = 5
    val ArchStroke = 12.dp

    // The podium of the artists: the photo of the first and of the others, how tall each step is, how round its top is, the space between steps and the sizes of the words
    val PodiumPhotoFirst = 112.dp
    val PodiumPhoto = 88.dp
    val PodiumStepFirst = 150.dp
    val PodiumStepSecond = 118.dp
    val PodiumStepThird = 92.dp
    val PodiumCorner = 16.dp
    val PodiumGap = 8.dp
    val PodiumNameGap = 8.dp
    val PodiumPhraseHeight = 84.dp
    val PodiumImpactStroke = 5.dp
    // The moments of the podium, in milliseconds: the title alone, the step rising, the time listened showing, the wait before each artist (longer for the first), the stamp, the name, how long the words stay and the end
    const val PodiumIntroMillis = 1500
    const val PodiumRiseMillis = 500
    const val PodiumTimeMillis = 350
    const val PodiumTeaseMillis = 2200
    const val PodiumFirstTeaseMillis = 3400
    const val PodiumStampMillis = 320
    const val PodiumNameMillis = 350
    const val PodiumShowMillis = 1900
    const val PodiumEndMillis = 1800
    const val PodiumFadeMillis = 300
    const val PodiumCallScale = 2.2f
    val PodiumCallSize = 34.sp
    const val PodiumDotMillis = 450
    const val PodiumImpactMillis = 500
    const val PodiumImpactGrow = 0.7f
    const val PodiumStampScale = 2.6f
    const val PodiumStampTilt = -14f
    val PodiumNameSize = 15.sp
    val PodiumRankSize = 36.sp
    val PodiumTimeSize = 14.sp

    // The stack of genres: how wide and how tall it is, and how round its ends are
    val Stack = 300.dp
    val StackHeight = 360.dp
    val StackEnd = 48.dp
    val StackGap = 4.dp
    val StackLabel = 16.sp

    // The parts of the day: how tall the bars are and how far apart
    val ClockHeight = 240.dp
    val ClockLabel = 52.dp
    val ClockBarCorner = 12.dp
    val ClockGap = 10.dp

    // The card of the kind of listener, its size, its corner and padding, and the shape on it
    val CardWidth = 280.dp
    val CardHeight = 340.dp
    val CardCorner = 24.dp
    val CardPadding = 24.dp
    const val CardLean = 6f
    val CardShape = 150.dp

    // The bars of the months on a page, and the buttons at the end
    val StoryChart = 220.dp
    val ButtonCorner = 28.dp
    val ButtonLine = 2.dp
    const val StoryMillis = 9500
    const val PageMillis = 650
    const val HoldMillis = 200L
    val ButtonHeight = 52.dp

    // The card of the profile that leads here, its corner and padding, and the icon in it
    val ProfileCardCorner = 16.dp
    val ProfileCardPadding = 16.dp
    val ProfileCardIcon = 28.dp
    val ProfileCardGap = 12.dp
    const val ProfileCardAlpha = 0.12f
}
