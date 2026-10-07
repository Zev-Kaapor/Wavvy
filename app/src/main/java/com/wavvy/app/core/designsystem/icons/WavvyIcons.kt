package com.wavvy.app.core.designsystem.icons

// UI graphics and vectors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// House
private const val HomePath = "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"

// Magnifying glass
private const val ExplorePath =
    "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16" +
        "c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5" +
        "S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"

// Globe outline with the continents cut out
private const val DiscoverPath =
    "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM11,19.93c-3.95,-0.49 " +
        "-7,-3.85 -7,-7.93 0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zM17.9,17.39c" +
        "-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3c0,-0.55 -0.45,-1 -1,-1L8,12v-2h2c0.55,0 1,-0.45 " +
        "1,-1L11,7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41 0,2.08 -0.8,3.97 -2.1,5.39z"

// Head and shoulders
private const val PersonPath =
    "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"

// Bell outline
private const val BellPath =
    "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.89,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5" +
        "s-1.5,0.67 -1.5,1.5v0.68C7.64,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2zM16,17H8v-6c0,-2.48 1.51,-4.5 4,-4.5s4,2.02 4,4.5v6z"

// Arrow pointing left
private const val BackPath = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"

// Arrow going into a door, drawn on a 32 unit grid
private const val LoginPath =
    "M12.219,26.156h6.094c1.156,0 2.125,-0.406 2.875,-1.188 0.75,-0.75 1.219,-1.75 1.219,-2.875v-12.219c0,-1.125 -0.469,-2.125 -1.219,-2.875s-1.75,-1.188 -2.875,-1.188h-6.094v2.563h6.094c0.875,0 1.531,0.656 1.531,1.5v12.219c0,0.844 -0.656,1.531 -1.531,1.531h-6.094v2.531z" +
        "M0,13.563v4.875c0,0.563 0.469,1.031 1.031,1.031h5.688v3.844c0,0.344 0.156,0.625 0.469,0.781 0.125,0.031 0.281,0.031 0.344,0.031 0.219,0 0.406,-0.063 0.563,-0.219l7.344,-7.344c0.281,-0.281 0.25,-0.844 0,-1.156l-7.344,-7.313c-0.25,-0.25 -0.563,-0.281 -0.906,-0.188 -0.313,0.156 -0.469,0.406 -0.469,0.75v3.875h-5.688c-0.563,0 -1.031,0.469 -1.031,1.031z"
private const val LoginGrid = 32f

// Puzzle piece
private const val IntegrationsPath =
    "M20.5,11H19V7c0,-1.1 -0.9,-2 -2,-2h-4V3.5C13,2.12 11.88,1 10.5,1S8,2.12 8,3.5V5H4c-1.1,0 -1.99,0.9 -1.99,2v3.8H3.5" +
        "c1.49,0 2.7,1.21 2.7,2.7s-1.21,2.7 -2.7,2.7H2V20c0,1.1 0.9,2 2,2h3.8v-1.5c0,-1.49 1.21,-2.7 2.7,-2.7 1.49,0 2.7,1.21 " +
        "2.7,2.7V22H17c1.1,0 2,-0.9 2,-2v-4h1.5c1.38,0 2.5,-1.12 2.5,-2.5S21.88,11 20.5,11z"

// Gear
private const val SettingsPath =
    "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61" +
        "l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 " +
        "-0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 " +
        "-0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l" +
        "-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94" +
        "l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96" +
        "c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6" +
        "s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z"

// Door with an arrow going out, drawn with a line on a 512 unit grid
private const val SignOutPath =
    "M340,120 V100 A80,80 0 0 0 260,20 H100 A80,80 0 0 0 20,100 V412 A80,80 0 0 0 100,492 H260 A80,80 0 0 0 340,412 V395 " +
        "M215,256 H492 M437,188 L489,240 Q502,256 489,272 L437,324"

// Door with an arrow going in, the mirror of the one going out
private const val SignInPath =
    "M172,120 V100 A80,80 0 0 1 252,20 H412 A80,80 0 0 1 492,100 V412 A80,80 0 0 1 412,492 H252 A80,80 0 0 1 172,412 V395 " +
        "M20,256 H295 M240,188 L292,240 Q305,256 292,272 L240,324"
