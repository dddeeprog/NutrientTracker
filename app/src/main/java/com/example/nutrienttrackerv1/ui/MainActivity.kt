@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nutrienttrackerv1.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nutrienttrackerv1.data.*
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.material.icons.filled.SmartToy
import coil.compose.AsyncImage
import com.example.nutrienttrackerv1.ai.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("🥗 Nutrient Tracker (Android)") }) },
        bottomBar = {
            NavigationBar {
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
        Box(Modifier.padding(inner)) {
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

// ============== Screens ==============
@Composable
fun TodayScreen(vm: MainViewModel, day: DaySummary?) {
    val scroll = rememberScrollState()
    val today = vm.today.collectAsState().value

    Column(Modifier.padding(16.dp).verticalScroll(scroll)) {
        Text("日期：$today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        val state = day
        if (state == null) {
            Text("加载中…")
            return
        }

        Text("进度（相对每日目标）", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        NUTRIENTS_META.forEach { nm ->
            val value = state.totals[nm.key] ?: 0.0
            val target = when (nm.key) {
                "calories_kcal" -> state.goal.calories_kcal
                "protein_g" -> state.goal.protein_g
                "carbs_g" -> state.goal.carbs_g
                "fat_g" -> state.goal.fat_g
                "fiber_g" -> state.goal.fiber_g
                "sugar_g" -> state.goal.sugar_g
                "sodium_mg" -> state.goal.sodium_mg
                "calcium_mg" -> state.goal.calcium_mg
                "iron_mg" -> state.goal.iron_mg
                "vitamin_c_mg" -> state.goal.vitamin_c_mg
                else -> 0.0
            } ?: 0.0
            val ratio = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

            Text("${nm.label}: ${"%.1f".format(value)} / ${"%.1f".format(target)}")
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("记录明细（食物库映射）", fontWeight = FontWeight.Bold)
        state.foods.forEach {
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${it.food.name} · ${"%.0f".format(it.entry.amount_g)} g")
                        val kcal = (it.entry.amount_g / 100.0) * it.food.calories_kcal_per_100g
                        Text("≈ ${"%.0f".format(kcal)} kcal")
                    }
                    val scope = rememberCoroutineScope()
                    Button(onClick = { scope.launch { vm.deleteEntry(it.entry.id) } }) { Text("删除") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("记录明细（AI营养直加）", fontWeight = FontWeight.Bold)
        state.customs.forEach {
            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${it.label} · ≈ ${"%.0f".format(it.calories_kcal)} kcal")
                        Text("蛋白 ${it.protein_g} g | 碳水 ${it.carbs_g} g | 脂肪 ${it.fat_g} g")
                        if (!it.notes.isNullOrBlank()) Text(it.notes!!)
                    }
                    val scope = rememberCoroutineScope()
                    Button(onClick = { scope.launch { vm.deleteCustom(it.id) } }) { Text("删除") }
                }
            }
        }
    }
}

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
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selections[idx], onCheckedChange = { selections[idx] = it })
                            Text("${it.name} · 估算 ${"%.0f".format(it.estimated_weight_g ?: 0.0)} g · 置信 ${"%.2f".format(it.confidence ?: 0.0)}")
                        }
                        Text("≈ ${"%.0f".format(it.calories_kcal ?: 0.0)} kcal | P ${it.protein_g} g | C ${it.carbs_g} g | F ${it.fat_g} g")
                        it.notes?.takeIf { s -> s.isNotBlank() }?.let { n -> Text(n) }
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

