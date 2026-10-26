package com.wavvy.app.features.menu

// Android and Compose layouts
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.core.designsystem.components.ScrollIndicatorWidth
import com.wavvy.app.core.designsystem.components.scrollIndicator
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.player.ui.components.PlayerDimens

// The pieces of the menus drawn as cards, the ones of a song, of an album, of a playlist, of an artist, of a podcast and of the queue are made of them

// The head of a menu, the cover when there is one, the name in bold and the line under it in the color of the accent, with something at the end if asked
@Composable
internal fun CardMenuHead(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    coverUrl: String? = null,
    hasCover: Boolean = true,
    isProfile: Boolean = false,
    coverShape: Shape = RoundedCornerShape(ItemMenuDimens.HeaderCoverCorner),
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ItemMenuDimens.HeaderVertical)
    ) {
        if (hasCover) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ItemMenuDimens.HeaderCover)
                    .clip(coverShape)
                    .background(WavvyTheme.colors.skeleton)
            ) {
                // A person with no photo yet is the silhouette of the profile, under the photo that covers it when it comes
                if (isProfile) {
                    Icon(imageVector = WavvyIcons.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize(HomeDimens.ProfileSilhouette))
                }
                AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (hasCover) ItemMenuDimens.HeaderGap else ItemMenuDimens.CreditsTitleStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WavvyTheme.colors.playerAccent,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
        trailing?.invoke()
    }
}

// The cards of the top, side by side with the room above and under them
@Composable
internal fun CardMenuTiles(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardTileGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap, bottom = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap),
        content = content
    )
}

// The head and the options of a menu, one under the other, and lying down, where the screen is too short for all of them, the head on the start and the options scrolling on the end
@Composable
internal fun CardMenuLayout(head: @Composable () -> Unit, options: @Composable () -> Unit) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGroupGap),
            modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.LandscapeBottom)
        ) {
            // The head sits in the middle of the height the options take
            Column(
                verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
                modifier = Modifier
                    .weight(ItemMenuDimens.LandscapeHeadWeight)
                    .align(Alignment.CenterVertically)
            ) { head() }
            // The bar on the end tells there are more options below
            val optionsScroll = rememberScrollState()
            Column(
                verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
                modifier = Modifier
                    .weight(1f - ItemMenuDimens.LandscapeHeadWeight)
                    .scrollIndicator(optionsScroll)
                    .verticalScroll(optionsScroll)
                    .padding(end = ScrollIndicatorWidth + ItemMenuDimens.LandscapeIndicatorGap)
            ) { options() }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
            modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = PlayerDimens.OptionsBottom)
        ) {
            head()
            options()
        }
    }
}

// A card that comes after the menu is open, such as the ones that need the page of what the menu is for, it fades in and opens its room instead of showing all of a sudden
@Composable
internal fun MenuReveal(content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(tween(WavvyMotion.ExpandMillis)) + expandVertically(tween(WavvyMotion.ExpandMillis)),
        exit = fadeOut(tween(WavvyMotion.ExpandMillis)) + shrinkVertically(tween(WavvyMotion.ExpandMillis))
    ) { content() }
}

// A menu that is only a few cards, with no head
@Composable
internal fun CardMenuList(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
        modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = PlayerDimens.OptionsBottom)
    ) { content() }
}

// One of the cards of the top, the icon in the color of the accent over its name
@Composable
internal fun MenuCardTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .alpha(if (enabled) 1f else ItemMenuDimens.DisabledAlpha)
            .height(ItemMenuDimens.CardTileHeight)
            .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
            .background(WavvyTheme.colors.menuCard)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(ItemMenuDimens.CardTilePadding)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WavvyTheme.colors.playerAccent,
            modifier = Modifier.size(ItemMenuDimens.CardTileIcon)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .padding(top = ItemMenuDimens.CardTileLabelTop)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )
    }
}

// One option as a card, the icon in the color of the accent, its name and a line under it when there is one
@Composable
internal fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
            .background(WavvyTheme.colors.menuCard)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WavvyTheme.colors.playerAccent,
            modifier = Modifier.size(ItemMenuDimens.CardRowIcon)
        )
        Column(modifier = Modifier.weight(1f).padding(start = ItemMenuDimens.CardRowTextStart)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
