package com.taxihelper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = getSharedPreferences("s", MODE_PRIVATE)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        val fields = listOf("fuel" to "Расход, л/100 км" to 8f, "fprice" to "Цена бензина, ₽/л" to 60f,
            "amort" to "Амортизация, ₽/км" to 3f, "minkm" to "Мин. чистыми, ₽/км" to 25f,
            "speed" to "Ср. скорость в поездке, км/ч" to 45f,
            "basekm" to "База тарифа без кэфа, ₽/км (оценка)" to 13f,
            "skiprating" to "Скип, если рейтинг пассажира ≤" to 4.86f)
        val edits = fields.map { (kl, def) ->
            val (key, label) = kl
            box.addView(TextView(this).apply { text = label })
            EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(p.getFloat(key, def).toString())
            }.also { box.addView(it) }
            key to box.getChildAt(box.childCount - 1) as EditText
        }
        box.addView(CheckBox(this).apply {
            text = "Отладка: показывать найденный текст"; isChecked = p.getBoolean("debug", true)
            setOnCheckedChangeListener { _, c -> p.edit().putBoolean("debug", c).apply() }
        })
        box.addView(Button(this).apply {
            text = "Сохранить"
            setOnClickListener {
                val e = p.edit()
                edits.forEach { (k, v) -> v.text.toString().toFloatOrNull()?.let { e.putFloat(k, it) } }
                e.apply(); Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
            }
        })
        box.addView(Button(this).apply {
            text = "Включить службу (Спец. возможности)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        setContentView(ScrollView(this).apply { addView(box) })
    }
}
