package edu.utexas.jinheng.drawtogether.realtimedraw

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import edu.utexas.jinheng.drawtogether.R
import edu.utexas.jinheng.drawtogether.model.DrawPath
import edu.utexas.jinheng.drawtogether.model.DrawPoint
import edu.utexas.jinheng.drawtogether.sensor.ShakeSensorEventListener
import edu.utexas.jinheng.drawtogether.view.PencilView

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private val TAG = javaClass.simpleName

    private lateinit var surfaceView: SurfaceView

    private var ratio = -1.0
    private var marginLeft = 0.0
    private var marginTop = 0.0
    private var currentColor = "Charcoal"
    private var currentPath: DrawPath? = null
    private lateinit var currentPencil: PencilView
    private val nameToColorMap = HashMap<String, Int>()
    private val colorIdToName = HashMap<Int, String>()

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometerSensor: Sensor
    private lateinit var shakeSensorEventListener: ShakeSensorEventListener

    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var allPath: ArrayList<DrawPath>

    private var currentKey: String? = null
    private lateinit var pointRef: DatabaseReference
    private lateinit var pushRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        surfaceView = findViewById(R.id.surface_view)

        initEvents()
    }

    private fun initEvents() {
        surfaceView.holder.addCallback(this)

        generateColorMap()

        currentPencil = findViewById(R.id.charcoal)
        currentPencil.isSelected = true

        initializeShakeSensor()

        allPath = ArrayList()

        database = FirebaseDatabase.getInstance()
        myRef = database.reference

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading")
        progressDialog.setCancelable(false)
        progressDialog.show()

        pushRef = database.getReference("paper").push()
        val paper = database.getReference("paper")
        paper.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allPath = ArrayList()
                for (snapshot in dataSnapshot.children) {
                    val drawPath = DrawPath()
                    drawPath.color = (snapshot.child("color").value as String?).toString()

                    val points = ArrayList<DrawPoint>()
                    for (aPoint in snapshot.child("points").children) {
                        points.add(DrawPoint(aPoint.child("x").value as Double, aPoint.child("y").value as Double))
                    }
                    drawPath.points = points
                    allPath.add(drawPath)
                }
                draw(allPath)
                progressDialog.dismiss()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG, "onCancelled")
            }
        })
    }

    private fun generateColorMap() {
        nameToColorMap["Charcoal"] = 0xff1c283f.toInt()
        nameToColorMap["Elephant"] = 0xff9a9ba5.toInt()
        nameToColorMap["Dove"] = 0xffebebf2.toInt()
        nameToColorMap["Ultramarine"] = 0xff39477f.toInt()
        nameToColorMap["Indigo"] = 0xff59569e.toInt()
        nameToColorMap["GrapeJelly"] = 0xff9a50a5.toInt()
        nameToColorMap["Mulberry"] = 0xffd34ca3.toInt()
        nameToColorMap["Flamingo"] = 0xfffe5192.toInt()
        nameToColorMap["SexySalmon"] = 0xfff77c88.toInt()
        nameToColorMap["Peach"] = 0xfffc9f95.toInt()
        nameToColorMap["Melon"] = 0xfffcc397.toInt()
        colorIdToName[R.id.charcoal] = "Charcoal"
        colorIdToName[R.id.elephant] = "Elephant"
        colorIdToName[R.id.dove] = "Dove"
        colorIdToName[R.id.ultramarine] = "Ultramarine"
        colorIdToName[R.id.indigo] = "Indigo"
        colorIdToName[R.id.grape_jelly] = "GrapeJelly"
        colorIdToName[R.id.mulberry] = "Mulberry"
        colorIdToName[R.id.flamingo] = "Flamingo"
        colorIdToName[R.id.sexy_salmon] = "SexySalmon"
        colorIdToName[R.id.peach] = "Peach"
        colorIdToName[R.id.melon] = "Melon"
    }

    private fun wipeCanvas() {
        myRef.removeValue()
    }

    fun onViewClicked(view: View) {
        val colorName = colorIdToName[view.id] ?: return

        currentColor = colorName
        if (view is PencilView) {
            currentPencil.isSelected = false
            currentPencil.invalidate()
            val pencil = view
            pencil.isSelected = true
            pencil.invalidate()
            currentPencil = pencil
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(shakeSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeSensorEventListener)
    }

    private fun initializeShakeSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        shakeSensorEventListener = ShakeSensorEventListener()
        shakeSensorEventListener.setOnShakeListener(object : ShakeSensorEventListener.OnShakeListener {
            override fun onShake(count: Int) {
                wipeCanvas()
            }
        })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        ratio = -1.0
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val isPortrait = width < height
        ratio = if (isPortrait) {
            EDGE_WIDTH.toDouble() / height
        } else {
            EDGE_WIDTH.toDouble() / width
        }
        if (isPortrait) {
            marginLeft = (width - height) / 2.0
            marginTop = 0.0
        } else {
            marginLeft = 0.0
            marginTop = (height - width) / 2.0
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val viewLocation = IntArray(2)
        surfaceView.getLocationInWindow(viewLocation)
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN
            || action == MotionEvent.ACTION_MOVE
            || action == MotionEvent.ACTION_UP
            || action == MotionEvent.ACTION_CANCEL) {
            val x = event.rawX
            val y = event.rawY
            val pointX = (x - marginLeft - viewLocation[0]) * ratio
            val pointY = (y - marginTop - viewLocation[1]) * ratio

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    currentKey = pushRef.key
                    pointRef = database.getReference("paper/$currentKey/points")

                    currentPath = DrawPath()
                    currentPath?.color = currentColor
                    val point = DrawPoint(pointX, pointY)
                    currentPath?.points?.add(point)
                    pushRef.setValue(currentPath)
                }
                MotionEvent.ACTION_MOVE -> {
                    val point = DrawPoint(pointX, pointY)
                    currentPath?.points?.add(point)
                    pointRef.push().setValue(point)
                }
                MotionEvent.ACTION_UP -> {
                    val point = DrawPoint(pointX, pointY)
                    currentPath?.points?.add(point)
                    pointRef.push().setValue(point)
                    currentPath = null
                    pushRef = database.getReference("paper").push()
                }
            }
            return true
        }
        return false
    }

    private fun draw(results: ArrayList<DrawPath>) {
        var canvas: Canvas? = null

        try {
            val holder = surfaceView.holder
            canvas = holder.lockCanvas()
            canvas?.drawColor(Color.WHITE)
        } finally {
            if (canvas != null) {
                surfaceView.holder.unlockCanvasAndPost(canvas)
            }
        }

        try {
            val holder = surfaceView.holder
            canvas = holder.lockCanvas()

            synchronized(holder) {
                canvas?.drawColor(Color.WHITE)
                val paint = Paint()
                for (drawPath in results) {
                    val points = drawPath.points
                    val color = nameToColorMap[drawPath.color]
                    if (color != null) {
                        paint.color = color
                    } else {
                        paint.color = nameToColorMap[currentColor] ?: 0
                    }
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = (4 / ratio).toFloat()
                    val iterator = points.iterator()
                    val firstPoint = iterator.next()
                    val path = Path()
                    val firstX = (firstPoint.x / ratio + marginLeft).toFloat()
                    val firstY = (firstPoint.y / ratio + marginTop).toFloat()
                    path.moveTo(firstX, firstY)
                    while (iterator.hasNext()) {
                        val point = iterator.next()
                        val x = (point.x / ratio + marginLeft).toFloat()
                        val y = (point.y / ratio + marginTop).toFloat()
                        path.lineTo(x, y)
                    }
                    canvas?.drawPath(path, paint)
                }
            }
        } finally {
            if (canvas != null) {
                surfaceView.holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    companion object {
        private const val EDGE_WIDTH = 683
    }
}