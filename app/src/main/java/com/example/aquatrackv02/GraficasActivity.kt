package com.example.aquatrackv02

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.aquatrackv02.ui.theme.Aquatrackv02Theme
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class GraficasActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Obtener datos de prueba (en una app real, estos deberían venir de una base de datos)
        val datosEjemplo = obtenerDatosEjemplo()
        
        setContent {
            Aquatrackv02Theme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Estadísticas de consumo") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver"
                                    )
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    PantallaGraficos(
                        bebidas = datosEjemplo,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    private fun obtenerDatosEjemplo(): List<Bebida> {
        val calendar = Calendar.getInstance()
        val hoy = calendar.time
        
        // Crear datos para los últimos 7 días
        val datos = mutableListOf<Bebida>()
        
        // Hoy
        datos.add(Bebida(tipo = "Agua", cantidad = 250, fecha = hoy))
        datos.add(Bebida(tipo = "Café", cantidad = 200, fecha = hoy))
        datos.add(Bebida(tipo = "Jugo", cantidad = 300, fecha = hoy))
        
        // Ayer
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 500, fecha = calendar.time))
        datos.add(Bebida(tipo = "Té", cantidad = 250, fecha = calendar.time))
        
        // Anteayer
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 750, fecha = calendar.time))
        datos.add(Bebida(tipo = "Refresco", cantidad = 330, fecha = calendar.time))
        
        // 3 días atrás
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 1000, fecha = calendar.time))
        
        // 4 días atrás
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 500, fecha = calendar.time))
        datos.add(Bebida(tipo = "Café", cantidad = 200, fecha = calendar.time))
        datos.add(Bebida(tipo = "Leche", cantidad = 250, fecha = calendar.time))
        
        // 5 días atrás
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 750, fecha = calendar.time))
        
        // 6 días atrás
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        datos.add(Bebida(tipo = "Agua", cantidad = 500, fecha = calendar.time))
        datos.add(Bebida(tipo = "Jugo", cantidad = 350, fecha = calendar.time))
        
        return datos
    }
}

