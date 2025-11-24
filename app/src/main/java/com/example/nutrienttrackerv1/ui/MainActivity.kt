@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nutrienttrackerv1.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.nutrienttrackerv1.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.nutrienttrackerv1.ai.AiParsed
import com.example.nutrienttrackerv1.ai.AiService
import com.example.nutrienttrackerv1.ai.buildChatRequest
import com.example.nutrienttrackerv1.ai.buildRetrofit
import com.example.nutrienttrackerv1.ai.bytesToDataUrl
import com.example.nutrienttrackerv1.ai.compressImage
import com.example.nutrienttrackerv1.ai.parseAiJson
import com.example.nutrienttrackerv1.data.CustomEntry
import com.example.nutrienttrackerv1.data.DaySummary
import com.example.nutrienttrackerv1.data.EntryWithFood
import com.example.nutrienttrackerv1.data.Food
import com.example.nutrienttrackerv1.data.NUTRIENTS_META
import com.example.nutrienttrackerv1.data.NutrientMeta
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



// ============== Activity ==============
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppScaffold(vm) }
    }
}

// ============== Scaffold / Tabs ==============
enum class Tab(val label: String, val icon: @Composable () -> Unit) {
    Today("今日", { Icon(Icons.Filled.Today, contentDescription = null) }),
    Add("记录", { Icon(Icons.Filled.Add, contentDescription = null) }),
    Foods("食物库", { Icon(Icons.Filled.List, contentDescription = null) }),
    Goals("目标", { Icon(Icons.Filled.Settings, contentDescription = null) }),
    Export("导出", { Icon(Icons.Filled.Download, contentDescription = null) }),
    Trend("趋势", { Icon(Icons.Filled.Assessment, contentDescription = null) }),

    AI("AI", { Icon(Icons.Filled.SmartToy, contentDescription = null) }),

}

@Composable
fun AppScaffold(vm: MainViewModel) {
    var tab by remember { mutableStateOf(Tab.Today) }

    val colorScheme = MaterialTheme.colorScheme
    val backgroundGradient = remember(colorScheme) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.primary.copy(alpha = 0.25f),
                colorScheme.surface,
                colorScheme.surfaceVariant
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = colorScheme.onBackground,
            bottomBar = {
                NavigationBar(tonalElevation = 0.dp) {
                    Tab.values().forEach {
                        NavigationBarItem(
                            selected = tab == it,
                            onClick = { tab = it },
                            icon = it.icon,
                            label = { Text(it.label) }
                        )
                    }
                }
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) {
                val day by vm.day.collectAsState()
                when (tab) {
                    Tab.Today -> TodayScreen(vm, day)
                    Tab.Add -> AddEntryScreen(vm)
                    Tab.Foods -> FoodScreen(vm)
                    Tab.Goals -> GoalsScreen(vm)
                    Tab.Export -> ExportScreen(vm)
                    Tab.Trend -> TrendScreen(vm)
                    Tab.AI -> AiScreen(vm)
                }
            }
        }
    }
}

// ============== Screens ==============
@Composable
fun TodayScreen(vm: MainViewModel, day: DaySummary?) {
    val listState = rememberLazyListState()
    val today by vm.today.collectAsState()
    val heroHeight = 260.dp
    val density = LocalDensity.current
    val heroHeightPx = remember(density) { with(density) { heroHeight.toPx() } }
    val parallaxOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                -listState.firstVisibleItemScrollOffset / 2f
            } else {
                -heroHeightPx / 2f
            }
        }
    }

    val reachedGoals = remember(day) {
        day?.let { summary ->
            listOf("calories_kcal", "protein_g", "carbs_g", "fat_g").all { key ->
                val target = summary.targetFor(key)
                target > 0 && (summary.totals[key] ?: 0.0) >= target
            }
        } ?: false
    }
    var showCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(reachedGoals) {
        if (reachedGoals) {
            showCelebration = true
            delay(3200)
            showCelebration = false
        } else {
            showCelebration = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TodayHeroCover(
                    today = today,
                    summary = day,
                    height = heroHeight,
                    parallaxOffset = parallaxOffset
                )
            }

            if (day == null) {
                items(3) { index ->
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (index == 0) 180.dp else 120.dp)
                    )
                }
            } else {
                item { KeyMetricsCard(summary = day) }

                if (day.foods.isNotEmpty()) {
                    item { SectionHeader(text = "记录明细（食物库映射）") }
                }
                items(day.foods, key = { it.entry.id }) { entry ->
                    FoodEntryCard(entry = entry, vm = vm)
                }

                if (day.customs.isNotEmpty()) {
                    item { SectionHeader(text = "记录明细（AI营养直加）") }
                }
                items(day.customs, key = { it.id }) { custom ->
                    CustomEntryCard(entry = custom, vm = vm)
                }
            }
        }

        if (showCelebration) {
            CelebrationOverlay()
        }
    }
}