private const val LineIconGrid = 512f
private const val LineIconStroke = 40f

// Camera that marks a video, a rounded body and its lens on the same 512 unit grid
private const val VideoCameraPath =
    "M56,104H290A56,56 0 0 1 346,160V350A56,56 0 0 1 290,406H56A56,56 0 0 1 0,350V160A56,56 0 0 1 56,104Z" +
        "M374,190L462,139A33,33 0 0 1 512,168V344A33,33 0 0 1 462,373L374,322Z"

// Icons of the Home taken from Metrolist (GPL-3.0), drawn on the 960 unit grid of Material Symbols
private const val SymbolsGrid = 960f
private const val PlayPath = "M320,760v-560l440,280 -440,280ZM400,480ZM400,614 L610,480 400,346v268Z"
private const val ExplicitPath =
    "M360,680h240v-80L440,600v-80h160v-80L440,440v-80h160v-80L360,280v400ZM200,840q-33,0 -56.5,-23.5T120,760v-560" +
        "q0,-33 23.5,-56.5T200,120h560q33,0 56.5,23.5T840,200v560q0,33 -23.5,56.5T760,840L200,840ZM200,760h560v-560L200,200v560Z" +
        "M200,200v560,-560Z"
private const val ArrowForwardPath = "M647,520L160,520L160,440L647,440L423,216L480,160L800,480L480,800L423,744L647,520Z"
private const val NavigateNextPath = "M376,720L320,664L504,480L320,296L376,240L616,480L376,720Z"
private const val MoreVerticalPath =
    "M480,800Q447,800 423.5,776.5Q400,753 400,720Q400,687 423.5,663.5Q447,640 480,640Q513,640 536.5,663.5Q560,687 560,720" +
        "Q560,753 536.5,776.5Q513,800 480,800ZM480,560Q447,560 423.5,536.5Q400,513 400,480Q400,447 423.5,423.5Q447,400 480,400" +
        "Q513,400 536.5,423.5Q560,447 560,480Q560,513 536.5,536.5Q513,560 480,560ZM480,320Q447,320 423.5,296.5Q400,273 400,240" +
        "Q400,207 423.5,183.5Q447,160 480,160Q513,160 536.5,183.5Q560,207 560,240Q560,273 536.5,296.5Q513,320 480,320Z"

// Filled triangle, two bars and a note, the controls of the player
private const val PlayArrowPath = "M8,5v14l11,-7z"
private const val PausePath = "M6,19h4V5H6v14zM14,5v14h4V5h-4z"
private const val MusicNotePath = "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z"

// Arrow down, skips, queue, repeat, shuffle, share and hearts of the expanded player
private const val ArrowDownPath = "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6 1.41,-1.41z"
private const val ArrowUpPath = "M7.41,15.41L12,10.83l4.59,4.58L18,14l-6,-6 -6,6z"
private const val SkipPreviousPath =
    "M7,6c0.55,0 1,0.45 1,1v10c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1V7c0,-0.55 0.45,-1 1,-1zM10.66,12.82l5.77,4.07" +
        "c0.66,0.47 1.58,-0.01 1.58,-0.82V7.93c0,-0.81 -0.91,-1.28 -1.58,-0.82l-5.77,4.07c-0.57,0.4 -0.57,1.24 0,1.64z"
private const val SkipNextPath =
    "M7.58,16.89l5.77,-4.07c0.56,-0.4 0.56,-1.24 0,-1.63L7.58,7.11C6.91,6.65 6,7.12 6,7.93v8.14c0,0.81 0.91,1.28 1.58,0.82z" +
        "M16,7v10c0,0.55 0.45,1 1,1s1,-0.45 1,-1V7c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1z"
private const val QueueMusicPath =
    "M15,6H3v2h12V6zM15,10H3v2h12V10zM3,16h8v-2H3V16zM17,6v8.18C16.69,14.07 16.35,14 16,14c-1.66,0 -3,1.34 -3,3" +
        "s1.34,3 3,3s3,-1.34 3,-3V8h3V6H17z"
