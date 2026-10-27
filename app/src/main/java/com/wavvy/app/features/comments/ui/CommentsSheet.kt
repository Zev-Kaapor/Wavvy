package com.wavvy.app.features.comments.ui

// Android configuration
import android.content.res.Configuration
// Back button of the system
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
// Image loading
import coil3.compose.AsyncImage
// Language of the device
import java.util.Locale
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.lyrics.LyricsTranslator
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.comments.data.Comment
import com.wavvy.app.features.comments.data.CommentSort
import com.wavvy.app.features.comments.data.CommentsRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.library.ui.Chip

// The rules of the community of YouTube that its own notice links to
private const val GuidelinesUrl = "https://support.google.com/youtube/answer/13209064"

// The language the translator gives when it could not tell the one of the text
private const val UnknownLanguage = "auto"

// What one list of the sheet is doing, waiting for its first page, showing it or failed
private enum class CommentsStatus { Loading, Content, Error }

// One list of the sheet, the comments of the album or the replies of one of them
private data class CommentList(
    val status: CommentsStatus = CommentsStatus.Loading,
    val comments: List<Comment> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false
)

// The name of an order on its chip
private val CommentSort.titleRes: Int
    get() = when (this) {
        CommentSort.Top -> R.string.comments_sort_top
        CommentSort.Newest -> R.string.comments_sort_newest
    }

// The sheet put aside while the page of who wrote a comment is open, it comes back as it was with the album
private var parked: CommentsState? = null

// The comments of an album and the replies of the one that is open, asked as the sheet needs them, with the translation of the ones in another language
@Stable
private class CommentsState(
    val albumId: String,
    private val repository: CommentsRepository
) {
    // Its own scope and the place of the list, so the sheet can be put aside while the page of who wrote a comment is open and come back as it was
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val listState = LazyListState()

    // True once the first comments were asked
    var isStarted = false
        private set

    var sort by mutableStateOf(CommentSort.Top)
        private set
    var list by mutableStateOf(CommentList())
        private set

    // The comment whose replies are open, and its replies
    var opened by mutableStateOf<Comment?>(null)
        private set
    var replies by mutableStateOf(CommentList())
        private set

    // The translation of each comment written in another language than the one of the device, and the ones shown translated
    var translations by mutableStateOf(emptyMap<String, String>())
        private set
    var translated by mutableStateOf(emptySet<String>())
        private set

    private val language = Locale.getDefault().language
    private val checked = mutableSetOf<String>()
    private val translating = Semaphore(CommentsDimens.TranslateParallel)
    private var listJob: Job? = null
    private var repliesJob: Job? = null

    // Asks the first comments in the order chosen, the token of the album is asked only once
    fun load() {
        isStarted = true
        listJob?.cancel()
        list = CommentList()
        listJob = scope.launch {
            val page = repository.threadToken(albumId).getOrNull()?.let { repository.comments(it, sort).getOrNull() }
            list = if (page == null) CommentList(CommentsStatus.Error) else CommentList(CommentsStatus.Content, page.comments, page.continuation)
            page?.let { translate(it.comments) }
        }
    }

    // Another order, the comments are asked again in it from the top
    fun select(sort: CommentSort) {
        if (sort == this.sort) return
        this.sort = sort
        load()
        scope.launch { listState.scrollToItem(0) }
    }

    // Adds the next comments when the list gets close to its end
    fun loadMore() {
        val current = list
        val continuation = current.continuation ?: return
        if (current.isLoadingMore || current.status != CommentsStatus.Content) return

        list = current.copy(isLoadingMore = true)
        listJob = scope.launch {
            val page = repository.more(continuation).getOrNull()
            list = if (page == null) {
                list.copy(isLoadingMore = false)
            } else {
                list.copy(comments = (list.comments + page.comments).distinctBy { it.id }, continuation = page.continuation, isLoadingMore = false)
            }
            page?.let { translate(it.comments) }
        }
    }

    // Opens the replies of a comment in the place of the list, the comment itself comes in the answer and is left out
    fun open(comment: Comment) {
        val target = comment.replies ?: return
        repliesJob?.cancel()
        opened = comment
        replies = CommentList()
        repliesJob = scope.launch {
            val page = repository.replies(target).getOrNull()
            replies = if (page == null) CommentList(CommentsStatus.Error) else CommentList(CommentsStatus.Content, page.comments.filter { it.isReply }, page.continuation)
            page?.let { translate(it.comments) }
        }
    }

    // Adds the next replies when their list gets close to its end
    fun loadMoreReplies() {
        val current = replies
        val continuation = current.continuation ?: return
        if (current.isLoadingMore || current.status != CommentsStatus.Content) return

        replies = current.copy(isLoadingMore = true)
        repliesJob = scope.launch {
            val page = repository.more(continuation).getOrNull()
            replies = if (page == null) {
                replies.copy(isLoadingMore = false)
            } else {
                replies.copy(comments = (replies.comments + page.comments.filter { it.isReply }).distinctBy { it.id }, continuation = page.continuation, isLoadingMore = false)
            }
            page?.let { translate(it.comments) }
        }
    }

    // Goes back from the replies to the list
    fun close() {
        repliesJob?.cancel()
        opened = null
    }

    // Stops what the sheet was still asking, once it is closed for good
    fun release() {
        scope.cancel()
    }

    // Shows a comment translated, or back as it was written
    fun toggleTranslation(id: String) {
        translated = if (id in translated) translated - id else translated + id
    }

    // Translates each comment once, a few at a time, and keeps the translation only of the ones written in another language
    private fun translate(comments: List<Comment>) {
        comments.filter { checked.add(it.id) }.forEach { comment ->
            scope.launch {
                translating.withPermit {
                    LyricsTranslator.translate(comment.text.split("\n"), language).onSuccess { result ->
                        val text = result.lines.joinToString("\n")
                        val isOther = !result.sourceLanguage.equals(language, ignoreCase = true) && result.sourceLanguage != UnknownLanguage
                        if (isOther && text != comment.text) translations = translations + (comment.id to text)
                    }
                }
            }
        }
    }
}

