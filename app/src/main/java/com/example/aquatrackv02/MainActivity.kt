package com.example.aquatrackv02

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import NotificationHelper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.aquatrackv02.ui.theme.Aquatrackv02Theme
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

// Modelo de datos para bebidas
data class Bebida(
    val id: UUID = UUID.randomUUID(),
    val tipo: String,
    val cantidad: Int, // en mililitros
    val fecha: Date = Date()
)

class MainActivity : ComponentActivity() {

    private val bebidas = mutableStateListOf<Bebida>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permissions = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 102)
            }
        }

        NotificationHelper.showNotification(this, "Test", "Notificaciones activadas", 1)

        enableEdgeToEdge()
        setContent {
            Aquatrackv02Theme {
                val bebidas = remember { mutableStateListOf<Bebida>() }
                val context = LocalContext.current

                // Cargar datos guardados
                LaunchedEffect(Unit) {
                    val sharedPreferences = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
                    val bebidasJson = sharedPreferences.getString("bebidas_guardadas", null)

                    if (!bebidasJson.isNullOrEmpty()){
                        try {
                            val gson = Gson()
                            val type = object : TypeToken<List<Bebida>>() {}.type
                            val bebidasGuardadas = gson.fromJson<List<Bebida>>(bebidasJson, type)

                            //Limpiar lista actual y agregar elementos guardados
                            bebidas.clear()
                            bebidas.addAll(bebidasGuardadas)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error al cargar datos guardados", e)
                        }
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Registro de Bebidas")
                            },
                            actions = {
                                Button(
                                    onClick = {
                                        val gson = Gson()
                                        val bebidasJson = gson.toJson(bebidas)
                                        // Navegar a la actividad de gráficos
                                        val intent = Intent(this@MainActivity, GraficasActivity::class.java).apply {
                                            putExtra("bebidas_json", bebidasJson)
                                        }
                                        startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text("Estadísticas")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    PantallaRegistroBebidas(
                        bebidas = bebidas,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


 @OptIn(ExperimentalMaterial3Api::class) @Composable
fun PantallaRegistroBebidas(bebidas: SnapshotStateList<Bebida>, modifier: Modifier = Modifier) {
    // Estado para mantener la lista de bebidas
    val context = LocalContext.current
    val bebidas = remember { mutableStateListOf<Bebida>() }

    // Estados para el formulario
    var tipoBebidaSeleccionada by remember { mutableStateOf("Agua") }
    var cantidadMl by remember { mutableStateOf("250") }
    var mostrarDialogo by remember { mutableStateOf(false) }

    // Opciones de bebidas disponibles
    val tiposBebida = listOf("Agua", "Café", "Té", "Jugo", "Refresco", "Leche", "Otro")

    // Detectar tamaño de pantalla para adaptación responsiva
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    if (isTabletOrLandscape) {
        // Layout para tablets o modo horizontal con ConstraintLayout
        ConstraintLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (headerText, summarySection, listSection) = createRefs()
            val barrier = createEndBarrier(headerText, summarySection)

            // Título principal
            Text(
                text = "Registro de líquidos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(headerText) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
            )

            // Panel izquierdo: Resumen y botón
            Column(
                modifier = Modifier
                    .constrainAs(summarySection) {
                        top.linkTo(headerText.bottom, 16.dp)
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.35f)
                        height = Dimension.fillToConstraints
                    }
            ) {
                // Tarjeta con el resumen del día
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Hoy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        
                        val totalMl = bebidas.sumOf { it.cantidad }
                        Text(
                            text = "Total: $totalMl ml",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        LinearProgressIndicator(
                            progress = { (totalMl / 2500f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                        
                        Text(
                            text = "${(totalMl * 100 / 2500)}% de la meta diaria (2500 ml)",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Botón para agregar nueva bebida
                Button(
                    onClick = { mostrarDialogo = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                ) {
                    Icon(Icons.Default.Add, "Agregar bebida")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar bebida")
                }
            }

            // Panel derecho: Lista de bebidas
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(listSection) {
                        top.linkTo(headerText.bottom, 16.dp)
                        start.linkTo(barrier, 8.dp)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            ) {
                val (title, listContent) = createRefs()
                
                Text(
                    text = "Bebidas registradas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                )
                
                if (bebidas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .constrainAs(listContent) {
                                top.linkTo(title.bottom, 8.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom)
                                width = Dimension.fillToConstraints
                                height = Dimension.fillToConstraints
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay bebidas registradas hoy",
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .constrainAs(listContent) {
                                top.linkTo(title.bottom, 8.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom)
                                width = Dimension.fillToConstraints
                                height = Dimension.fillToConstraints
                            }
                    ) {
                        items(bebidas) { bebida ->
                            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val horaFormateada = formato.format(bebida.fecha)
                            
                            ListItem(
                                headlineContent = { Text(bebida.tipo) },
                                supportingContent = { Text("$horaFormateada - ${bebida.cantidad} ml") }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    } else {
        // Layout para móviles o modo vertical
        ConstraintLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (headerText, summaryCard, listTitle, listContainer, addButton) = createRefs()
            
            // Título principal
            Text(
                text = "Registro de líquidos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(headerText) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
            )
            
            // Tarjeta con el resumen del día
            Card(
                modifier = Modifier
                    .constrainAs(summaryCard) {
                        top.linkTo(headerText.bottom, 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hoy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    val totalMl = bebidas.sumOf { it.cantidad }
                    Text(
                        text = "Total: $totalMl ml",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LinearProgressIndicator(
                        progress = { (totalMl / 2500f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    
                    Text(
                        text = "${(totalMl * 100 / 2500)}% de la meta diaria (2500 ml)",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Título de la lista de bebidas
            Text(
                text = "Bebidas registradas",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.constrainAs(listTitle) {
                    top.linkTo(summaryCard.bottom, 16.dp)
                    start.linkTo(parent.start)
                }
            )
            
            // Lista de bebidas
            if (bebidas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .constrainAs(listContainer) {
                            top.linkTo(listTitle.bottom, 8.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(addButton.top, 16.dp)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay bebidas registradas hoy",
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .constrainAs(listContainer) {
                            top.linkTo(listTitle.bottom, 8.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(addButton.top, 16.dp)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        }
                ) {
                    items(bebidas) { bebida ->
                        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val horaFormateada = formato.format(bebida.fecha)
                        
                        ListItem(
                            headlineContent = { Text(bebida.tipo) },
                            supportingContent = { Text("$horaFormateada - ${bebida.cantidad} ml") }
                        )
                        HorizontalDivider()
                    }
                }
            }
            
            // Botón para agregar nueva bebida
            ExtendedFloatingActionButton(
                onClick = { mostrarDialogo = true },
                icon = { Icon(Icons.Default.Add, "Agregar bebida") },
                text = { Text("Agregar bebida") },
                modifier = Modifier.constrainAs(addButton) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
            )
        }
    }

    // Diálogo para agregar una nueva bebida
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Agregar bebida") },
            text = {
                Column {
                    // Selector de tipo de bebida
                    var expandedDropdown by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = it },
                    ) {
                        TextField(
                            value = tipoBebidaSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de bebida") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled= true)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            tiposBebida.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        tipoBebidaSeleccionada = opcion
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para cantidad en ml
                    TextField(
                        value = cantidadMl,
                        onValueChange = { cantidadMl = it },
                        label = { Text("Cantidad (ml)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {


                        // Validar datos y agregar a la lista
                        val cantidad = cantidadMl.toIntOrNull() ?: 0
                        if (cantidad > 0) {
                            bebidas.add(Bebida(tipo = tipoBebidaSeleccionada, cantidad = cantidad))
                            guardarBebidas(context, bebidas)
                            cantidadMl = "250"  // Reset valor predeterminado
                            mostrarDialogo = false

                            //Calcular el total
                            val totalMl = bebidas.sumOf { it.cantidad }

                            //Notificar  si alcanza o supera la meta
                            if (totalMl >= 2500){
                                NotificationHelper.showNotification(
                                    context=context,
                                    title = "¡Felicidades!",
                                    message = "Has llegado a tu meta diaria de 2500 ml",
                                    notificationId = 100
                                )
                            }
                        }
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaRegistroBebidasPreview() {
    val bebidasPreview = remember { mutableStateListOf<Bebida>() }
    Aquatrackv02Theme {
        PantallaRegistroBebidas(bebidas = bebidasPreview)
    }
}

private fun guardarBebidas(context: Context, bebidas: List<Bebida>){
    try {
        val sharedPreferences = context.getSharedPreferences("app_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val bebidasJson = gson.toJson(bebidas)
        editor.putString("bebidas_guardadas", bebidasJson)
        editor.apply()
    } catch (e: Exception) {
        Log.e("MainActivity", "Error al guardar datos", e)
    }
}