private const val RepeatPath = "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4z"
private const val ShufflePath =
    "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5V4h-5.5z" +
        "M14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z"
private const val SharePath =
    "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7" +
        "l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7" +
        "L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16" +
        "c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z"
private const val FavoritePath =
    "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3" +
        " 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z"
private const val FavoriteBorderPath =
    "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35" +
        "l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5" +
        " 4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z"
private const val LyricsPath =
    "M80,880v-720q0,-33 23.5,-56.5T160,80h440q33,0 56.5,23.5T680,160v17q-24,11 -44,27t-36,36v-80L160,160v527l47,-47h393v-160" +
        "q16,20 36,36t44,27v97q0,33 -23.5,56.5T600,720L240,720L80,880ZM240,560h160v-80L240,480v80ZM760,480q-50,0 -85,-35" +
        "t-35,-85q0,-50 35,-85t85,-35q11,0 21,2t19,5v-207h160v80h-80v240q0,50 -35,85t-85,35ZM240,440h280v-80L240,360v80Z" +
        "M240,320h280v-80L240,240v80ZM160,640v-480,480Z"

// Close, locks, bin, play next and drag handle of the queue
private const val ClosePath =
    "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"
private const val LockPath =
    "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10" +
        "c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1" +
        " 1.71,0 3.1,1.39 3.1,3.1v2z"
private const val LockOpenPath =
    "M12,17c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6h1.9" +
        "c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10" +
        "c0,-1.1 -0.9,-2 -2,-2zM18,20H6V10h12v10z"
private const val DeletePath = "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"
private const val PlaylistPlayPath = "M19,9H2v2h17V9zM19,5H2v2h17V5zM2,15h13v-2H2V15zM17,13v6l5,-3L17,13z"
private const val DragHandlePath = "M3,18h18v-2H3v2zM3,13h18v-2H3v2zM3,6v2h18V6H3z"

// Push pin that marks what is pinned to the speed dial, traced from the image of Zev on the 512 unit grid
private const val PinPath =
    "M9,510C4,507 2,501 3,497C3,493 13,481 40,448C60,423 84,398 133,349C161,320 185,296 185,295C185,295 165,275 141,251C111,221 98,206 97,204C95,197 99,191 111,183C133,168 163,169 183,185L187,188 L261,124L336,60 L334,55C327,39 331,17 343,5C348,1 351,-1 357,0" +
        "C362,1 506,145 509,152C515,168 481,185 458,177C455,176 452,175 451,175C450,174 447,177 443,182C439,186 411,220 379,256L322,323 L325,328C342,351 341,383 323,404C317,411 314,413 309,413L304,413 L261,371L219,329 L170,377C105,442 90,455 36,499C19,513 16,514 9,510Z"

// Arrow that points to the top start, to move a search to the field, and the clock of the history
// Wi-Fi arcs, mobile data bars and Wi-Fi crossed out, the connection of the device
private const val WifiPath =
    "M1,9l2,2c4.97,-4.97 13.03,-4.97 18,0l2,-2C16.93,2.93 7.08,2.93 1,9zM9,17l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0zM5,13l2,2c2.76,-2.76 7.24,-2.76 10,0l2,-2C15.14,9.14 8.87,9.14 5,13z"
private const val CellularPath = "M17,4h3v16h-3V4zM5,14h3v6H5v-6zM11,9h3v11h-3V9z"
private const val WifiOffPath =
    "M23.64,7c-0.45,-0.34 -4.93,-4 -11.64,-4 -1.5,0 -2.89,0.19 -4.15,0.48L18.18,13.8 23.64,7zM3.41,1.31L2,2.72l2.05,2.05C1.91,5.76 0.59,6.82 0.36,7l11.63,14.49 0.01,0.01 0.01,-0.01 3.9,-4.86 3.32,3.32 1.41,-1.41L3.41,1.31z"

