package com.taxihelper

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class OrderService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var tv: TextView? = null
    private var lastKey = ""

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collect(root, texts)
        val o = Parser.parse(texts) ?: return
        val key = "${o.price}|${o.pickupKm}|${o.tripKm}"
        if (key == lastKey) return
        lastKey = key
        show(o)
    }

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (n == null) return
        n.text?.let { out.add(it.toString()) }
        n.contentDescription?.let { out.add(it.toString()) }
        for (i in 0 until n.childCount) collect(n.getChild(i), out)
    }

    private fun show(o: Order) {
        val p = getSharedPreferences("s", MODE_PRIVATE)
        val fuel = p.getFloat("fuel", 8f); val fprice = p.getFloat("fprice", 60f)
        val amort = p.getFloat("amort", 3f); val minKm = p.getFloat("minkm", 25f)
        val speed = p.getFloat("speed", 45f); val debug = p.getBoolean("debug", true)
        val baseKm = p.getFloat("basekm", 13f)
        val skipRating = p.getFloat("skiprating", 4.86f)
        val pickup = o.pickupKm ?: 0.0
        val tripKm = o.tripKm ?: o.tripMin?.let { it / 60.0 * speed }
        val est = o.tripKm == null
        var color = Color.rgb(90, 90, 90)
        val lines = ArrayList<String>()
        if (tripKm != null) {
            val km = pickup + tripKm
            val total = if (o.surge) o.price + tripKm * baseKm else o.price
            val net = total - km * (fuel / 100 * fprice + amort)
            val netKm = net / km
            color = when {
                netKm >= minKm -> Color.rgb(30, 140, 60)
                netKm >= minKm * 0.85 -> Color.rgb(200, 150, 0)
                else -> Color.rgb(190, 40, 40)
            }
            lines.add(if (netKm >= minKm) "БРАТЬ" else if (netKm >= minKm * 0.85) "НА ГРАНИ" else "НЕ БРАТЬ")
            lines.add("%.0f ₽/км чистыми (грязными %.0f)".format(netKm, total / km))
            if (o.surge) lines.add("Кэф +%.0f ₽ + база ≈%.0f ₽".format(o.price, tripKm * baseKm))
            lines.add("Подача %.1f км · поездка %s%.0f км".format(pickup, if (est) "≈" else "", tripKm))
            o.tripMin?.let { lines.add("≈ %.0f ₽/час чистыми".format(net / (((o.pickupMin ?: 0) + it) / 60.0))) }
        } else {
            lines.add("НЕТ ДАННЫХ О ПОЕЗДКЕ")
            lines.add((if (o.surge) "Кэф +" else "") + "%.0f ₽, подача %.1f км".format(o.price, pickup))
        }
        if (o.rating != null && o.rating <= skipRating + 0.001) {
            color = Color.rgb(190, 40, 40)
            val head = "НЕ БРАТЬ: рейтинг ★%.2f".format(o.rating)
            if (lines.isEmpty()) lines.add(head) else lines[0] = head
        }
        o.rating?.let { lines.add("Пассажир ★%.2f".format(it) + if (it < 4.7) " ⚠" else "") }
        if (debug) lines.add("[" + o.raw.take(250) + "]")
        val txt = lines.joinToString("\n")

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        tv?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        val v = TextView(this).apply {
            text = txt; setTextColor(Color.WHITE); textSize = 18f
            setBackgroundColor(color); setPadding(36, 24, 36, 24)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP; y = 120 }
        wm.addView(v, lp)
        tv = v
        h.removeCallbacksAndMessages(null)
        h.postDelayed({ try { wm.removeView(v) } catch (_: Exception) {}; tv = null }, 12000)
    }

    override fun onInterrupt() {}
}