// The comments of an album in a sheet, as YouTube shows them, the orders on top, its notice and the comments, and the replies of one of them in their place
// Opening who wrote a comment puts the sheet aside and tells the page, which opens it again as it was when it comes back
@Composable
fun CommentsSheet(albumId: String, onDismiss: () -> Unit, onAuthorOpened: () -> Unit = {}) {
    val context = LocalContext.current
    val state = remember(albumId) {
        parked?.takeIf { it.albumId == albumId } ?: CommentsState(albumId, CommentsRepository(context)).also { parked?.release() }
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(state) {
        parked = null
        if (!state.isStarted) state.load()
    }

    WavvySheet(
        onDismiss = {
            if (parked !== state) state.release()
            onDismiss()
        }
    ) {
        val closeSheet = LocalSheetClose.current

        // Opens the page of who wrote a comment, the sheet is put aside to come back with the album
        val openAuthor: (Comment) -> (() -> Unit)? = { comment ->
            authorItemOf(comment)?.let { item ->
                {
                    parked = state
                    if (ItemNavigator.open(item)) {
                        onAuthorOpened()
                        closeSheet()
                    } else {
                        parked = null
                    }
                }
            }
        }

        // Back leaves the replies before it closes the sheet
        BackHandler(enabled = state.opened != null, onBack = state::close)

        // The replies come in from the side they are on, and the list comes back from the other
        AnimatedContent(
            targetState = state.opened,
            transitionSpec = {
                val side = if (targetState != null) 1 else -1
                (slideInHorizontally(tween(WavvyMotion.ScreenFadeMillis)) { side * it / WavvyMotion.PageSlideDivisor } + fadeIn(tween(WavvyMotion.ScreenFadeMillis)))
                    .togetherWith(fadeOut(tween(WavvyMotion.ScreenFadeMillis)))
            },
            label = "comments",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isLandscape) 1f else CommentsDimens.SheetFraction)
        ) { opened ->
            if (opened == null) {
                CommentsPage(state = state, onClose = closeSheet, openAuthor = openAuthor)
            } else {
                RepliesPage(state = state, comment = opened, onClose = closeSheet, openAuthor = openAuthor)
            }
        }
    }
}

// The title with the cross, the chips of the orders and the comments under the notice of what can be done here
@Composable
private fun CommentsPage(
    state: CommentsState,
    onClose: () -> Unit,
    openAuthor: (Comment) -> (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SheetHeader(title = stringResource(R.string.comments_title), onClose = onClose)

        Row(
            horizontalArrangement = Arrangement.spacedBy(CommentsDimens.ChipGap),
            modifier = Modifier.padding(start = CommentsDimens.ChipsHorizontal, end = CommentsDimens.ChipsHorizontal, bottom = CommentsDimens.ChipsBottom)
        ) {
            CommentSort.entries.forEach { sort ->
                Chip(text = stringResource(sort.titleRes), isOn = sort == state.sort) { state.select(sort) }
            }
        }

        CommentsList(
            list = state.list,
            state = state,
            listState = state.listState,
            onMore = state::loadMore,
            onRetry = state::load,
            openAuthor = openAuthor,
            modifier = Modifier.weight(1f)
        ) {
            item(key = "notice") { CommentNoticeBox() }
        }
    }
}

