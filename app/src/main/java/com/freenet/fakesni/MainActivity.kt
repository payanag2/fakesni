package com.freenet.fakesni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freenet.fakesni.ui.theme.Background
import com.freenet.fakesni.ui.theme.BorderColor
import com.freenet.fakesni.ui.theme.CardBackground
import com.freenet.fakesni.ui.theme.DividerColor
import com.freenet.fakesni.ui.theme.Error
import com.freenet.fakesni.ui.theme.FakeSNITheme
import com.freenet.fakesni.ui.theme.FocusBlue
import com.freenet.fakesni.ui.theme.IconTeal
import com.freenet.fakesni.ui.theme.LogInfoColor
import com.freenet.fakesni.ui.theme.OnBackground
import com.freenet.fakesni.ui.theme.OnSurface
import com.freenet.fakesni.ui.theme.OnSurfaceDim
import com.freenet.fakesni.ui.theme.PrimaryDim
import com.freenet.fakesni.ui.theme.Success
import com.freenet.fakesni.ui.theme.Surface
import com.freenet.fakesni.ui.theme.SurfaceVariant
import com.freenet.fakesni.ui.theme.TimestampColor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FakeSNITheme { MainScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val config   by vm.config.collectAsState()
    val running  by vm.isRunning.collectAsState()
    val logLines by vm.logLines.collectAsState()
    var advancedExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FAKE SNI", color = OnBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        // Text("SNI Spoofing Proxy", color = OnSurfaceDim, fontSize = 14.sp, letterSpacing = 0.3.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatusCard(running = running, config = config)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StartStopButton(running = running, onToggle = { if (running) vm.stop() else vm.start() }, modifier = Modifier.weight(1f))
                TestButton(enabled = !running, onClick = { vm.test() })
            }

            SectionDivider("CONFIGURATION")

            ConfigCard(title = "Proxy", icon = Icons.Default.SwapHoriz) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField("Listen IP", config.listenHost, { vm.update(config.copy(listenHost = it)) }, !running, modifier = Modifier.weight(2f))
                    AppField("Port", config.listenPort, { vm.update(config.copy(listenPort = it)) }, !running, KeyboardType.Number, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField("Connect IP", config.connectHost, { vm.update(config.copy(connectHost = it)) }, !running, modifier = Modifier.weight(2f))
                    AppField("Port", config.connectPort, { vm.update(config.copy(connectPort = it)) }, !running, KeyboardType.Number, Modifier.weight(1f))
                }
            }

            ConfigCard(title = "SNI Spoofing", icon = Icons.Default.Security) {
                AppField("Fake SNI hostname", config.fakeSni, { vm.update(config.copy(fakeSni = it)) }, !running)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionDropdown(
                        label = "uTLS fingerprint",
                        selected = config.utls,
                        options = SniConfig.UTLS_OPTIONS,
                        onSelected = { vm.update(config.copy(utls = it)) },
                        enabled = !running,
                        modifier = Modifier.weight(1f)
                    )
                    OptionDropdown(
                        label = "Injector",
                        selected = config.injector,
                        options = SniConfig.INJECTOR_OPTIONS,
                        onSelected = { vm.update(config.copy(injector = it)) },
                        enabled = !running,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AdvancedSectionHeader(expanded = advancedExpanded, onClick = { advancedExpanded = !advancedExpanded })

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConfigCard(title = "Timing", icon = Icons.Default.Timer) {
                        StepperRow("Fake repeat", config.fakeRepeat, { vm.update(config.copy(fakeRepeat = it)) }, 1, 20, !running)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppField("Fake delay", config.fakeDelay, { vm.update(config.copy(fakeDelay = it)) }, !running, modifier = Modifier.weight(1f))
                            AppField("ACK timeout", config.ackTimeout, { vm.update(config.copy(ackTimeout = it)) }, !running, modifier = Modifier.weight(1f))
                        }
                    }

                    ConfigCard(title = "Fragmentation", icon = Icons.AutoMirrored.Filled.CallSplit) {
                        SwitchRow("Enable fragmentation", config.enableFragment, { vm.update(config.copy(enableFragment = it)) }, !running)
                        AnimatedVisibility(config.enableFragment, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AppField("Fragment delay", config.fragmentDelay, { vm.update(config.copy(fragmentDelay = it)) }, !running, modifier = Modifier.weight(1f))
                                    StepperCompact("SNI chunk", config.sniChunk, { vm.update(config.copy(sniChunk = it)) }, 1, 100, !running, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    ConfigCard(title = "Routing", icon = Icons.Default.Router) {
                        SwitchRow("Add IP rule for uid 0", config.addIpRule, { vm.update(config.copy(addIpRule = it)) }, !running)
                        AnimatedVisibility(config.addIpRule, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                AppField("Network interface", config.networkInterface, { vm.update(config.copy(networkInterface = it)) }, !running)
                            }
                        }
                    }
                }
            }

            SectionDivider("LOG OUTPUT")

            LogCard(lines = logLines)

            AppFooter()
        }
    }
}