private data class FocusMetric(val key: String, val label: String, val unit: String)

@Composable
private fun TodayHeroCover(
    today: String,
    summary: DaySummary?,
    height: Dp,
    parallaxOffset: Float
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { translationY = parallaxOffset }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.9f),
                            colors.primaryContainer.copy(alpha = 0.7f),
                            colors.tertiary.copy(alpha = 0.65f)
                        )
                    )
                )
                .border(1.dp, colors.onPrimary.copy(alpha = 0.12f), shape)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset.Zero,
                            radius = 900f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "今日营养概览",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimary.copy(alpha = 0.88f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = today,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
                summary?.let { data ->
                    val calories = data.totals["calories_kcal"] ?: 0.0
                    val goal = data.targetFor("calories_kcal")
                    val completion = if (goal > 0) (calories / goal * 100).coerceAtMost(999.0) else 0.0
                    Column {
                        Text(
                            text = "${"%.0f".format(calories)} kcal 已摄入",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (goal > 0) "目标 ${"%.0f".format(goal)} kcal" else "未设置目标",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroChip(
                                title = "完成度",
                                value = "${"%.0f".format(completion)}%",
                                modifier = Modifier.weight(1f)
                            )
                            HeroChip(
                                title = "记录项目",
                                value = "${data.foods.size + data.customs.size}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = "同步今日数据中…",
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(horizontal = 20.dp),
                            color = colors.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.heightIn(min = 64.dp),
        interactive = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun KeyMetricsCard(summary: DaySummary) {
    val focus = listOf(
        FocusMetric("calories_kcal", "卡路里", "kcal"),
        FocusMetric("protein_g", "蛋白质", "g"),
        FocusMetric("carbs_g", "碳水", "g"),
        FocusMetric("fat_g", "脂肪", "g")
    )
    val accent = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.inversePrimary
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), interactive = true) {
        Text(
            text = "关键营养进度",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        val rows = focus.chunked(2)
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { metric ->
                    val colorIndex = focus.indexOf(metric) % accent.size
                    NutrientRingCard(summary = summary, metric = metric, color = accent[colorIndex])
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (rowIndex != rows.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RowScope.NutrientRingCard(summary: DaySummary, metric: FocusMetric, color: Color) {
    val value = summary.totals[metric.key] ?: 0.0
    val target = summary.targetFor(metric.key)
    val progress = if (target > 0) (value / target).toFloat() else 0f

    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 170.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NutrientRing(progress = progress, color = color, modifier = Modifier.size(120.dp))
        Spacer(Modifier.height(12.dp))
        Text(metric.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${"%.0f".format(value)} ${metric.unit}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (target > 0) "目标 ${"%.0f".format(target)} ${metric.unit}" else "未设置目标",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NutrientRing(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtLeast(0f),
        animationSpec = tween(durationMillis = 850),
        label = "ringProgress"
    )

    Canvas(modifier = modifier) {
        val sweep = 270f
        val startAngle = 135f
        val strokeWidth = size.minDimension * 0.12f

        drawArc(
            color = color.copy(alpha = 0.15f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val clamped = animatedProgress.coerceAtMost(1f)
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep * clamped,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        if (animatedProgress > 1f) {
            val overflow = (animatedProgress - 1f).coerceAtMost(0.3f)
            drawArc(
                color = MaterialTheme.colorScheme.secondary,
                startAngle = startAngle,
                sweepAngle = sweep * overflow,
                useCenter = false,
                style = Stroke(width = strokeWidth * 0.6f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
    )
}

@Composable
private fun FoodEntryCard(entry: EntryWithFood, vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        interactive = false
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "份量 ${"%.0f".format(entry.entry.amount_g)} g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                val kcal = (entry.entry.amount_g / 100.0) * entry.food.calories_kcal_per_100g
                Text(
                    text = "≈ ${"%.0f".format(kcal)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(onClick = { scope.launch { vm.deleteEntry(entry.entry.id) } }) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun CustomEntryCard(entry: CustomEntry, vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        interactive = false
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "≈ ${"%.0f".format(entry.calories_kcal)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "P ${"%.1f".format(entry.protein_g)} g | C ${"%.1f".format(entry.carbs_g)} g | F ${"%.1f".format(entry.fat_g)} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = { scope.launch { vm.deleteCustom(entry.id) } }) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (interactive && pressed) 0.97f else 1f
    val restingElevation = if (interactive) 18.dp else 12.dp
    val targetElevation = if (interactive && pressed) 10.dp else restingElevation
    val scale by animateFloatAsState(targetScale, animationSpec = tween(durationMillis = 180), label = "cardScale")
    val elevation by animateDpAsState(targetElevation, animationSpec = tween(durationMillis = 180), label = "cardElevation")

    val glassModifier = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(
                    colors.surface.copy(alpha = 0.55f),
                    colors.surfaceVariant.copy(alpha = 0.35f)
                )
            )
        )
        .border(1.dp, colors.onSurface.copy(alpha = 0.08f), shape)

    val clickableModifier = if (interactive) {
        Modifier.clickable(interactionSource = interactionSource, indication = null) {}
    } else {
        Modifier
    }

    Column(
        modifier = glassModifier
            .then(clickableModifier)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
private fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(baseColor, highlight, baseColor),
                    start = Offset(translate - size.width, translate - size.height),
                    end = Offset(translate, translate)
                )
                when (val outline = shape.toOutline(size, layoutDirection)) {
                    is Outline.Rounded -> drawRoundRect(brush = brush, cornerRadius = outline.roundRect.cornerRadius)
                    else -> drawRect(brush = brush)
                }
            }
    )
}

@Composable
private fun CelebrationOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.05f))
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
        val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)
        composition?.let {
            LottieAnimation(
                composition = it,
                progress = { progress },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun DaySummary.targetFor(key: String): Double = when (key) {
    "calories_kcal" -> goal.calories_kcal
    "protein_g" -> goal.protein_g
    "carbs_g" -> goal.carbs_g
    "fat_g" -> goal.fat_g
    "fiber_g" -> goal.fiber_g
    "sugar_g" -> goal.sugar_g
    "sodium_mg" -> goal.sodium_mg
    "calcium_mg" -> goal.calcium_mg
    "iron_mg" -> goal.iron_mg
    "vitamin_c_mg" -> goal.vitamin_c_mg
    else -> 0.0
} ?: 0.0

@Composable
fun AddEntryScreen(vm: MainViewModel) {
    val foods by vm.foods.collectAsState()
    val scope = rememberCoroutineScope()

    Column(Modifier.padding(16.dp)) {
        Text("记录摄入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (foods.isEmpty()) {
            Text("当前没有食物，请先添加自定义食物。")
            return
        }

        var selected by remember { mutableStateOf(foods.first()) }
        var mode by remember { mutableStateOf(0) } // 0=克, 1=份
        var gramsText by remember { mutableStateOf("%.1f".format(selected.serving_size_g)) }
        var servingsText by remember { mutableStateOf("1.0") }
        var notes by remember { mutableStateOf("") }
        var showPicker by remember { mutableStateOf(false) }

        Button(onClick = { showPicker = true }) { Text("选择食物：${selected.name}") }
        if (showPicker) {
            AlertDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = { TextButton({ showPicker = false }) { Text("关闭") } },
                text = {
                    Column(Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState())) {
                        foods.forEach { f ->
                            TextButton(onClick = {
                                selected = f
                                gramsText = "%.1f".format(f.serving_size_g)
                                showPicker = false
                            }) { Text(f.name) }
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("按克(g)") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("按份") })
        }
        Spacer(Modifier.height(8.dp))

        if (mode == 0) {
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it },
                label = { Text("克数(g)") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = servingsText,
                onValueChange = { servingsText = it },
                label = { Text("份数") },
                modifier = Modifier.fillMaxWidth()
            )
            val amount = (servingsText.toDoubleOrNull() ?: 0.0) * selected.serving_size_g
            Text("换算克数：${"%.0f".format(amount)} g")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("备注(可选)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val amount = if (mode == 0) (gramsText.toDoubleOrNull() ?: 0.0)
            else (servingsText.toDoubleOrNull() ?: 0.0) * selected.serving_size_g
            if (amount > 0) scope.launch { vm.addEntry(amount, selected.id, notes.ifBlank { null }) }
        }) { Text("添加记录") }
    }
}

@Composable
fun FoodScreen(vm: MainViewModel) {
    val foods by vm.foods.collectAsState()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var serving by remember { mutableStateOf("100.0") }
    var cal by remember { mutableStateOf("0.0") }
    var pro by remember { mutableStateOf("0.0") }
    var carb by remember { mutableStateOf("0.0") }
    var fat by remember { mutableStateOf("0.0") }
    var fiber by remember { mutableStateOf("0.0") }
    var sugar by remember { mutableStateOf("0.0") }
    var na by remember { mutableStateOf("0.0") }
    var ca by remember { mutableStateOf("0.0") }
    var fe by remember { mutableStateOf("0.0") }
    var vc by remember { mutableStateOf("0.0") }

    Column(Modifier.padding(16.dp).verticalScroll(scroll)) {
        Text("添加自定义食物（每100g）", fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("食物名称 *") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        Row {
            OutlinedTextField(serving, { serving = it }, label = { Text("默认一份(g)") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(cal, { cal = it }, label = { Text("kcal/100g") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        Row {
            OutlinedTextField(pro, { pro = it }, label = { Text("蛋白 g/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(carb, { carb = it }, label = { Text("碳水 g/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(fat, { fat = it }, label = { Text("脂肪 g/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(fiber, { fiber = it }, label = { Text("纤维 g/100g") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(sugar, { sugar = it }, label = { Text("糖 g/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(na, { na = it }, label = { Text("钠 mg/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(ca, { ca = it }, label = { Text("钙 mg/100g") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(fe, { fe = it }, label = { Text("铁 mg/100g") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(vc, { vc = it }, label = { Text("维C mg/100g") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val f = Food(
                name = name.trim(),
                serving_size_g = serving.toDoubleOrNull() ?: 100.0,
                calories_kcal_per_100g = cal.toDoubleOrNull() ?: 0.0,
                protein_g_per_100g = pro.toDoubleOrNull() ?: 0.0,
                carbs_g_per_100g = carb.toDoubleOrNull() ?: 0.0,
                fat_g_per_100g = fat.toDoubleOrNull() ?: 0.0,
                fiber_g_per_100g = fiber.toDoubleOrNull() ?: 0.0,
                sugar_g_per_100g = sugar.toDoubleOrNull() ?: 0.0,
                sodium_mg_per_100g = na.toDoubleOrNull() ?: 0.0,
                calcium_mg_per_100g = ca.toDoubleOrNull() ?: 0.0,
                iron_mg_per_100g = fe.toDoubleOrNull() ?: 0.0,
                vitamin_c_mg_per_100g = vc.toDoubleOrNull() ?: 0.0
            )
            if (f.name.isNotBlank()) {
                scope.launch {
                    vm.addFood(f) {
                        name = ""; serving = "100.0"; cal = "0.0"; pro = "0.0"; carb = "0.0"; fat = "0.0"
                        fiber = "0.0"; sugar = "0.0"; na = "0.0"; ca = "0.0"; fe = "0.0"; vc = "0.0"
                    }
                }
            }
        }) { Text("添加到食物库") }

        Spacer(Modifier.height(16.dp))
        Text("当前食物库（${foods.size}）", fontWeight = FontWeight.Bold)
        foods.forEach { Text("• ${it.name}") }
    }
}

@Composable
fun GoalsScreen(vm: MainViewModel) {
    val g by vm.goalState.collectAsState()
    val scope = rememberCoroutineScope()
    var goal by remember { mutableStateOf(g) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("每日目标", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = "%.1f".format(goal.calories_kcal ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(calories_kcal = v) } },
            label = { Text("热量 kcal") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.protein_g ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(protein_g = v) } },
            label = { Text("蛋白质 g") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.carbs_g ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(carbs_g = v) } },
            label = { Text("碳水 g") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.fat_g ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(fat_g = v) } },
            label = { Text("脂肪 g") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.fiber_g ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(fiber_g = v) } },
            label = { Text("膳食纤维 g") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.sugar_g ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(sugar_g = v) } },
            label = { Text("糖 g") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.sodium_mg ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(sodium_mg = v) } },
            label = { Text("钠 mg") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.calcium_mg ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(calcium_mg = v) } },
            label = { Text("钙 mg") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.iron_mg ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(iron_mg = v) } },
            label = { Text("铁 mg") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%.1f".format(goal.vitamin_c_mg ?: 0.0),
            onValueChange = { it.toDoubleOrNull()?.let { v -> goal = goal.copy(vitamin_c_mg = v) } },
            label = { Text("维生素C mg") }, modifier = Modifier.fillMaxWidth()
        ); Spacer(Modifier.height(8.dp))

        Button(onClick = { scope.launch { vm.updateGoal(goal) } }) { Text("保存目标") }
    }
}

@Composable
fun ExportScreen(vm: MainViewModel) {
    val day by vm.day.collectAsState()
    val context = LocalContext.current

    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val createLauncher = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri: Uri? ->
        val csv = pendingCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) writeTextToUri(context, uri, csv)
        pendingCsv = null
    }

    Column(Modifier.padding(16.dp)) {
        Text("导出数据", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val d = day ?: return@Button
            val rows = buildList {
                // entries
                d.foods.forEach { ef ->
                    val factor = ef.entry.amount_g / 100.0
                    val map = mutableMapOf<String, Double>()
                    NUTRIENTS_META.forEach { nm ->
                        val per100 = when (nm.per100Column) {
                            "calories_kcal_per_100g" -> ef.food.calories_kcal_per_100g
                            "protein_g_per_100g" -> ef.food.protein_g_per_100g
                            "carbs_g_per_100g" -> ef.food.carbs_g_per_100g
                            "fat_g_per_100g" -> ef.food.fat_g_per_100g
                            "fiber_g_per_100g" -> ef.food.fiber_g_per_100g
                            "sugar_g_per_100g" -> ef.food.sugar_g_per_100g
                            "sodium_mg_per_100g" -> ef.food.sodium_mg_per_100g
                            "calcium_mg_per_100g" -> ef.food.calcium_mg_per_100g
                            "iron_mg_per_100g" -> ef.food.iron_mg_per_100g
                            "vitamin_c_mg_per_100g" -> ef.food.vitamin_c_mg_per_100g
                            else -> 0.0
                        }
                        map[nm.key] = factor * per100
                    }
                    add(listOf(
                        d.goal.id.toString(),
                        d.goal.calories_kcal?.toString() ?: "",
                        "food",
                        ef.food.name,
                        "%.1f".format(ef.entry.amount_g),
                        *(NUTRIENTS_META.map { "%.2f".format(map[it.key] ?: 0.0) }.toTypedArray()),
                        ef.entry.notes ?: ""
                    ))
                }
                // custom_entries
                d.customs.forEach { c ->
                    add(listOf(
                        d.goal.id.toString(),
                        d.goal.calories_kcal?.toString() ?: "",
                        "custom",
                        c.label,
                        "",
                        *(NUTRIENTS_META.map { key -> "%.2f".format(
                            when (key.key) {
                                "calories_kcal" -> c.calories_kcal
                                "protein_g" -> c.protein_g
                                "carbs_g" -> c.carbs_g
                                "fat_g" -> c.fat_g
                                "fiber_g" -> c.fiber_g
                                "sugar_g" -> c.sugar_g
                                "sodium_mg" -> c.sodium_mg
                                "calcium_mg" -> c.calcium_mg
                                "iron_mg" -> c.iron_mg
                                "vitamin_c_mg" -> c.vitamin_c_mg
                                else -> 0.0
                            }
                        ) }.toTypedArray()),
                        c.notes ?: ""
                    ))
                }
            }
            val header = listOf("goal_id", "kcal_goal", "type", "label", "amount_g", *NUTRIENTS_META.map { it.key }.toTypedArray(), "notes")
            val csv = buildString {
                appendLine(header.joinToString(","))
                rows.forEach { appendLine(it.joinToString(",")) }
            }
            pendingCsv = csv
            createLauncher.launch("nutrition_${vm.today.value}.csv")
        }) { Text("下载当日记录 CSV") }
    }
}

private fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { os ->
        OutputStreamWriter(os, Charsets.UTF_8).use { it.write(text) }
    }
}

@Composable
fun TrendScreen(vm: MainViewModel) {
    var data by remember { mutableStateOf<List<Pair<String, Map<String, Double>>>>(emptyList()) }
    LaunchedEffect(Unit) { data = vm.weekTrend() }

    Column(Modifier.padding(16.dp)) {
        Text("最近 7 天趋势（custom 口径）", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (data.isEmpty()) {
            Text("过去 7 天没有记录。")
        } else {
            Column {
                data.forEach { (d, sums) ->
                    Text("$d  · kcal=${"%.0f".format(sums["calories_kcal"] ?: 0.0)}  | P=${"%.0f".format(sums["protein_g"] ?: 0.0)} C=${"%.0f".format(sums["carbs_g"] ?: 0.0)} F=${"%.0f".format(sums["fat_g"] ?: 0.0)}")
                }
            }
        }
    }
}

@Composable
fun AiScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // 从 ViewModel 取数据库里的配置列表与当前选择
    val settings by vm.apiSettings.collectAsState()
    val active by vm.activeProvider.collectAsState()

    // 只保留三家的“模板默认值”，用于新建时快速填默认模型/Endpoint
    fun defaultModel(p: String) = when (p) {
        "Kimi (Moonshot)" -> "moonshot-v1-8k"
        "Qwen (DashScope)" -> "qwen2.5-vl"
        "Doubao (Ark)"    -> "doubao-vision-lite"
        else              -> "qwen2.5-vl"
    }
    fun defaultEndpoint(p: String) = when (p) {
        "Kimi (Moonshot)" -> "https://api.moonshot.cn/v1/chat/completions"
        "Qwen (DashScope)"-> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        "Doubao (Ark)"    -> "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
        else              -> "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    }

    // 当前编辑表单（来自选中的配置 or 新建）
    var provider by remember(active, settings) {
        mutableStateOf(active ?: settings.firstOrNull()?.provider ?: "Kimi (Moonshot)")
    }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(defaultModel(provider)) }
    var endpoint by remember { mutableStateOf(defaultEndpoint(provider)) }
    var extra by remember { mutableStateOf("如有可见餐具或手部，请用于估算比例。若为复合菜肴，请拆分主要成分。") }

    // 当 active/provider 或 settings 变化时，把表单同步为数据库里保存的值
    LaunchedEffect(provider, settings) {
        val s = settings.firstOrNull { it.provider == provider }
        if (s != null) {
            apiKey = s.api_key ?: ""
            model = s.model ?: defaultModel(provider)
            endpoint = s.endpoint ?: defaultEndpoint(provider)
        } else {
            // 新建：根据模板提供默认值
            apiKey = ""
            model = defaultModel(provider)
            endpoint = defaultEndpoint(provider)
        }
    }

    // 图片选择
    var picked by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> picked = uri }

    // 运行状态
    var loading by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf(AiParsed()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("📷 图片 AI 营养分析", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
        Text("选择/新建 API 配置")
        Spacer(Modifier.height(4.dp))

        // 下拉框：数据库已有的 provider + 三家模板（去重）
        val dbProviders = settings.map { it.provider }
        val templateProviders = listOf("Kimi (Moonshot)", "Qwen (DashScope)", "Doubao (Ark)")
        val allProviders = (dbProviders + templateProviders).distinct()

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = provider,
                onValueChange = {},
                readOnly = true,
                label = { Text("使用的 API 配置") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allProviders.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p) },
                        onClick = {
                            expanded = false
                            provider = p
                            vm.selectApiProvider(p)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("＋ 新建自定义配置…") },
                    onClick = {
                        expanded = false
                        provider = "自定义API"
                        vm.selectApiProvider(provider)
                        apiKey = ""
                        model = ""
                        endpoint = ""
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("名称（唯一）") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("模型") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("API Endpoint") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = extra, onValueChange = { extra = it }, label = { Text("额外说明（可选）") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                vm.saveApiSetting(provider.trim(), apiKey.trim(), model.trim(), endpoint.trim())
            }) { Text("💾 保存/更新此配置") }

            Spacer(Modifier.width(8.dp))
            val exists = settings.any { it.provider == provider }
            OutlinedButton(
                enabled = exists,
                onClick = { vm.deleteApiSetting(provider) }
            ) { Text("🗑 删除此配置") }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        Row {
            Button(onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (picked == null) "选择图片" else "重新选择图片") }

            Spacer(Modifier.width(12.dp))
            if (picked != null) {
                AsyncImage(model = picked, contentDescription = null, modifier = Modifier.size(80.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            enabled = picked != null && apiKey.isNotBlank() && model.isNotBlank() && endpoint.isNotBlank() && !loading,
            onClick = {
                loading = true; error = null; rawText = null; parsed = AiParsed()
                scope.launch {
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            compressImage(ctx, picked!!)   // 压缩在 IO 线程做
                        }
                        val dataUrl = bytesToDataUrl(bytes)
                        val req = buildChatRequest(model, dataUrl, extra)
                        val svc = buildRetrofit().create(AiService::class.java)
                        val resp = svc.chat(endpoint, "Bearer $apiKey", req)
                        val content = resp.choices.firstOrNull()?.message?.content.orEmpty()
                        rawText = content
                        parsed = parseAiJson(content)
                    } catch (e: java.net.SocketTimeoutException) {
                        error = "请求超时：请重试或换更小的图片/更快的网络。"
                    } catch (e: Exception) {
                        error = e.message ?: e.toString()
                    } finally {
                        loading = false
                    }
                }

            }
        ) { Text(if (loading) "分析中…" else "开始分析（使用上方选择的 API）") }


        if (loading) {
            Spacer(Modifier.height(16.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.height(12.dp))
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(80.dp))
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp)); Text("错误：$error", color = MaterialTheme.colorScheme.error)
        }

        if (rawText != null) {
            Spacer(Modifier.height(12.dp))
            Text("AI 原始 JSON（截断展示）")
            Text(rawText!!.take(1200))
        }

        if (parsed.items.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            parsed.serving_context?.takeIf { it.isNotBlank() }?.let { Text("上下文：$it"); Spacer(Modifier.height(4.dp)) }
            parsed.error_margin_pct?.let { Text("误差约：$it%"); Spacer(Modifier.height(4.dp)) }
            parsed.advice?.takeIf { it.isNotBlank() }?.let { Text("建议：$it"); Spacer(Modifier.height(8.dp)) }

            val selections = remember(parsed) { mutableStateListOf<Boolean>().apply { repeat(parsed.items.size) { add(true) } } }
            parsed.items.forEachIndexed { idx, it ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    interactive = false,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selections[idx], onCheckedChange = { selections[idx] = it })
                            Spacer(Modifier.width(8.dp))
                            Text("${it.name} · 估算 ${"%.0f".format(it.estimated_weight_g ?: 0.0)} g · 置信 ${"%.2f".format(it.confidence ?: 0.0)}")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "≈ ${"%.0f".format(it.calories_kcal ?: 0.0)} kcal | P ${it.protein_g} g | C ${it.carbs_g} g | F ${it.fat_g} g",
                            style = MaterialTheme.typography.bodySmall
                        )
                        it.notes?.takeIf { s -> s.isNotBlank() }?.let { n ->
                            Spacer(Modifier.height(4.dp))
                            Text(n, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    var count = 0
                    parsed.items.forEachIndexed { i, it ->
                        if (!selections[i]) return@forEachIndexed
                        vm.addCustomFromAI(
                            label = it.name,
                            calories = it.calories_kcal ?: 0.0,
                            protein = it.protein_g ?: 0.0,
                            carbs = it.carbs_g ?: 0.0,
                            fat = it.fat_g ?: 0.0,
                            fiber = it.fiber_g ?: 0.0,
                            sugar = it.sugar_g ?: 0.0,
                            sodium = it.sodium_mg ?: 0.0,
                            calcium = it.calcium_mg ?: 0.0,
                            iron = it.iron_mg ?: 0.0,
                            vitaminC = it.vitamin_c_mg ?: 0.0,
                            notes = "AI: ${it.notes ?: ""}",
                            source = "AI:${provider}"
                        )
                        count++
                    }
                }
            }) { Text("✅ 添加选中项（AI 直加）") }
        }
    }
}

