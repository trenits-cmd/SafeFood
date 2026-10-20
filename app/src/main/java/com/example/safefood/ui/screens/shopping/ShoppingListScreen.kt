package com.example.safefood.ui.screens.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.data.model.ShoppingItem
import com.example.safefood.data.model.ShoppingItemStatus
import com.example.safefood.ui.SafeFoodViewModel

@Composable
fun ShoppingListScreen(viewModel: SafeFoodViewModel) {
    val shoppingList by viewModel.shoppingList.collectAsState()

    var newItemName by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<ShoppingItemStatus?>(null) } // null = All

    val filteredList = remember(shoppingList, selectedFilter) {
        if (selectedFilter == null) shoppingList else shoppingList.filter { it.status == selectedFilter }
    }

    val neededCount = remember(shoppingList) { shoppingList.count { it.status == ShoppingItemStatus.NEEDED } }
    val inCartCount = remember(shoppingList) { shoppingList.count { it.status == ShoppingItemStatus.IN_CART } }
    val alreadyHaveCount = remember(shoppingList) { shoppingList.count { it.status == ShoppingItemStatus.ALREADY_HAVE } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Summary Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPill(
                label = "To Buy",
                count = neededCount,
                color = MaterialTheme.colorScheme.primary,
                isSelected = selectedFilter == ShoppingItemStatus.NEEDED,
                onClick = {
                    selectedFilter = if (selectedFilter == ShoppingItemStatus.NEEDED) null else ShoppingItemStatus.NEEDED
                },
                modifier = Modifier.weight(1f)
            )
            SummaryPill(
                label = "In Cart",
                count = inCartCount,
                color = Color(0xFF2E7D32),
                isSelected = selectedFilter == ShoppingItemStatus.IN_CART,
                onClick = {
                    selectedFilter = if (selectedFilter == ShoppingItemStatus.IN_CART) null else ShoppingItemStatus.IN_CART
                },
                modifier = Modifier.weight(1f)
            )
            SummaryPill(
                label = "Already Have",
                count = alreadyHaveCount,
                color = Color(0xFF1565C0),
                isSelected = selectedFilter == ShoppingItemStatus.ALREADY_HAVE,
                onClick = {
                    selectedFilter = if (selectedFilter == ShoppingItemStatus.ALREADY_HAVE) null else ShoppingItemStatus.ALREADY_HAVE
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Add New Item Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemName,
                onValueChange = { newItemName = it },
                placeholder = { Text("Add ingredient (e.g. Rice, Applesauce)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    viewModel.addShoppingItem(newItemName)
                    newItemName = ""
                },
                enabled = newItemName.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        // Action Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedFilter == null) "All Items (${filteredList.size})" else "${selectedFilter?.name} (${filteredList.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (inCartCount > 0 || alreadyHaveCount > 0) {
                OutlinedButton(
                    onClick = { viewModel.clearCompletedShoppingItems() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.RemoveShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Cart/Completed", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Your shopping list is empty!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onStatusChanged = { newStatus ->
                            viewModel.updateShoppingStatus(item.id, newStatus)
                        },
                        onDelete = {
                            viewModel.deleteShoppingItem(item.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryPill(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.08f),
        label = "pillBg"
    )

    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onStatusChanged: (ShoppingItemStatus) -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = when (item.status) {
        ShoppingItemStatus.NEEDED -> MaterialTheme.colorScheme.surface
        ShoppingItemStatus.IN_CART -> Color(0xFFE8F5E9)
        ShoppingItemStatus.ALREADY_HAVE -> Color(0xFFE3F2FD)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textDecoration = if (item.status == ShoppingItemStatus.IN_CART) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.status == ShoppingItemStatus.IN_CART) Color.Gray else MaterialTheme.colorScheme.onSurface
                )

                if (!item.sourceMeal.isNullOrBlank()) {
                    Text(
                        text = "From: ${item.sourceMeal}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status Switcher: "Cart" or "Already Have"
                StatusButton(
                    label = "In Cart",
                    isSelected = item.status == ShoppingItemStatus.IN_CART,
                    icon = Icons.Default.ShoppingCart,
                    activeColor = Color(0xFF2E7D32),
                    onClick = {
                        val nextStatus = if (item.status == ShoppingItemStatus.IN_CART) ShoppingItemStatus.NEEDED else ShoppingItemStatus.IN_CART
                        onStatusChanged(nextStatus)
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                StatusButton(
                    label = "Already Have",
                    isSelected = item.status == ShoppingItemStatus.ALREADY_HAVE,
                    icon = Icons.Default.Home,
                    activeColor = Color(0xFF1565C0),
                    onClick = {
                        val nextStatus = if (item.status == ShoppingItemStatus.ALREADY_HAVE) ShoppingItemStatus.NEEDED else ShoppingItemStatus.ALREADY_HAVE
                        onStatusChanged(nextStatus)
                    }
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusButton(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) activeColor else activeColor.copy(alpha = 0.1f)
    val contentColor = if (isSelected) Color.White else activeColor

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