// The arrow back with the title, the comment that was opened standing out on top and its replies under it
@Composable
private fun RepliesPage(
    state: CommentsState,
    comment: Comment,
    onClose: () -> Unit,
    openAuthor: (Comment) -> (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SheetHeader(title = stringResource(R.string.comments_replies), onClose = onClose, onBack = state::close)

        CommentsList(
            list = state.replies,
            state = state,
            onMore = state::loadMoreReplies,
            onRetry = { state.open(comment) },
            openAuthor = openAuthor,
            modifier = Modifier.weight(1f)
        ) {
            item(key = "opened") {
                CommentRow(
                    comment = comment,
                    translation = state.translations[comment.id],
                    isTranslated = comment.id in state.translated,
                    onTranslate = { state.toggleTranslation(comment.id) },
                    onReplies = null,
                    onAuthor = openAuthor(comment),
                    modifier = Modifier.background(WavvyTheme.colors.menuCard)
                )
            }
        }
    }
}

// The title of the sheet with the cross that closes it, and the arrow that goes back from the replies
@Composable
private fun SheetHeader(title: String, onClose: () -> Unit, onBack: (() -> Unit)? = null) {
    val color = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(CommentsDimens.HeaderHeight)
            .padding(start = if (onBack == null) CommentsDimens.HeaderStart else CommentsDimens.HeaderEnd, end = CommentsDimens.HeaderEnd)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = color)
            }
            Spacer(modifier = Modifier.width(CommentsDimens.HeaderBackGap))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.merge(CommentsType.Title),
            color = color,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onClose) {
            Icon(imageVector = WavvyIcons.Close, contentDescription = stringResource(R.string.cd_close), tint = color)
        }
    }
}

// A list of comments, its placeholder while the first page comes, the message when it failed or has none, and the next ones as it scrolls
// What comes before the comments is given by the page, the notice of the list or the comment whose replies these are
@Composable
private fun CommentsList(
    list: CommentList,
    state: CommentsState,
    onMore: () -> Unit,
    onRetry: () -> Unit,
    // What opens the page of who wrote a comment
    openAuthor: (Comment) -> (() -> Unit)?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    top: LazyListScope.() -> Unit = {}
) {
    val fade = tween<Float>(WavvyMotion.ScreenFadeMillis)

    // True when the last rows on the screen are close to the end of the list
    val nearEnd by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - CommentsDimens.LoadMoreThreshold
        }
    }
    LaunchedEffect(nearEnd, list.continuation, list.status) {
        if (nearEnd && list.status == CommentsStatus.Content) onMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = CommentsDimens.ListBottom),
        modifier = modifier.fillMaxSize()
    ) {
        top()

        // The placeholder fades out under the comments that fade in, in the same places
        when (list.status) {
            CommentsStatus.Loading -> item(key = "skeleton") {
                CommentsSkeleton(modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = fade))
            }

            CommentsStatus.Error -> item(key = "error") {
                HomeMessage(
                    text = stringResource(R.string.comments_error),
                    modifier = Modifier.height(HomeDimens.EmptyHeight),
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = onRetry
                )
            }

            CommentsStatus.Content -> {
                if (list.comments.isEmpty()) {
                    item(key = "empty") {
                        HomeMessage(text = stringResource(R.string.comments_empty), modifier = Modifier.height(HomeDimens.EmptyHeight))
                    }
                }

                items(list.comments, key = { it.id }) { comment ->
                    CommentRow(
                        comment = comment,
                        translation = state.translations[comment.id],
                        isTranslated = comment.id in state.translated,
                        onTranslate = { state.toggleTranslation(comment.id) },
                        onReplies = comment.replies?.let { { state.open(comment) } },
                        onAuthor = openAuthor(comment),
                        modifier = Modifier.animateItem(fadeInSpec = fade, placementSpec = null, fadeOutSpec = null)
                    )
                }

                if (list.isLoadingMore) {
                    item(key = "more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(CommentsDimens.MorePadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(CommentsDimens.MoreSpinner))
                        }
                    }
                }
            }
        }
    }
}