// ─── Status Card ────────────────────────────────────────────────────────────

@Composable
fun StatusCard(running: Boolean, config: SniConfig) {
    val sineEasing = CubicBezierEasing(0.45f, 0.05f, 0.55f, 0.95f)
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(2200, easing = sineEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ringAlpha by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0.45f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, if (running) Success.copy(alpha = 0.35f) else BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dot with pulsing ring when running
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                if (running) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = ringAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (running) Success.copy(alpha = pulse) else OnSurfaceDim)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (running) "RUNNING" else "STOPPED",
                    color = if (running) Success else OnSurfaceDim,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (running) "${config.listenHost}:${config.listenPort}  →  ${config.fakeSni}"
                    else "Tap Start Proxy to begin",
                    color = if (running) OnSurface else OnSurfaceDim,
                    fontSize = 12.sp
                )
            }
            if (running) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Success.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Success.copy(alpha = 0.3f))
                ) {
                    Text(
                        config.utls.uppercase(),
                        color = Success,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─── Start / Stop Button ────────────────────────────────────────────────────

@Composable
fun StartStopButton(running: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val borderColor = if (running) Error.copy(alpha = 0.6f) else BorderColor
    val textColor   = if (running) Error else OnBackground
    val bgColor     = if (running) Error.copy(alpha = 0.08f) else PrimaryDim

    OutlinedButton(
        onClick = onToggle,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bgColor, contentColor = textColor)
    ) {
        Icon(
            if (running) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = textColor
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (running) "Stop Spoofing" else "Start Spoofing",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun TestButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) BorderColor else DividerColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Surface,
            contentColor = if (enabled) OnSurface else OnSurfaceDim
        )
    ) {
        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Test", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Section Divider ────────────────────────────────────────────────────────

@Composable
fun SectionDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor, thickness = 1.dp)
        Text(
            "  $label  ",
            color = OnSurfaceDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor, thickness = 1.dp)
    }
}

// ─── Advanced Section Header ─────────────────────────────────────────────────

@Composable
fun AdvancedSectionHeader(expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor, thickness = 1.dp)
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "ADVANCED",
                color = OnSurfaceDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceDim,
                modifier = Modifier.size(14.dp)
            )
        }
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor, thickness = 1.dp)
    }
}

// ─── Config Card ────────────────────────────────────────────────────────────

@Composable
fun ConfigCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        // Teal left accent stripe
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(IconTeal.copy(alpha = 0.7f), IconTeal.copy(alpha = 0.1f)))
                    )
            )
            Column(modifier = Modifier.padding(start = 13.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = IconTeal, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(title, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.4.sp)
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

// ─── Fields ─────────────────────────────────────────────────────────────────

@Composable
fun AppField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value, onValueChange = onValue,
        label = { Text(label, fontSize = 11.sp) },
        enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FocusBlue,
            unfocusedBorderColor = BorderColor,
            disabledBorderColor = DividerColor,
            focusedLabelColor = FocusBlue,
            unfocusedLabelColor = OnSurface,
            disabledLabelColor = OnSurfaceDim,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            disabledTextColor = OnSurfaceDim,
            cursorColor = FocusBlue,
            focusedContainerColor = SurfaceVariant,
            unfocusedContainerColor = Surface,
            disabledContainerColor = Surface,
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusBlue, unfocusedBorderColor = BorderColor, disabledBorderColor = DividerColor,
                focusedLabelColor = FocusBlue, unfocusedLabelColor = OnSurface, disabledLabelColor = OnSurfaceDim,
                focusedTextColor = OnBackground, unfocusedTextColor = OnBackground, disabledTextColor = OnSurfaceDim,
                focusedContainerColor = SurfaceVariant, unfocusedContainerColor = Surface, disabledContainerColor = Surface,
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = SurfaceVariant) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = if (option == selected) IconTeal else OnBackground, fontSize = 13.sp) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