// Waves around a dot, a mix that starts from the artist
private const val MixPath =
    "M7.76,16.24C6.67,15.16 6,13.66 6,12s0.67,-3.16 1.76,-4.24l1.42,1.42C8.45,9.9 8,10.9 8,12c0,1.1 0.45,2.1 1.17,2.83L7.76,16.24z" +
        "M16.24,16.24l-1.42,-1.42C15.55,14.1 16,13.1 16,12c0,-1.1 -0.45,-2.1 -1.17,-2.83l1.42,-1.42C17.33,8.84 18,10.34 18,12S17.33,15.16 16.24,16.24z" +
        "M12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2S13.1,10 12,10z" +
        "M20,12c0,2.21 -0.9,4.21 -2.35,5.65l1.42,1.42C20.88,17.26 22,14.76 22,12s-1.12,-5.26 -2.93,-7.07l-1.42,1.42C19.1,7.79 20,9.79 20,12z" +
        "M6.35,6.35L4.93,4.93C3.12,6.74 2,9.24 2,12s1.12,5.26 2.93,7.07l1.42,-1.42C4.9,16.21 4,14.21 4,12S4.9,7.79 6.35,6.35z"
private const val NorthWestPath = "M5,15h2V8.41L18.59,20 20,18.59 8.41,7H15V5H5v10z"
private const val HistoryPath =
    "M13,3c-4.97,0 -9,4.03 -9,9H1l3.89,3.89 0.07,0.14L9,12H6c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06" +
        "l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08V8H12z"

// List with checks, the selection of several songs
private const val ChecklistPath =
    "M22,7h-9v2h9V7zM22,15h-9v2h9V15zM5.54,11L2,7.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,11z" +
        "M5.54,19L2,15.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,19z"

// Magnifying glass of the search
private const val SearchPath =
    "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16" +
        "c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5" +
        " 14,7.01 14,9.5 11.99,14 9.5,14z"

// Two letters of different scripts, the translation of the lyrics
private const val TranslatePath =
    "M12.87,15.07l-2.54,-2.51 0.03,-0.03c1.74,-1.94 2.98,-4.17 3.71,-6.53L17,6V4h-7V2H8v2H1v1.99h11.17C11.5,7.92 10.44,9.75 9,11.35" +
        " 8.07,10.32 7.3,9.19 6.69,8h-2c0.73,1.63 1.73,3.17 2.98,4.56l-5.09,5.02L4,19l5,-5 3.11,3.11 0.76,-2.04z" +
        "M18.5,10h-2L12,22h2l1.12,-3h4.75L21,22h2l-4.5,-12zM15.88,17l1.62,-4.33L19.12,17h-3.24z"

// Bulleted list
private const val LibraryPath =
    "M3,13h2v-2L3,11v2zM3,17h2v-2L3,15v2zM3,9h2L5,7L3,7v2zM7,13h14v-2L7,11v2zM7,17h14v-2L7,15v2zM7,7v2h14L21,7L7,7z"

// Material icons are drawn on a 24 unit grid and shown at 24dp
private const val IconGrid = 24f
private val IconSize = 24.dp

// Builds an icon from path data
private fun icon(name: String, pathData: String, grid: Float = IconGrid): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconSize,
        defaultHeight = IconSize,
        viewportWidth = grid,
        viewportHeight = grid
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        // Icons are tinted by the caller, so the fill color does not matter
        fill = SolidColor(Color.Black)
    ).build()

// Builds an icon drawn with a rounded line instead of a fill
private fun lineIcon(name: String, pathData: String, grid: Float, strokeWidth: Float): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconSize,
        defaultHeight = IconSize,
        viewportWidth = grid,
        viewportHeight = grid
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        // Icons are tinted by the caller, so the line color does not matter
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()