// What can be done here, with the link to the rules of the community of YouTube
@Composable
private fun CommentNoticeBox() {
    val linkColor = MaterialTheme.colorScheme.primary
    val notice = stringResource(R.string.comments_notice)
    val link = stringResource(R.string.comments_notice_link)
    val text = remember(notice, link, linkColor) {
        buildAnnotatedString {
            append(notice)
            append(" ")
            withLink(LinkAnnotation.Url("$GuidelinesUrl?hl=${Locale.getDefault().toLanguageTag()}", TextLinkStyles(SpanStyle(color = linkColor)))) { append(link) }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.merge(CommentsType.Notice),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(WavvyTheme.colors.menuCard)
            .padding(horizontal = CommentsDimens.NoticeHorizontal, vertical = CommentsDimens.NoticeVertical)
    )
}

// The page of who wrote a comment, the page of an artist or the profile of a person, none when the comment has no channel
private fun authorItemOf(comment: Comment): HomeItem? {
    val channelId = comment.authorChannelId ?: return null

    return HomeItem(
        kind = HomeItemKind.Artist,
        id = channelId,
        title = comment.author,
        thumbnailUrl = comment.authorPhotoUrl,
        countText = comment.author,
        isProfile = !comment.isArtist
    )
}

// One comment, the photo of who wrote it with the line down to its replies, the name and the time, the text and the link that translates it,
// the like, the dislike and the reply, and the link that opens the replies
@Composable
private fun CommentRow(
    comment: Comment,
    translation: String?,
    isTranslated: Boolean,
    onTranslate: () -> Unit,
    onReplies: (() -> Unit)?,
    onAuthor: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val language = remember { Locale.getDefault().let { it.getDisplayLanguage(it) } }
    val repliesText = comment.repliesText.takeIf { onReplies != null }
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    // Where the middle of the link of the replies is, from the top of the row, so the line can curve into it
    var linkMiddle by remember { mutableFloatStateOf(0f) }
    val rowTop = with(LocalDensity.current) { CommentsDimens.CommentTop.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // The line down from the photo that curves into the link of the replies, as YouTube draws it
                if (repliesText != null && linkMiddle > 0f) {
                    val x = (CommentsDimens.CommentStart + CommentsDimens.Avatar / 2).toPx()
                    val top = (CommentsDimens.CommentTop + CommentsDimens.Avatar + CommentsDimens.ThreadLineGap).toPx()
                    val end = (CommentsDimens.CommentStart + CommentsDimens.Avatar + CommentsDimens.AvatarGap - CommentsDimens.ThreadLineGap).toPx()
                    val curve = CommentsDimens.ThreadCurve.toPx()
                    val line = Path().apply {
                        moveTo(x, top)
                        lineTo(x, linkMiddle - curve)
                        quadraticTo(x, linkMiddle, x + curve, linkMiddle)
                        lineTo(end, linkMiddle)
                    }
                    drawPath(path = line, color = lineColor, style = Stroke(width = CommentsDimens.ThreadLine.toPx()))
                }
            }
            .padding(start = CommentsDimens.CommentStart, end = CommentsDimens.CommentEnd, top = CommentsDimens.CommentTop)
    ) {
        AsyncImage(
            model = comment.authorPhotoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(CommentsDimens.Avatar)
                .clip(CircleShape)
                .background(WavvyTheme.colors.skeleton)
                .then(if (onAuthor != null) Modifier.clickable(onClick = onAuthor) else Modifier)
        )

        Column(modifier = Modifier.weight(1f).padding(start = CommentsDimens.AvatarGap)) {
            CommentHead(comment = comment, onAuthor = onAuthor)

            Text(
                text = if (isTranslated && translation != null) translation else comment.text,
                style = MaterialTheme.typography.bodyMedium.merge(CommentsType.Body),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = CommentsDimens.TextGap)
            )

            // Only for a comment written in another language than the one of the device
            if (translation != null) {
                Text(
                    text = if (isTranslated) stringResource(R.string.comments_original) else stringResource(R.string.comments_translate, language),
                    style = MaterialTheme.typography.bodyMedium.merge(CommentsType.Body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = CommentsDimens.TranslateTop)
                        .clickable(onClick = onTranslate)
                )
            }

            CommentActions(comment = comment, onReplies = onReplies)

            // How many replies with the arrow that opens them, where the line of the photo ends
            if (repliesText != null && onReplies != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .onGloballyPositioned { linkMiddle = rowTop + it.positionInParent().y + it.size.height / 2f }
                        .clickable(onClick = onReplies)
                        .padding(vertical = CommentsDimens.RepliesVertical)
                ) {
                    Text(
                        text = repliesText,
                        style = MaterialTheme.typography.bodyMedium.merge(CommentsType.Link),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = WavvyIcons.NavigateNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(CommentsDimens.RepliesArrow)
                    )
                }
            }
        }
    }
}

