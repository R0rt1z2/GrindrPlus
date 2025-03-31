package com.grindrplus.manager.ui

    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp

    @Composable
    fun CalculatorScreen() {
        var display by remember { mutableStateOf("0") }
        var operation by remember { mutableStateOf<String?>(null) }
        var firstNumber by remember { mutableStateOf<Double?>(null) }
        var newNumber by remember { mutableStateOf(true) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = display,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Buttons grid in fixed layout
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
                            CalculatorButton(number, ButtonType.NUMBER, modifier = Modifier.weight(1f)) {
                                updateDisplay(number, display, newNumber) { display = it; newNumber = false }
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
                            CalculatorButton(number, ButtonType.NUMBER, modifier = Modifier.weight(1f)) {
                                updateDisplay(number, display, newNumber) { display = it; newNumber = false }
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
                            CalculatorButton(number, ButtonType.NUMBER, modifier = Modifier.weight(1f)) {
                                updateDisplay(number, display, newNumber) { display = it; newNumber = false }
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
                            if (operation != null && firstNumber != null) {
                                val secondNumber = display.toDoubleOrNull() ?: 0.0
                                val result = when (operation) {
                                    "+" -> firstNumber!! + secondNumber
                                    "-" -> firstNumber!! - secondNumber
                                    "×" -> firstNumber!! * secondNumber
                                    "÷" -> if (secondNumber != 0.0) firstNumber!! / secondNumber else Double.POSITIVE_INFINITY
                                    else -> secondNumber
                                }
                                display = if (result % 1 == 0.0) result.toInt().toString() else result.toString()
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

    enum class ButtonType { NUMBER, OPERATION, FUNCTION, EQUALS }

    @Composable
    private fun CalculatorButton(
        text: String,
        type: ButtonType,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
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
            modifier = modifier
                .height(60.dp)
                .fillMaxWidth(),
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