// Icons drawn in code so no icon library has to ship with the app
object WavvyIcons {
    val Home: ImageVector by lazy { icon("Home", HomePath) }
    val Explore: ImageVector by lazy { icon("Explore", ExplorePath) }
    val Discover: ImageVector by lazy { icon("Discover", DiscoverPath) }
    val Library: ImageVector by lazy { icon("Library", LibraryPath) }
    val Person: ImageVector by lazy { icon("Person", PersonPath) }
    val Bell: ImageVector by lazy { icon("Bell", BellPath) }
    val Back: ImageVector by lazy { icon("Back", BackPath) }
    val Explicit: ImageVector by lazy { icon("Explicit", ExplicitPath, SymbolsGrid) }
    val Play: ImageVector by lazy { icon("Play", PlayPath, SymbolsGrid) }
    val ArrowForward: ImageVector by lazy { icon("ArrowForward", ArrowForwardPath, SymbolsGrid) }
    val NavigateNext: ImageVector by lazy { icon("NavigateNext", NavigateNextPath, SymbolsGrid) }
    val MoreVertical: ImageVector by lazy { icon("MoreVertical", MoreVerticalPath, SymbolsGrid) }
    val PlayArrow: ImageVector by lazy { icon("PlayArrow", PlayArrowPath) }
    val Pause: ImageVector by lazy { icon("Pause", PausePath) }
    val MusicNote: ImageVector by lazy { icon("MusicNote", MusicNotePath) }
    val ArrowDown: ImageVector by lazy { icon("ArrowDown", ArrowDownPath) }
    val ArrowUp: ImageVector by lazy { icon("ArrowUp", ArrowUpPath) }
    val SkipPrevious: ImageVector by lazy { icon("SkipPrevious", SkipPreviousPath) }
    val SkipNext: ImageVector by lazy { icon("SkipNext", SkipNextPath) }
    val QueueMusic: ImageVector by lazy { icon("QueueMusic", QueueMusicPath) }
    val Repeat: ImageVector by lazy { icon("Repeat", RepeatPath) }
    val Shuffle: ImageVector by lazy { icon("Shuffle", ShufflePath) }
    val Share: ImageVector by lazy { icon("Share", SharePath) }
    val Favorite: ImageVector by lazy { icon("Favorite", FavoritePath) }
    val FavoriteBorder: ImageVector by lazy { icon("FavoriteBorder", FavoriteBorderPath) }
    val Lyrics: ImageVector by lazy { icon("Lyrics", LyricsPath, SymbolsGrid) }
    val Translate: ImageVector by lazy { icon("Translate", TranslatePath) }
    val Close: ImageVector by lazy { icon("Close", ClosePath) }
    val Search: ImageVector by lazy { icon("Search", SearchPath) }
    val Checklist: ImageVector by lazy { icon("Checklist", ChecklistPath) }
    val Pin: ImageVector by lazy { icon("Pin", PinPath, LineIconGrid) }
    val Mix: ImageVector by lazy { icon("Mix", MixPath) }
    val Wifi: ImageVector by lazy { icon("Wifi", WifiPath) }
    val Cellular: ImageVector by lazy { icon("Cellular", CellularPath) }
    val WifiOff: ImageVector by lazy { icon("WifiOff", WifiOffPath) }
    val NorthWest: ImageVector by lazy { icon("NorthWest", NorthWestPath) }
    val History: ImageVector by lazy { icon("History", HistoryPath) }
    val Lock: ImageVector by lazy { icon("Lock", LockPath) }
    val LockOpen: ImageVector by lazy { icon("LockOpen", LockOpenPath) }
    val Delete: ImageVector by lazy { icon("Delete", DeletePath) }
    val PlaylistPlay: ImageVector by lazy { icon("PlaylistPlay", PlaylistPlayPath) }
    val DragHandle: ImageVector by lazy { icon("DragHandle", DragHandlePath) }
    val VideoCamera: ImageVector by lazy { icon("VideoCamera", VideoCameraPath, LineIconGrid) }
    val Login: ImageVector by lazy { icon("Login", LoginPath, LoginGrid) }
    val Integrations: ImageVector by lazy { icon("Integrations", IntegrationsPath) }
    val Settings: ImageVector by lazy { icon("Settings", SettingsPath) }
    val SignOut: ImageVector by lazy { lineIcon("SignOut", SignOutPath, LineIconGrid, LineIconStroke) }
    val SignIn: ImageVector by lazy { lineIcon("SignIn", SignInPath, LineIconGrid, LineIconStroke) }
}