// ─── Switches & Steppers ────────────────────────────────────────────────────

@Composable
fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) OnBackground else OnSurfaceDim, fontSize = 13.sp)
        Switch(
            checked = checked, onCheckedChange = onChecked, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = IconTeal,
                checkedBorderColor = IconTeal,
                uncheckedThumbColor = OnSurfaceDim,
                uncheckedTrackColor = Surface,
                uncheckedBorderColor = BorderColor
            )
        )
    }
}

@Composable
fun StepperRow(label: String, value: Int, onValue: (Int) -> Unit, min: Int, max: Int, enabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = if (enabled) OnBackground else OnSurfaceDim, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepBtn("−", enabled && value > min) { onValue(value - 1) }
            Text("$value", color = if (enabled) OnBackground else OnSurfaceDim, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center)
            StepBtn("+", enabled && value < max) { onValue(value + 1) }
        }
    }
}

@Composable
fun StepperCompact(label: String, value: Int, onValue: (Int) -> Unit, min: Int, max: Int, enabled: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = Surface, border = BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, color = OnSurfaceDim, fontSize = 10.sp, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepBtn("−", enabled && value > min) { onValue(value - 1) }
                Text("$value", color = if (enabled) OnBackground else OnSurfaceDim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                StepBtn("+", enabled && value < max) { onValue(value + 1) }
            }
        }
    }
}

@Composable
fun StepBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (enabled) SurfaceVariant else DividerColor)
            .border(BorderStroke(1.dp, if (enabled) BorderColor else DividerColor), CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) OnBackground else OnSurfaceDim, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
    }
}

// ─── Footer ─────────────────────────────────────────────────────────────────

@Composable
fun AppFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "v0.6.0",
            color = OnSurfaceDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        Text(
            "by OH",
            color = OnSurfaceDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        val thanksText = buildAnnotatedString {
            withStyle(SpanStyle(color = OnSurfaceDim, fontSize = 13.sp)) { append("Thanks to ") }
            withLink(LinkAnnotation.Url("https://github.com/aleskxyz")) {
                withStyle(SpanStyle(color = IconTeal, fontSize = 13.sp, textDecoration = TextDecoration.Underline)) {
                    append("@aleskxyz")
                }
            }
            withStyle(SpanStyle(color = OnSurfaceDim, fontSize = 13.sp)) { append("  &  ") }
            withLink(LinkAnnotation.Url("https://github.com/patterniha")) {
                withStyle(SpanStyle(color = IconTeal, fontSize = 13.sp, textDecoration = TextDecoration.Underline)) {
                    append("@patterniha")
                }
            }
        }

        Text(text = thanksText)
    }
}

// ─── Log Card ───────────────────────────────────────────────────────────────

@Composable
fun LogCard(lines: List<String>) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) scope.launch { listState.animateScrollToItem(lines.size - 1) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF080808),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(IconTeal))
                    Spacer(Modifier.width(8.dp))
                    Text("LOG", color = OnSurfaceDim, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp)
                }
                Text("${lines.size} lines", color = OnSurfaceDim, fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            if (lines.isEmpty()) {
                Text("— waiting for output —", color = OnSurfaceDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                LazyColumn(state = listState, modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(lines) { line ->
                        // Split timestamp prefix from message
                        val parts = line.split("  ", limit = 2)
                        if (parts.size == 2) {
                            Row {
                                Text(parts[0], color = TimestampColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.alignByBaseline())
                                Text("  ", fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.alignByBaseline())
                                Text(
                                    parts[1],
                                    color = when {
                                        parts[1].startsWith("[!]") -> Error
                                        parts[1].startsWith("[err]") -> Error.copy(alpha = 0.75f)
                                        parts[1].startsWith("[*]") -> LogInfoColor
                                        else -> OnSurface
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.alignByBaseline().weight(1f)
                                )
                            }
                        } else {
                            Text(line, color = OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
