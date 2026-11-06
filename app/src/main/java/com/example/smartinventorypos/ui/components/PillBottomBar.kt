package com.example.smartinventorypos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.theme.Yellow
import com.example.smartinventorypos.ui.theme.YellowSoft

data class BottomItem<K>(val key: K, val label: String, val icon: ImageVector)

/** The selected destination expands into a soft yellow pill with its label. */
@Composable
fun <K> PillBottomBar(items: List<BottomItem<K>>, selected: K, onSelect: (K) -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.key == selected
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .selectable(isSelected, role = Role.Tab, onClick = { onSelect(item.key) })
                        .background(if (isSelected) YellowSoft else Color.Transparent)
                        .padding(end = if (isSelected) 18.dp else 0.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (isSelected) Yellow else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) Brown else Muted
                        )
                    }
                    if (isSelected) {
                        Text(item.label, style = MaterialTheme.typography.labelLarge, color = Brown)
                    }
                }
            }
        }
    }
}
