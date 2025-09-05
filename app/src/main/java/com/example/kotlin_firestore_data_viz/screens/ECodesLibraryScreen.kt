package com.example.kotlin_firestore_data_viz.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kotlin_firestore_data_viz.data.AdditiveLocal
import com.example.kotlin_firestore_data_viz.data.LocalAdditives
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ECodesLibraryScreen(navController: NavController) {
    val context = LocalContext.current
    val all: List<AdditiveLocal> = remember { LocalAdditives.all(context) }

    var query by remember { mutableStateOf("") }
    var selectedCategory: String? by remember { mutableStateOf(null) }

    val categories: List<String> = remember(all) {
        all.map { it.category ?: eCodeCategory(it.code) }
            .toSet()
            .toList()
            .sorted()
    }

    val filtered: List<AdditiveLocal> = remember(all, query, selectedCategory) {
        val q = query.trim().lowercase(Locale.ROOT)
        all.filter { a ->
            val cat = a.category ?: eCodeCategory(a.code)
            val matchesQuery =
                q.isBlank() ||
                        a.code.lowercase(Locale.ROOT).contains(q) ||
                        (a.nameFi ?: "").lowercase(Locale.ROOT).contains(q) ||
                        (a.nameEn ?: "").lowercase(Locale.ROOT).contains(q)
            val matchesCat = selectedCategory == null || selectedCategory == cat
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "E-koodit / E-codes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${filtered.size} of ${all.size} additives",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
        ) {
            // Enhanced Search
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search E-codes or names") },
                        placeholder = { Text("e.g., E100, Curcumin, Colors...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (query.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${filtered.size} results found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Category Section
            Text(
                "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) },
                        leadingIcon = {
                            Icon(
                                getCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (query.isEmpty()) "No additives found" else "No results for \"$query\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Try adjusting your search terms or filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filtered) { a ->
                        val title = a.nameFi?.ifBlank { a.nameEn ?: a.code } ?: (a.nameEn ?: a.code)
                        val cat = a.category ?: eCodeCategory(a.code)
                        EnhancedEItemCard(
                            code = a.code,
                            title = title,
                            category = cat,
                            onOpen = {
                                val url = "https://world.openfoodfacts.org/additive/${a.code.lowercase(Locale.ROOT)}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedEItemCard(
    code: String,
    title: String,
    category: String,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // E-code badge
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    code,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        getCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onOpen,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Details")
            }
        }
    }
}

// Helper function to get category icons
private fun getCategoryIcon(category: String) = when {
    category.contains("Colour", ignoreCase = true) || category.contains("Värit", ignoreCase = true) -> Icons.Default.Palette
    category.contains("Preservative", ignoreCase = true) || category.contains("Säilöntä", ignoreCase = true) -> Icons.Default.Shield
    category.contains("Antioxidant", ignoreCase = true) || category.contains("Hapettu", ignoreCase = true) -> Icons.Default.HealthAndSafety
    category.contains("Stabilizer", ignoreCase = true) || category.contains("Paksunn", ignoreCase = true) -> Icons.Default.Balance
    category.contains("Acidity", ignoreCase = true) || category.contains("Happamuuden", ignoreCase = true) -> Icons.Default.Science
    category.contains("Enhancer", ignoreCase = true) || category.contains("Makuvoim", ignoreCase = true) -> Icons.Default.Restaurant
    category.contains("Antibiotic", ignoreCase = true) -> Icons.Default.Medication
    category.contains("Sweet", ignoreCase = true) || category.contains("Makeut", ignoreCase = true) -> Icons.Default.Cake
    else -> Icons.Default.Category
}

/** Fallback category by numeric E-range. */
private fun eCodeCategory(code: String): String {
    val n = code.drop(1).toIntOrNull() ?: return "Additive"
    return when (n) {
        in 100..199 -> "Värit / Colours"
        in 200..299 -> "Säilöntäaineet / Preservatives"
        in 300..399 -> "Hapettumisenesto / Antioxidants"
        in 400..499 -> "Paksunn./Stabilizers"
        in 500..599 -> "Happamuuden säätö / Acidity"
        in 600..699 -> "Makuvoim./Enhancers"
        in 700..799 -> "Antibiotics (historic)"
        in 900..999 -> "Makeut./Glazing/Propellants"
        else -> "Other"
    }
}