@Composable
fun PantallaGraficos(bebidas: List<Bebida>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Para guardar el tipo de bebida seleccionado en el gráfico
    var seleccionActual by remember { mutableStateOf("") }
    
    // Obtener el ancho de pantalla para adaptación responsive
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600
    
    if (isTabletOrLandscape) {
        // Layout para tablets o modo horizontal usando ConstraintLayout
        ConstraintLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (pieChartSection, barChartSection) = createRefs()
            
            // Panel izquierdo - Gráfico circular
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(pieChartSection) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.percent(0.5f)
                        height = Dimension.fillToConstraints
                    }
                    .padding(end = 8.dp)
            ) {
                val (title, selectionCard, chart) = createRefs()
                
                Text(
                    text = "Consumo por tipo de bebida",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                )
                
                // Mostrar selección actual si hay algo seleccionado
                if (seleccionActual.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .constrainAs(selectionCard) {
                                top.linkTo(title.bottom, 8.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                width = Dimension.fillToConstraints
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = seleccionActual,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Gráfico circular
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            setUsePercentValues(true)
                            setExtraOffsets(20f, 10f, 20f, 10f)
                            
                            // Configuración visual
                            dragDecelerationFrictionCoef = 0.95f
                            isDrawHoleEnabled = true
                            setHoleColor(Color.WHITE)
                            setTransparentCircleColor(Color.WHITE)
                            setTransparentCircleAlpha(110)
                            holeRadius = 50f
                            transparentCircleRadius = 53f
                            setDrawCenterText(true)
                            centerText = "Bebidas"
                            setCenterTextSize(16f)
                            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                            
                            // Rotación y gestos
                            rotationAngle = 0f
                            isRotationEnabled = true
                            isHighlightPerTapEnabled = true
                            
                            // Leyenda
                            legend.apply {
                                isEnabled = true
                                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                                orientation = Legend.LegendOrientation.HORIZONTAL
                                setDrawInside(false)
                                textSize = 12f
                                form = Legend.LegendForm.CIRCLE
                                formSize = 12f
                                xEntrySpace = 10f
                                yEntrySpace = 0f
                            }
                            
                            // Evento de selección
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    e?.let {
                                        if (it is PieEntry) {
                                            val tipo = it.label
                                            val porcentaje = it.value
                                            // Mostrar información de la selección
                                            val total = bebidas.sumOf { bebida -> bebida.cantidad }
                                            val cantidadTipo = bebidas.filter { bebida -> bebida.tipo == tipo }
                                                                     .sumOf { bebida -> bebida.cantidad }
                                            seleccionActual = "Seleccionado: $tipo - $cantidadTipo ml (${String.format("%.1f", porcentaje)}%)"
                                            invalidate() // Refrescar gráfico
                                        }
                                    }
                                }
                                
                                override fun onNothingSelected() {
                                    seleccionActual = ""
                                    invalidate() // Refrescar gráfico
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .constrainAs(chart) {
                            top.linkTo(if (seleccionActual.isEmpty()) title.bottom else selectionCard.bottom, 16.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                    update = { chart ->
                        // Agrupar por tipo de bebida
                        val totalMl = bebidas.sumOf { it.cantidad }.toFloat()
                        val porTipo = bebidas.groupBy { it.tipo }
                            .mapValues { entry -> entry.value.sumOf { it.cantidad }.toFloat() }
                        
                        val entries = porTipo.map { (tipo, cantidad) ->
                            val porcentaje = (cantidad / totalMl) * 100f
                            PieEntry(porcentaje, tipo, MPPointF(0f, 0f))
                        }
                        
                        val coloresPersonalizados = intArrayOf(
                            Color.rgb(64, 89, 128), Color.rgb(149, 165, 124),
                            Color.rgb(217, 184, 162), Color.rgb(191, 134, 134),
                            Color.rgb(179, 48, 80), Color.rgb(217, 80, 138),
                            Color.rgb(254, 149, 7), Color.rgb(254, 247, 120)
                        )
                        
                        val dataSet = PieDataSet(entries, "").apply {
                            // Colores y estilos
                            colors = coloresPersonalizados.toList()
                            setDrawIcons(false)
                            sliceSpace = 3f
                            iconsOffset = MPPointF(0f, 40f)
                            selectionShift = 5f
                            
                            // Formato de valores
                            valueTextSize = 14f
                            valueTextColor = Color.WHITE
                            valueTypeface = Typeface.DEFAULT_BOLD
                            valueFormatter = PercentFormatter(chart)
                        }
                        
                        // Configurar data
                        val data = PieData(dataSet)
                        chart.data = data
                        
                        // Animación
                        chart.animateY(1400, Easing.EaseInOutQuad)
                        
                        // Refrescar
                        chart.invalidate()
                    }
                )
            }
            
            // Panel derecho - Gráfico de barras
            ConstraintLayout(
                modifier = Modifier
                    .constrainAs(barChartSection) {
                        top.linkTo(parent.top)
                        start.linkTo(pieChartSection.end, 8.dp)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        height = Dimension.fillToConstraints
                    }
            ) {
                val (title, chart) = createRefs()
                
                Text(
                    text = "Consumo diario (últimos 7 días)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                )
                
                // Gráfico de barras
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            
                            // Zoom y gestos
                            setPinchZoom(true)
                            setScaleEnabled(true)
                            setDrawBarShadow(false)
                            setDrawGridBackground(false)
                            
                            // Ejes
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                granularity = 1f
                                setDrawGridLines(false)
                                textSize = 12f
                                typeface = Typeface.DEFAULT_BOLD
                            }
                            
                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 0f
                                typeface = Typeface.DEFAULT
                                textSize = 12f
                                
                                // Formato de valores en ml
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "${value.toInt()} ml"
                                    }
                                }
                            }
                            
                            axisRight.isEnabled = false
                            
                            // Leyenda
                            legend.apply {
                                isEnabled = true
                                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                                orientation = Legend.LegendOrientation.VERTICAL
                                setDrawInside(false)
                                form = Legend.LegendForm.SQUARE
                                formSize = 12f
                                textSize = 12f
                                typeface = Typeface.DEFAULT_BOLD
                            }
                            
                            // Evento de selección
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    e?.let {
                                        if (it is BarEntry) {
                                            val index = it.x.toInt()
                                            val valor = it.y
                                            // La etiqueta se obtiene del eje X
                                            val etiqueta = xAxis.valueFormatter.getFormattedValue(it.x, xAxis)
                                            seleccionActual = "Día: $etiqueta - Consumo: ${valor.toInt()} ml"
                                            invalidate()
                                        }
                                    }
                                }
                                
                                override fun onNothingSelected() {
                                    seleccionActual = ""
                                    invalidate()
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .constrainAs(chart) {
                            top.linkTo(title.bottom, 16.dp)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                    update = { chart ->
                        // Ordenar por fecha y agrupar por día
                        val formato = SimpleDateFormat("dd/MM", Locale.getDefault())
                        val calendar = Calendar.getInstance()
                        
                        // Crear mapa con los últimos 7 días
                        val diasMap = mutableMapOf<String, Int>()
                        calendar.time = Date() // Reset to today
                        
                        // Llenar el mapa con los últimos 7 días para asegurar que todos aparezcan en el gráfico
                        for (i in 6 downTo 0) {
                            calendar.add(Calendar.DAY_OF_MONTH, -i)
                            diasMap[formato.format(calendar.time)] = 0
                            calendar.add(Calendar.DAY_OF_MONTH, i) // Volver al día actual
                        }
                        
                        // Agrupar bebidas por día
                        bebidas.forEach { bebida ->
                            val fechaFormateada = formato.format(bebida.fecha)
                            diasMap[fechaFormateada] = (diasMap[fechaFormateada] ?: 0) + bebida.cantidad
                        }
                        
                        // Convertir a entradas para el gráfico, ordenadas por fecha
                        val fechasOrdenadas = diasMap.keys.sortedBy { 
                            val parts = it.split("/")
                            parts[0].toInt() * 100 + parts[1].toInt() 
                        }
                        
                        val entries = fechasOrdenadas.mapIndexed { index, fecha ->
                            BarEntry(index.toFloat(), diasMap[fecha]?.toFloat() ?: 0f)
                        }
                        
                        // Configurar etiquetas en el eje X
                        chart.xAxis.valueFormatter = IndexAxisValueFormatter(fechasOrdenadas)
                        
                        // Configurar el dataset
                        val dataSet = BarDataSet(entries, "Consumo diario").apply {
                            // Colores degradados
                            val startColor = Color.rgb(100, 181, 246)
                            val endColor = Color.rgb(30, 136, 229)
                            
                            color = startColor
                            highLightColor = endColor
                            valueTextColor = Color.BLACK
                            valueTextSize = 10f
                            valueTypeface = Typeface.DEFAULT_BOLD
                            
                            // Formato de valores
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return if (value > 0) "${value.toInt()} ml" else ""
                                }
                            }
                        }
                        
                        // Configurar data
                        val data = BarData(dataSet).apply {
                            barWidth = 0.7f
                            // Al menos 20px entre barras
                            setValueTextSize(10f)
                        }
                        
                        chart.data = data
                        
                        // Configurar espaciado
                        chart.setFitBars(true)
                        
                        // Animación
                        chart.animateY(1400, Easing.EaseInOutQuad)
                        
                        // Refrescar
                        chart.invalidate()
                    }
                )
            }
        }
    } else {
        // Layout para móviles o pantalla vertical
        ConstraintLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val (selectionCard, pieTitle, pieChart, barTitle, barChart) = createRefs()
            
            // Mostrar selección actual si hay algo seleccionado
            if (seleccionActual.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .constrainAs(selectionCard) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = seleccionActual,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Título del gráfico circular
            Text(
                text = "Consumo por tipo de bebida",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.constrainAs(pieTitle) {
                    top.linkTo(if (seleccionActual.isEmpty()) parent.top else selectionCard.bottom, 16.dp)
                    start.linkTo(parent.start)
                }
            )
            
            // Gráfico circular
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        description.isEnabled = false
                        setUsePercentValues(true)
                        setExtraOffsets(20f, 10f, 20f, 10f)
                        
                        // Configuración visual
                        dragDecelerationFrictionCoef = 0.95f
                        isDrawHoleEnabled = true
                        setHoleColor(Color.WHITE)
                        setTransparentCircleColor(Color.WHITE)
                        setTransparentCircleAlpha(110)
                        holeRadius = 50f
                        transparentCircleRadius = 53f
                        setDrawCenterText(true)
                        centerText = "Bebidas"
                        setCenterTextSize(16f)
                        setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                        
                        // Rotación y gestos
                        rotationAngle = 0f
                        isRotationEnabled = true
                        isHighlightPerTapEnabled = true
                        
                        // Leyenda
                        legend.apply {
                            isEnabled = true
                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            setDrawInside(false)
                            textSize = 12f
                            form = Legend.LegendForm.CIRCLE
                            formSize = 12f
                            xEntrySpace = 10f
                            yEntrySpace = 0f
                        }
                        
                        // Evento de selección
                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                e?.let {
                                    if (it is PieEntry) {
                                        val tipo = it.label
                                        val porcentaje = it.value
                                        // Mostrar información de la selección
                                        val total = bebidas.sumOf { bebida -> bebida.cantidad }
                                        val cantidadTipo = bebidas.filter { bebida -> bebida.tipo == tipo }
                                                                 .sumOf { bebida -> bebida.cantidad }
                                        seleccionActual = "Seleccionado: $tipo - $cantidadTipo ml (${String.format("%.1f", porcentaje)}%)"
                                        invalidate() // Refrescar gráfico
                                    }
                                }
                            }
                            
                            override fun onNothingSelected() {
                                seleccionActual = ""
                                invalidate() // Refrescar gráfico
                            }
                        })
                    }
                },
                modifier = Modifier
                    .constrainAs(pieChart) {
                        top.linkTo(pieTitle.bottom, 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        height = Dimension.value(300.dp)
                    },
                update = { chart ->
                    // Agrupar por tipo de bebida
                    val totalMl = bebidas.sumOf { it.cantidad }.toFloat()
                    val porTipo = bebidas.groupBy { it.tipo }
                        .mapValues { entry -> entry.value.sumOf { it.cantidad }.toFloat() }
                    
                    val entries = porTipo.map { (tipo, cantidad) ->
                        val porcentaje = (cantidad / totalMl) * 100f
                        PieEntry(porcentaje, tipo, MPPointF(0f, 0f))
                    }
                    
                    val coloresPersonalizados = intArrayOf(
                        Color.rgb(64, 89, 128), Color.rgb(149, 165, 124),
                        Color.rgb(217, 184, 162), Color.rgb(191, 134, 134),
                        Color.rgb(179, 48, 80), Color.rgb(217, 80, 138),
                        Color.rgb(254, 149, 7), Color.rgb(254, 247, 120)
                    )
                    
                    val dataSet = PieDataSet(entries, "").apply {
                        // Colores y estilos
                        colors = coloresPersonalizados.toList()
                        setDrawIcons(false)
                        sliceSpace = 3f
                        iconsOffset = MPPointF(0f, 40f)
                        selectionShift = 5f
                        
                        // Formato de valores
                        valueTextSize = 14f
                        valueTextColor = Color.WHITE
                        valueTypeface = Typeface.DEFAULT_BOLD
                        valueFormatter = PercentFormatter(chart)
                    }
                    
                    // Configurar data
                    val data = PieData(dataSet)
                    chart.data = data
                    
                    // Animación
                    chart.animateY(1400, Easing.EaseInOutQuad)
                    
                    // Refrescar
                    chart.invalidate()
                }
            )
            
            // Título del gráfico de barras
            Text(
                text = "Consumo diario (últimos 7 días)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.constrainAs(barTitle) {
                    top.linkTo(pieChart.bottom, 24.dp)
                    start.linkTo(parent.start)
                }
            )
            
            // Gráfico de barras
            AndroidView(
                factory = { context ->
                    BarChart(context).apply {
                        description.isEnabled = false
                        
                        // Zoom y gestos
                        setPinchZoom(true)
                        setScaleEnabled(true)
                        setDrawBarShadow(false)
                        setDrawGridBackground(false)
                        
                        // Ejes
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            granularity = 1f
                            setDrawGridLines(false)
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        
                        axisLeft.apply {
                            setDrawGridLines(true)
                            axisMinimum = 0f
                            typeface = Typeface.DEFAULT
                            textSize = 12f
                            
                            // Formato de valores en ml
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return "${value.toInt()} ml"
                                }
                            }
                        }
                        
                        axisRight.isEnabled = false
                        
                        // Leyenda
                        legend.apply {
                            isEnabled = true
                            verticalAlignment = Legend.LegendVerticalAlignment.TOP
                            horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                            orientation = Legend.LegendOrientation.VERTICAL
                            setDrawInside(false)
                            form = Legend.LegendForm.SQUARE
                            formSize = 12f
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        
                        // Evento de selección
                        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                e?.let {
                                    if (it is BarEntry) {
                                        val index = it.x.toInt()
                                        val valor = it.y
                                        // La etiqueta se obtiene del eje X
                                        val etiqueta = xAxis.valueFormatter.getFormattedValue(it.x, xAxis)
                                        seleccionActual = "Día: $etiqueta - Consumo: ${valor.toInt()} ml"
                                        invalidate()
                                    }
                                }
                            }
                            
                            override fun onNothingSelected() {
                                seleccionActual = ""
                                invalidate()
                            }
                        })
                    }
                },
                modifier = Modifier
                    .constrainAs(barChart) {
                        top.linkTo(barTitle.bottom, 8.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom, 16.dp)
                        width = Dimension.fillToConstraints
                        height = Dimension.value(300.dp)
                    },
                update = { chart ->
                    // Ordenar por fecha y agrupar por día
                    val formato = SimpleDateFormat("dd/MM", Locale.getDefault())
                    val calendar = Calendar.getInstance()
                    
                    // Crear mapa con los últimos 7 días
                    val diasMap = mutableMapOf<String, Int>()
                    calendar.time = Date() // Reset to today
                    
                    // Llenar el mapa con los últimos 7 días para asegurar que todos aparezcan en el gráfico
                    for (i in 6 downTo 0) {
                        calendar.add(Calendar.DAY_OF_MONTH, -i)
                        diasMap[formato.format(calendar.time)] = 0
                        calendar.add(Calendar.DAY_OF_MONTH, i) // Volver al día actual
                    }
                    
                    // Agrupar bebidas por día
                    bebidas.forEach { bebida ->
                        val fechaFormateada = formato.format(bebida.fecha)
                        diasMap[fechaFormateada] = (diasMap[fechaFormateada] ?: 0) + bebida.cantidad
                    }
                    
                    // Convertir a entradas para el gráfico, ordenadas por fecha
                    val fechasOrdenadas = diasMap.keys.sortedBy { 
                        val parts = it.split("/")
                        parts[0].toInt() * 100 + parts[1].toInt() 
                    }
                    
                    val entries = fechasOrdenadas.mapIndexed { index, fecha ->
                        BarEntry(index.toFloat(), diasMap[fecha]?.toFloat() ?: 0f)
                    }
                    
                    // Configurar etiquetas en el eje X
                    chart.xAxis.valueFormatter = IndexAxisValueFormatter(fechasOrdenadas)
                    
                    // Configurar el dataset
                    val dataSet = BarDataSet(entries, "Consumo diario").apply {
                        // Colores degradados
                        val startColor = Color.rgb(100, 181, 246)
                        val endColor = Color.rgb(30, 136, 229)
                        
                        color = startColor
                        highLightColor = endColor
                        valueTextColor = Color.BLACK
                        valueTextSize = 10f
                        valueTypeface = Typeface.DEFAULT_BOLD
                        
                        // Formato de valores
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return if (value > 0) "${value.toInt()} ml" else ""
                            }
                        }
                    }
                    
                    // Configurar data
                    val data = BarData(dataSet).apply {
                        barWidth = 0.7f
                        // Al menos 20px entre barras
                        setValueTextSize(10f)
                    }
                    
                    chart.data = data
                    
                    // Configurar espaciado
                    chart.setFitBars(true)
                    
                    // Animación
                    chart.animateY(1400, Easing.EaseInOutQuad)
                    
                    // Refrescar
                    chart.invalidate()
                }
            )
        }
    }
}
