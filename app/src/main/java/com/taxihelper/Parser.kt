package com.taxihelper

data class Order(val price: Double, val pickupKm: Double?, val pickupMin: Int?,
                 val tripKm: Double?, val tripMin: Int?, val rating: Double?, val raw: String, val surge: Boolean = false)

object Parser {
    private val priceRe = Regex("""(\+)?\s*(\d[\d ]*(?:[.,]\d+)?)\s*₽""")
    private val pickupRe = Regex("""(\d+(?:[.,]\d+)?)\s*(км|м)\s*[·•]\s*(\d+)\s*мин""")
    private val distRe = Regex("""(\d+(?:[.,]\d+)?)\s*(км|м)(?![а-яa-zA-Z])""")
    private val durRe = Regex("""(?:(\d+)\s*ч)?\s*(\d+)\s*мин""")
    private val star1 = Regex("""★\s*(\d[.,]\d+)|(\d[.,]\d+)\s*★""")
    private val star2 = Regex("""(?<![\d.,])([1-5][.,]\d{2})(?!\d)""")

    private fun num(s: String) = s.replace(" ", "").replace(',', '.').toDouble()
    private fun km(v: String, u: String) = num(v).let { if (u == "м") it / 1000 else it }
    private fun mins(m: MatchResult) = (m.groupValues[1].toIntOrNull() ?: 0) * 60 + m.groupValues[2].toInt()

    fun parse(texts: List<String>): Order? {
        var t = texts.joinToString("|").replace('\u00A0', ' ').replace('\u202F', ' ')
        val pr = priceRe.find(t) ?: return null
        val price = num(pr.groupValues[2])
        val surge = pr.groupValues[1].isNotEmpty()
        val raw = t
        var pKm: Double? = null; var pMin: Int? = null
        val pm = pickupRe.find(t)
        if (pm != null) {
            pKm = km(pm.groupValues[1], pm.groupValues[2]); pMin = pm.groupValues[3].toInt()
            t = t.removeRange(pm.range)
        }
        val dists = distRe.findAll(t).map { km(it.groupValues[1], it.groupValues[2]) }.toList()
        val durs = durRe.findAll(t).map { mins(it) }.toList()
        if (pKm == null) { pKm = dists.getOrNull(0); pMin = durs.getOrNull(0) }
        val tripKm = if (pm != null) dists.getOrNull(0) else dists.getOrNull(1)
        val tripMin = if (pm != null) durs.maxOrNull() else durs.getOrNull(1)
        val r = star1.find(t)?.let { (it.groupValues[1].ifEmpty { it.groupValues[2] }).replace(',', '.').toDouble() }
            ?: star2.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDouble()
        return Order(price, pKm, pMin, tripKm, tripMin, r, raw, surge)
    }
}
