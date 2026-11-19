package com.example.quizoverlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var questions: MutableList<Q>
    private lateinit var listBox: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questions = QuizStore.load(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        root.addView(TextView(this).apply { text = "Quiz Overlay"; textSize = 24f })

        root.addView(Button(this).apply {
            text = "📌 Ghim lên màn hình"
            setOnClickListener { pin() }
        })
        root.addView(Button(this).apply {
            text = "Tắt widget"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        })

        root.addView(TextView(this).apply {
            text = "Thêm câu hỏi mới"; textSize = 18f; setPadding(0, dp(20), 0, 0)
        })
        val qEdit = EditText(this).apply { hint = "Câu hỏi" }
        root.addView(qEdit)
        val optEdits = List(4) { i ->
            EditText(this).apply { hint = "Đáp án ${"ABCD"[i]}" }.also { root.addView(it) }
        }

        root.addView(TextView(this).apply { text = "Đáp án đúng:" })
        val group = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val radios = List(4) { i ->
            RadioButton(this).apply {
                text = "ABCD"[i].toString(); id = View.generateViewId()
            }.also { group.addView(it) }
        }
        radios[0].isChecked = true
        root.addView(group)

        root.addView(Button(this).apply {
            text = "➕ Thêm câu hỏi"
            setOnClickListener {
                val text = qEdit.text.toString().trim()
                val opts = optEdits.map { it.text.toString().trim() }
                if (text.isEmpty() || opts.any { it.isEmpty() }) {
                    toast("Điền đủ câu hỏi và 4 đáp án"); return@setOnClickListener
                }
                val ans = radios.indexOfFirst { it.id == group.checkedRadioButtonId }
                questions.add(Q(text, opts, ans))
                QuizStore.save(this@MainActivity, questions)
                qEdit.text.clear(); optEdits.forEach { it.text.clear() }
                refreshList()
            }
        })

        root.addView(TextView(this).apply {
            text = "Danh sách câu hỏi"; textSize = 18f; setPadding(0, dp(20), 0, dp(8))
        })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox)
        refreshList()

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun refreshList() {
        listBox.removeAllViews()
        if (questions.isEmpty()) {
            listBox.addView(TextView(this).apply { text = "Chưa có câu hỏi nào." })
        }
        questions.forEachIndexed { i, q ->
            val row = LinearLayout(this)
            row.addView(TextView(this).apply { text = "${i + 1}. ${q.text}" },
                LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Button(this).apply {
                text = "Xóa"
                setOnClickListener {
                    questions.removeAt(i)
                    QuizStore.save(this@MainActivity, questions)
                    refreshList()
                }
            })
            listBox.addView(row)
        }
    }

    private fun pin() {
        if (questions.isEmpty()) { toast("Hãy thêm ít nhất 1 câu hỏi"); return }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            toast("Bật quyền rồi bấm Ghim lại")
            return
        }
        val i = Intent(this, OverlayService::class.java)
        stopService(i)
        startForegroundService(i)
    }
}
