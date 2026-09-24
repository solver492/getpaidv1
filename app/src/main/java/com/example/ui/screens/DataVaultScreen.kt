package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StructuredDataPoint
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DestructiveGlow
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PlatformViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DataVaultScreen(
    viewModel: PlatformViewModel,
    modifier: Modifier = Modifier
) {
    val allData by viewModel.allStructuredData.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf("all") }
    var selectedFilterStatus by remember { mutableStateOf("all") }

    val filteredList = allData.filter { item ->
        val matchesCategory = selectedFilterCategory == "all" || item.category == selectedFilterCategory
        val matchesStatus = when (selectedFilterStatus) {
            "valid" -> item.isValid
            "flagged" -> !item.isValid
            else -> true
        }
        matchesCategory && matchesStatus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("data_vault_screen")
    ) {
        // Filter bar
        Surface(
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REGISTRE DE DONNÉES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "${filteredList.size} entrées",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Category filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterPill(
                            label = "Toutes rubriques",
                            selected = selectedFilterCategory == "all",
                            onClick = { selectedFilterCategory = "all" }
                        )
                    }
                    items(categories) { cat ->
                        FilterPill(
                            label = "${cat.emoji} ${cat.name}",
                            selected = selectedFilterCategory == cat.slug,
                            onClick = { selectedFilterCategory = cat.slug }
                        )
                    }
                }

                // Status filters
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterPill(
                        label = "Tous statuts",
                        selected = selectedFilterStatus == "all",
                        onClick = { selectedFilterStatus = "all" }
                    )
                    FilterPill(
                        label = "Valides",
                        selected = selectedFilterStatus == "valid",
                        onClick = { selectedFilterStatus = "valid" }
                    )
                    FilterPill(
                        label = "Signalés",
                        selected = selectedFilterStatus == "flagged",
                        onClick = { selectedFilterStatus = "flagged" }
                    )
                }
            }
        }

        // List of entries
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AUCUN ENREGISTREMENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Discute avec Alex dans l'onglet Chat pour générer des données certifiées !",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    DataVaultItemCard(item)
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) EmeraldPrimary.copy(alpha = 0.2f) else SurfaceVariantDark
    val border = if (selected) EmeraldPrimary else BorderSubtle

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

@Composable
fun DataVaultItemCard(item: StructuredDataPoint) {
    var expandedJson by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.product,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                if (item.isValid) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldGlow)
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VALIDE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DestructiveGlow)
                            .border(1.dp, DestructiveRed.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SIGNALÉ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DestructiveRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Marque: ${item.brand}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EmeraldLight
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "${item.price} ${item.currency}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lieu: ${item.location} (${item.frequency})",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            if (!item.isValid && item.flagReason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Alerte: ${item.flagReason}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DestructiveRed
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clickable { expandedJson = !expandedJson }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (expandedJson) "MASQUER JSON" else "VOIR PAYLOAD JSON",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = EmeraldPrimary,
                    letterSpacing = 0.5.sp
                )
            }

            AnimatedVisibility(visible = expandedJson) {
                val jsonPreview = """
{
  "id": "${item.id}",
  "category": "${item.category}",
  "product": "${item.product}",
  "brand": "${item.brand}",
  "price": ${item.price},
  "currency": "${item.currency}",
  "location": "${item.location}",
  "frequency": "${item.frequency}",
  "isValid": ${item.isValid}
}
                """.trimIndent()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF070B10))
                        .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = jsonPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = EmeraldLight
                    )
                }
            }
        }
    }
}
