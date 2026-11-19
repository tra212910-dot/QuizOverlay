package com.example.quizoverlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {

    private var questions: List<Q> = emptyList()
    private var index = 0
    private var score = 0
    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        questions = QuizStore.load(this)
        startAsForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 40; y = 200 }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = 40f
            }
            elevation = 24f
        }
        wm.addView(root, params)
        render()
    }

    private fun startAsForeground() {
        val ch = "quiz"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ch, "Quiz", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, ch)
            .setContentTitle("Quiz đang chạy")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun enableDrag(v: View) {
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = params.x; sy = params.y; tx = e.rawX; ty = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = sx + (e.rawX - tx).toInt()
                    params.y = sy + (e.rawY - ty).toInt()
                    wm.updateViewLayout(root, params)
                }
            }
            true
        }
    }

    private fun render() {
        root.removeAllViews()

        val bar = LinearLayout(this)
        val title = TextView(this).apply {
            text = if (index < questions.size)
                "☰ Câu ${index + 1}/${questions.size}  (giữ để kéo)" else "☰ Kết quả"
            setTextColor(Color.GRAY); textSize = 12f
        }
        enableDrag(title)
        bar.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(TextView(this).apply {
            text = "✕"; textSize = 18f; setPadding(24, 0, 0, 0)
            setOnClickListener { stopSelf() }
        })
        root.addView(bar)

        if (index >= questions.size) {
            root.addView(TextView(this).apply {
                text = "Xong! Điểm: $score/${questions.size}"
                textSize = 20f; setTextColor(Color.BLACK); setPadding(0, 24, 0, 24)
            })
            root.addView(Button(this).apply {
                text = "Làm lại"
                setOnClickListener { index = 0; score = 0; render() }
            })
            return
        }

        val q = questions[index]
        root.addView(TextView(this).apply {
            text = q.text; textSize = 18f
            setTextColor(Color.BLACK); setPadding(0, 24, 0, 16)
        })

        val buttons = mutableListOf<Button>()
        val next = Button(this).apply {
            text = if (index == questions.size - 1) "Xem kết quả" else "Tiếp →"
            visibility = View.GONE
            setOnClickListener { index++; render() }
        }
        q.options.forEachIndexed { i, opt ->
            val b = Button(this).apply { text = opt; setAllCaps(false) }
            b.setOnClickListener {
                buttons.forEach { it.isEnabled = false }
                if (i == q.answer) score++
                else b.setBackgroundColor(Color.parseColor("#EF9A9A"))
                buttons[q.answer].setBackgroundColor(Color.parseColor("#A5D6A7"))
                next.visibility = View.VISIBLE
            }
            buttons.add(b)
            root.addView(b)
        }
        root.addView(next)
    }

    override fun onDestroy() {
        wm.removeView(root)
        super.onDestroy()
    }
}
