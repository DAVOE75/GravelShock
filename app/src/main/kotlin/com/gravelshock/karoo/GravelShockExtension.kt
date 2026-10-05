package com.gravelshock.karoo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.RemoteViews
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.ViewConfig

class GravelShockExtension : KarooExtension("gravelshock", "0.0.5"), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private var terrainEmitter: Emitter<StreamState>? = null
    private var fatigueEmitter: Emitter<StreamState>? = null

    private var terrainViewEmitter: ViewEmitter? = null
    private var fatigueViewEmitter: ViewEmitter? = null

    override val types: List<io.hammerhead.karooext.extension.DataTypeImpl> = listOf(
        object : io.hammerhead.karooext.extension.DataTypeImpl("gravelshock", "gravelshock_terrain") {
            override fun startStream(emitter: Emitter<StreamState>) {
                terrainEmitter = emitter
            }
            override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
                terrainViewEmitter = emitter
            }
        },
        object : io.hammerhead.karooext.extension.DataTypeImpl("gravelshock", "gravelshock_fatigue") {
            override fun startStream(emitter: Emitter<StreamState>) {
                fatigueEmitter = emitter
            }
            override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
                fatigueViewEmitter = emitter
            }
        }
    )

    private val smoothingWindow = mutableListOf<Double>()
    private val maxWindowSize = 250 // ~5 segundos a 50Hz
    
    private var accumulatedFatigue = 0.0
    private var currentTerrain = 1.0
    
    private var countSmooth = 0
    private var countGravel = 0
    private var countCobbles = 0
    private var countMTB = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("GravelShock", "Extension Started")
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d("GravelShock", "Accelerometer registered")
        } ?: run {
            Log.e("GravelShock", "No accelerometer found!")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("GravelShock", "Extension Stopped")
    }

    private fun getTerrainClassification(vibration: Double): Double {
        return when {
            vibration < 0.15 -> 1.0 // Smooth (antes 0.05)
            vibration < 0.45 -> 2.0 // Gravel (antes 0.15)
            vibration < 0.90 -> 3.0 // Cobbles (antes 0.35)
            else -> 4.0 // MTB
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            
            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble())
            val vibration = Math.abs(gForce - 1.0)
            
            smoothingWindow.add(vibration)
            if (smoothingWindow.size > maxWindowSize) {
                smoothingWindow.removeAt(0)
            }
            
            val avgVibration = smoothingWindow.average()
            val terrain = getTerrainClassification(avgVibration)
            currentTerrain = terrain
            
            when (terrain) {
                1.0 -> countSmooth++
                2.0 -> countGravel++
                3.0 -> countCobbles++
                4.0 -> countMTB++
            }
            
            accumulatedFatigue += vibration * 0.01 
            
            val terrainTypeId = types[0].dataTypeId
            val fatigueTypeId = types[1].dataTypeId
            
            terrainEmitter?.onNext(StreamState.Streaming(DataPoint(terrainTypeId, mapOf(DataType.Field.SINGLE to terrain))))
            fatigueEmitter?.onNext(StreamState.Streaming(DataPoint(fatigueTypeId, mapOf(DataType.Field.SINGLE to accumulatedFatigue))))
            
            updateViews()
        }
    }
    
    private fun getTerrainName(terrain: Double): String {
        return when (terrain) {
            1.0 -> "Asfalto Liso"
            2.0 -> "Gravel"
            3.0 -> "Adoquines"
            4.0 -> "MTB / Roto"
            else -> "Desconocido"
        }
    }
    
    private fun getTerrainColor(terrain: Double): Int {
        return when (terrain) {
            1.0 -> android.graphics.Color.parseColor("#388E3C") // Verde intenso
            2.0 -> android.graphics.Color.parseColor("#FBC02D") // Amarillo intenso
            3.0 -> android.graphics.Color.parseColor("#F57C00") // Naranja intenso
            4.0 -> android.graphics.Color.parseColor("#D32F2F") // Rojo intenso
            else -> android.graphics.Color.WHITE
        }
    }
    
    private var lastViewUpdate = 0L
    private fun updateViews() {
        val now = System.currentTimeMillis()
        if (now - lastViewUpdate < 500) return
        lastViewUpdate = now
        
        terrainViewEmitter?.let { emitter ->
            val views = RemoteViews(packageName, R.layout.view_terrain_v5)
            
            // Número gigante y en su color
            views.setTextViewText(R.id.terrain_number, currentTerrain.toInt().toString())
            views.setTextColor(R.id.terrain_number, getTerrainColor(currentTerrain))
            
            // Texto descriptivo debajo
            views.setTextViewText(R.id.terrain_text, getTerrainName(currentTerrain))
            
            val total = countSmooth + countGravel + countCobbles + countMTB
            if (total > 0) {
                val p1 = (countSmooth * 100) / total
                val p2 = (countGravel * 100) / total
                val p3 = (countCobbles * 100) / total
                val p4 = (countMTB * 100) / total
                
                val percentText = "AL: $p1% | Grv: $p2% | Ado: $p3% | MTB: $p4%"
                views.setTextViewText(R.id.terrain_percentages, percentText)
                
                // Generar bitmap de la barra
                val width = 400
                val height = 20
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint()
                
                var currentX = 0f
                
                paint.color = android.graphics.Color.parseColor("#388E3C") // Verde intenso
                var w = (countSmooth.toFloat() / total.toFloat()) * width
                canvas.drawRect(currentX, 0f, currentX + w, height.toFloat(), paint)
                currentX += w
                
                paint.color = android.graphics.Color.parseColor("#FBC02D") // Amarillo intenso
                w = (countGravel.toFloat() / total.toFloat()) * width
                canvas.drawRect(currentX, 0f, currentX + w, height.toFloat(), paint)
                currentX += w
                
                paint.color = android.graphics.Color.parseColor("#F57C00") // Naranja intenso
                w = (countCobbles.toFloat() / total.toFloat()) * width
                canvas.drawRect(currentX, 0f, currentX + w, height.toFloat(), paint)
                currentX += w
                
                paint.color = android.graphics.Color.parseColor("#D32F2F") // Rojo intenso
                w = (countMTB.toFloat() / total.toFloat()) * width
                canvas.drawRect(currentX, 0f, currentX + w, height.toFloat(), paint)
                
                views.setImageViewBitmap(R.id.terrain_bar, bitmap)
            }
            
            emitter.updateView(views)
        }
        
        fatigueViewEmitter?.let { emitter ->
            val views = RemoteViews(packageName, R.layout.view_fatigue_v2)
            views.setTextViewText(R.id.fatigue_number, String.format("%.1f", accumulatedFatigue))
            
            val prog = Math.min((accumulatedFatigue * 10).toInt(), 1000)
            views.setProgressBar(R.id.fatigue_progress, 1000, prog, false)
            
            emitter.updateView(views)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
