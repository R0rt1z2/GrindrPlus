package com.grindrplus.manager.ui

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.grindrplus.core.Config

@Composable
fun CalculatorScreen(calculatorScreen: MutableState<Boolean>) {
    var display by remember { mutableStateOf("0") }
    var operation by remember { mutableStateOf<String?>(null) }
    var firstNumber by remember { mutableStateOf<Double?>(null) }
    var newNumber by remember { mutableStateOf(true) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val windowInsets = WindowInsets.systemBars
    val topInset = windowInsets.asPaddingValues().calculateTopPadding()
    val bottomInset = windowInsets.asPaddingValues().calculateBottomPadding()

    var selectedEasterEgg by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val easterEggs = mapOf(
        666 to "https://i.imgur.com/399VOm7.jpeg"
    )

    LaunchedEffect(Unit) {
        showPasswordDialog = Config.get("calculator_first_launch", true) as Boolean
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showEasterEgg) {
            EasterEggDialog(
                onDismiss = { showEasterEgg = false },
                videoUrl = "${easterEggs[selectedEasterEgg]}",
            )
        }

        if (showPasswordDialog) {
            CalculatorPasswordDialog {
                Config.put("calculator_first_launch", false)
                showPasswordDialog = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = topInset + 16.dp,
                    bottom = bottomInset + 16.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val formattedDisplay = remember(display) {
                    if (display.length > 12) {
                        display.chunked(12).joinToString("\n")
                    } else {
                        display
                    }
                }

                Text(
                    text = formattedDisplay,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 3,
                    textAlign = TextAlign.End,
                    lineHeight = 52.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton("C", ButtonType.FUNCTION, modifier = Modifier.weight(1f)) {
                        display = "0"
                        operation = null
                        firstNumber = null
                        newNumber = true
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                    CalculatorButton("÷", ButtonType.OPERATION, modifier = Modifier.weight(1f)) {
                        handleOperation("÷", display) { op, num ->
                            operation = op; firstNumber = num; newNumber = true
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (number in listOf("7", "8", "9")) {
                        CalculatorButton(
                            number,
                            ButtonType.NUMBER,
                            modifier = Modifier.weight(1f)
                        ) {
                            updateDisplay(number, display, newNumber) {
                                display = it; newNumber = false
                            }
                        }
                    }
                    CalculatorButton("×", ButtonType.OPERATION, modifier = Modifier.weight(1f)) {
                        handleOperation("×", display) { op, num ->
                            operation = op; firstNumber = num; newNumber = true
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (number in listOf("4", "5", "6")) {
                        CalculatorButton(
                            number,
                            ButtonType.NUMBER,
                            modifier = Modifier.weight(1f)
                        ) {
                            updateDisplay(number, display, newNumber) {
                                display = it; newNumber = false
                            }
                        }
                    }
                    CalculatorButton("-", ButtonType.OPERATION, modifier = Modifier.weight(1f)) {
                        handleOperation("-", display) { op, num ->
                            operation = op; firstNumber = num; newNumber = true
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (number in listOf("1", "2", "3")) {
                        CalculatorButton(
                            number,
                            ButtonType.NUMBER,
                            modifier = Modifier.weight(1f)
                        ) {
                            updateDisplay(number, display, newNumber) {
                                display = it; newNumber = false
                            }
                        }
                    }
                    CalculatorButton("+", ButtonType.OPERATION, modifier = Modifier.weight(1f)) {
                        handleOperation("+", display) { op, num ->
                            operation = op; firstNumber = num; newNumber = true
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculatorButton("0", ButtonType.NUMBER, modifier = Modifier.weight(2f)) {
                        updateDisplay("0", display, newNumber) { display = it; newNumber = false }
                    }
                    CalculatorButton(".", ButtonType.NUMBER, modifier = Modifier.weight(1f)) {
                        if (!display.contains(".") && !newNumber) {
                            display = "$display."
                        } else if (newNumber) {
                            display = "0."
                            newNumber = false
                        }
                    }
                    CalculatorButton("=", ButtonType.EQUALS, modifier = Modifier.weight(1f)) {
                        if (operation == "×" && firstNumber == 53.0) {
                            calculatorScreen.value = false
                            return@CalculatorButton
                        }

                        if (easterEggs.containsKey(firstNumber?.toInt())) {
                            selectedEasterEgg = firstNumber!!.toInt()
                            showEasterEgg = true
                            return@CalculatorButton
                        }

                        if (operation != null && firstNumber != null) {
                            val secondNumber = display.toDoubleOrNull() ?: 0.0
                            val result = when (operation) {
                                "+" -> firstNumber!! + secondNumber
                                "-" -> firstNumber!! - secondNumber
                                "×" -> firstNumber!! * secondNumber
                                "÷" -> if (secondNumber != 0.0) firstNumber!! / secondNumber else Double.POSITIVE_INFINITY
                                else -> secondNumber
                            }
                            display = if (result % 1 == 0.0) result.toInt()
                                .toString() else result.toString()
                            operation = null
                            firstNumber = null
                            newNumber = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EasterEggDialog(onDismiss: () -> Unit, videoUrl: String) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            val context = LocalContext.current
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = videoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun CalculatorPasswordDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Calculator Exit Code",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "To exit calculator mode, enter:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "53 × =",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "1. Press 5 then 3\n2. Press × (multiply)\n3. Press = (equals)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

enum class ButtonType { NUMBER, OPERATION, FUNCTION, EQUALS }

@Composable
private fun CalculatorButton(
    text: String,
    type: ButtonType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "buttonScale"
    )

    val buttonColor = when (type) {
        ButtonType.NUMBER -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ButtonType.OPERATION -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        ButtonType.FUNCTION -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        ButtonType.EQUALS -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = buttonColor,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .scale(scale.value),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun updateDisplay(
    input: String,
    currentDisplay: String,
    isNewNumber: Boolean,
    onUpdate: (String) -> Unit,
) {
    if (isNewNumber) {
        onUpdate(input)
    } else {
        if (currentDisplay == "0") {
            onUpdate(input)
        } else {
            onUpdate(currentDisplay + input)
        }
    }
}

private fun handleOperation(
    op: String,
    currentDisplay: String,
    onOperation: (String, Double) -> Unit,
) {
    val number = currentDisplay.toDoubleOrNull() ?: 0.0
    onOperation(op, number)
}