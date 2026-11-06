package com.example.smartinventorypos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.theme.Danger
import com.example.smartinventorypos.ui.theme.DangerSoft
import com.example.smartinventorypos.ui.theme.Hairline
import com.example.smartinventorypos.ui.theme.Ink
import com.example.smartinventorypos.ui.theme.Mist
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.theme.Yellow
import com.example.smartinventorypos.ui.theme.YellowSoft

/** Side padding shared by every page. */
val PagePadding = 20.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    destructive: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.heightIn(min = 52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) Danger else Yellow,
            contentColor = if (destructive) Color.White else Ink,
            disabledContainerColor = Hairline,
            disabledContentColor = Muted
        ),
        elevation = null
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(contentColor = Brown)
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Small pill action for list rows. */
@Composable
fun SmallPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = CircleShape, color = Yellow, contentColor = Ink) {
        Box(Modifier.heightIn(min = 36.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Ink
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = if (filled) Yellow else Color.White,
        border = if (filled) null else BorderStroke(1.dp, Hairline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, Modifier.size(20.dp), tint = if (enabled) tint else Hairline)
        }
    }
}

/** Filled input with a yellow outline while focused. */
@Composable
fun PosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = shape,
        colors = fieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(1.5.dp, if (focused) Yellow else Color.Transparent, shape)
    )
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        colors = fieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Mist,
    unfocusedContainerColor = Mist,
    disabledContainerColor = Mist,
    errorContainerColor = Mist,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = Muted,
    unfocusedLabelColor = Muted,
    focusedLeadingIconColor = Muted,
    unfocusedLeadingIconColor = Muted,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
    cursorColor = Brown
)

/** Dark brown when selected, soft grey otherwise. */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        selected = selected,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) Brown else Mist,
        contentColor = if (selected) Yellow else Ink
    ) {
        Box(Modifier.heightIn(min = 40.dp).padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Page title with an optional back button and actions. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        onBack?.let {
            CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), it)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = Mist, content = content)
}

/** Stands in for a product photo: the first letter of the name. */
@Composable
fun Monogram(name: String, modifier: Modifier = Modifier, background: Color = YellowSoft) {
    Box(modifier.clip(MaterialTheme.shapes.small).background(background), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.titleLarge,
            color = Brown
        )
    }
}

@Composable
fun StockBadge(stock: Int, threshold: Int, modifier: Modifier = Modifier) {
    val (text, background, content) = when {
        stock <= 0 -> Triple(stringResource(R.string.out_of_stock), DangerSoft, Danger)
        stock <= threshold -> Triple(stringResource(R.string.low_stock), YellowSoft, Brown)
        else -> return
    }
    Box(
        modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = MaterialTheme.typography.labelSmall, color = content) }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(DangerSoft)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, Modifier.size(18.dp), tint = Danger)
        Text(message, style = MaterialTheme.typography.bodySmall, color = Danger)
    }
}

@Composable
fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(YellowSoft)
            .padding(12.dp)
    ) { Text(message, style = MaterialTheme.typography.bodySmall, color = Brown) }
}

@Composable
fun LoadingBar(modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
        color = Yellow,
        trackColor = Mist
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(YellowSoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, Modifier.size(28.dp), tint = Brown)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
    }
}

/** Dialog whose content scrolls, so forms still fit in landscape or with the keyboard open. */
@Composable
fun PosDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    busy: Boolean = false,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = MaterialTheme.shapes.large,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { content() }
        },
        confirmButton = {
            PrimaryButton(confirmText, onConfirm, destructive = destructive, busy = busy)
        },
        dismissButton = { QuietButton(stringResource(R.string.cancel), onDismiss) }
    )
}