// Who wrote it, in a pill when it is the artist of the album, with the mark of a checked channel, and how long ago, the name opens their page
@Composable
private fun CommentHead(comment: Comment, onAuthor: (() -> Unit)?) {
    val style = MaterialTheme.typography.bodySmall.merge(CommentsType.Head)
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = comment.author,
            style = style,
            color = if (comment.isCreator) MaterialTheme.colorScheme.onSurface else secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .then(if (onAuthor != null) Modifier.clickable(onClick = onAuthor) else Modifier)
                .then(
                    if (comment.isCreator) {
                        Modifier
                            .clip(CircleShape)
                            .background(WavvyTheme.colors.chip)
                            .padding(horizontal = CommentsDimens.CreatorHorizontal, vertical = CommentsDimens.CreatorVertical)
                    } else {
                        Modifier
                    }
                )
        )

        if (comment.isVerified) {
            Icon(
                imageVector = WavvyIcons.Check,
                contentDescription = null,
                tint = secondary,
                modifier = Modifier.padding(start = CommentsDimens.VerifiedGap).size(CommentsDimens.VerifiedIcon)
            )
        }

        if (comment.publishedTime.isNotEmpty()) {
            Text(text = " • ${comment.publishedTime}", style = style, color = secondary, maxLines = 1)
        }
    }
}

// The like with how many liked it, the dislike and the reply, which opens the replies when there are some
@Composable
private fun CommentActions(comment: Comment, onReplies: (() -> Unit)?) {
    val tint = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = CommentsDimens.ActionsTop).height(CommentsDimens.ActionHeight)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = CommentsDimens.LikeMinWidth)) {
            Icon(
                imageVector = WavvyIcons.ThumbUp,
                contentDescription = stringResource(R.string.comments_like),
                tint = tint,
                modifier = Modifier.size(CommentsDimens.ActionIcon)
            )
            if (comment.likeCount.isNotEmpty()) {
                Text(
                    text = comment.likeCount,
                    style = MaterialTheme.typography.labelSmall.merge(CommentsType.Count),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = CommentsDimens.CountGap)
                )
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(CommentsDimens.ActionWidth)) {
            Icon(
                imageVector = WavvyIcons.ThumbDown,
                contentDescription = stringResource(R.string.comments_dislike),
                tint = tint,
                modifier = Modifier.size(CommentsDimens.ActionIcon)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(CommentsDimens.ActionWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .then(if (onReplies != null) Modifier.clickable(onClick = onReplies) else Modifier)
        ) {
            Icon(
                imageVector = WavvyIcons.Comment,
                contentDescription = stringResource(R.string.comments_reply),
                tint = tint,
                modifier = Modifier.size(CommentsDimens.ActionIcon)
            )
        }

        // The heart the artist gave it
        if (comment.isHearted) {
            Icon(
                imageVector = WavvyIcons.Favorite,
                contentDescription = comment.heartText,
                tint = WavvyTheme.colors.playerLiked,
                modifier = Modifier.size(CommentsDimens.ActionIcon)
            )
        }
    }
}

// Placeholder of the comments while they come, each one where a comment will be, the photo, the name, two lines and the buttons
@Composable
private fun CommentsSkeleton(modifier: Modifier = Modifier) {
    val line = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        repeat(CommentsDimens.SkeletonRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CommentsDimens.CommentStart, end = CommentsDimens.CommentEnd, top = CommentsDimens.CommentTop)
            ) {
                Box(modifier = Modifier.size(CommentsDimens.Avatar).skeleton(CircleShape))

                Column(modifier = Modifier.weight(1f).padding(start = CommentsDimens.AvatarGap)) {
                    Box(modifier = Modifier.fillMaxWidth(CommentsDimens.SkeletonHeadFraction).height(CommentsDimens.SkeletonLine).skeleton(line))
                    Spacer(modifier = Modifier.height(CommentsDimens.SkeletonGap))
                    Box(modifier = Modifier.fillMaxWidth(CommentsDimens.SkeletonTextFraction).height(CommentsDimens.SkeletonLine).skeleton(line))
                    Spacer(modifier = Modifier.height(CommentsDimens.SkeletonGap))
                    Box(modifier = Modifier.fillMaxWidth(CommentsDimens.SkeletonShortFraction).height(CommentsDimens.SkeletonLine).skeleton(line))
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(top = CommentsDimens.ActionsTop).height(CommentsDimens.ActionHeight)
                    ) {
                        Box(modifier = Modifier.width(CommentsDimens.SkeletonActionWidth).height(CommentsDimens.ActionIcon).skeleton(line))
                    }
                }
            }
        }
    }
}
