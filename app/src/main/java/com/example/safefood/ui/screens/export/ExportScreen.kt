package com.example.safefood.ui.screens.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safefood.ui.SafeFoodViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(viewModel: SafeFoodViewModel) {
    val context = LocalContext.current
    var selectedExportType by remember { mutableIntStateOf(0) } // 0 = Foods, 1 = Shopping List

    val foods by viewModel.foods.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val csvText = remember(selectedExportType, foods, categories, shoppingList) {
        if (selectedExportType == 0) viewModel.getFoodsCsvText() else viewModel.getShoppingCsvText()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) when (selectedExportType) {
            0 -> viewModel.importFoodsCsv(uri)
            1 -> viewModel.importShoppingListCsv(uri)
        }
    }

    /**
     * Generates and offers a download of a template CSV for the selected type.
     * The food template includes all columns expected by CsvImporter.
     * The shopping list template includes ID, name, category, status, and source meal.
     */
    fun downloadTemplate(type: Int) {
        when (type) {
            0 -> {
                val template = "ID,Food Name,Category,Category ID,Rating (0-5),Last Rated Timestamp,Last Rated Date,Notes,Nutrients,Season,Image URL,Is Potential Meal,Is Blacklisted,Previous Category ID\n" +
                    "food_1,Whole Milk,Dairy,cat_dairy,5,2026-01-15 14:30:00,2026-01-15,creamy,Proteins;Healthy Fats,All Year,file:///android_asset/food_images/milk.jpg,true,false,"
                val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "safefood_foods_template_$stamp.csv"
                viewModel.downloadCsvFile(template, fileName, context)
            }
            1 -> {
                val template = "ID,Item Name,Category,Status,Source Meal\n" +
                    "shop_1,White Rice,General,NEEDED,Wednesday\n" +
                    "shop_2,Applesauce,General,IN_CART,Wednesday"
                val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "safefood_shopping_list_template_$stamp.csv"
                viewModel.downloadCsvFile(template, fileName, context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "CSV Export",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CSV Export",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Text(
                    text = "Export your safe food list, ratings, dates, or shopping list to CSV for record keeping, sharing with dietitians, or offline backups.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Select Data Type
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { selectedExportType = 0 },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Food Ratings")
                    }

                    Button(
                        onClick = { selectedExportType = 1 },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shopping List")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import CSV", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    text = "Restore an export or move your list from another device. Foods are matched by ID or name, so importing the same file twice updates ratings instead of creating duplicates. New groups and foods are added automatically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose a SafeFood CSV file")
                }
            }
        }

        OutlinedButton(
                onClick = { downloadTemplate(0) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Food Template", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { downloadTemplate(1) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shopping List Template", fontSize = 12.sp)
            }
        }

        // Preview Box
        Text("CSV Output Preview:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = csvText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }

        // Share & Copy Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val title = if (selectedExportType == 0) "SafeFood Ratings CSV" else "SafeFood Shopping List CSV"
                    val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
                    val baseName = if (selectedExportType == 0) "safefood_ratings" else "safefood_shopping_list"
                    viewModel.shareCsvData(context, csvText, title, "${baseName}_$stamp.csv")
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share CSV", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("CSV Export", csvText)
                    clipboard.setPrimaryClip(clip)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Text")
            }
        }
    }
