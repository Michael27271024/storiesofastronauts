package com.example.astronautstories.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.astronautstories.assets_loader.NativeBitmapRegistry

@Composable
fun AssetSpriteImage(
    bitmapRegistry: NativeBitmapRegistry,
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxDimension: Int = 256,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val bmp = remember(assetPath, maxDimension) {
        bitmapRegistry.getBitmap(assetPath, maxDimension)
    }
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(fallbackColor))
    }
}

/**
 * Uniformly framed badge/thumbnail slot so sprites of varying aspect ratios
 * (e.g. wide weapons/ships vs. tall astronauts vs. square emblems) render at
 * balanced, consistent visual proportions across every screen.
 */
@Composable
fun UniformAssetBadge(
    bitmapRegistry: NativeBitmapRegistry,
    assetPath: String? = null,
    @DrawableRes drawableRes: Int? = null,
    contentDescription: String? = null,
    sizeDp: Dp = 40.dp,
    cornerRadius: Dp = 10.dp,
    innerPadding: Dp = 4.dp,
    darkBackdrop: Boolean = false,
    cropToBounds: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    val backdropColor = if (darkBackdrop) Color(0xFF09090D) else Color(0xFFEBE2D3)
    val scale = if (cropToBounds) ContentScale.Crop else ContentScale.Fit
    val pad = if (cropToBounds) 0.dp else innerPadding

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(shape)
            .background(backdropColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .padding(pad),
        contentAlignment = Alignment.Center
    ) {
        when {
            drawableRes != null -> {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = contentDescription,
                    contentScale = scale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            assetPath != null -> {
                AssetSpriteImage(
                    bitmapRegistry = bitmapRegistry,
                    assetPath = assetPath,
                    contentDescription = contentDescription,
                    contentScale = scale,
                    maxDimension = 192,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
