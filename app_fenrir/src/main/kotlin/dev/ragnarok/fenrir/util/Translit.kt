package dev.ragnarok.fenrir.util

import java.util.*

object Translit {

    fun lat2cyr(s: String?): String? {
        if (s == null || s.isEmpty()) {
            return s
        }
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) { // Идем по строке слева направо. В принципе, подходит для обработки потока
            var ch = s[i]
            val lc = Character.isLowerCase(ch) // для сохранения регистра
            ch = Character.toUpperCase(ch)
            if (ch == 'J') { // Префиксная нотация вначале
                i++ // преходим ко второму символу сочетания
                ch = Character.toUpperCase(s[i])
                when (ch) {
                    'O' -> sb.append(ch('Ё', lc))
                    'H' -> if (i + 1 < s.length && Character.toUpperCase(s[i + 1]) == 'H') { // проверка на постфикс (вариант JHH)
                        sb.append(ch('Ъ', lc))
                        i++ // пропускаем постфикс
                    } else {
                        sb.append(ch('Ь', lc))
                    }
                    'U' -> sb.append(ch('Ю', lc))
                    'A' -> sb.append(ch('Я', lc))
                    else -> throw IllegalArgumentException("Illegal transliterated symbol '$ch' at position $i")
                }
            } else if (i + 1 < s.length && Character.toUpperCase(s[i + 1]) == 'H') { // Постфиксная нотация, требует информации о двух следующих символах. Для потока придется сделать обертку с очередью из трех символов.
                when (ch) {
                    'Z' -> sb.append(ch('Ж', lc))
                    'K' -> sb.append(ch('Х', lc))
                    'C' -> sb.append(ch('Ч', lc))
                    'S' -> if (i + 2 < s.length && Character.toUpperCase(s[i + 2]) == 'H') { // проверка на двойной постфикс
                        sb.append(ch('Щ', lc))
                        i++ // пропускаем первый постфикс
                    } else {
                        sb.append(ch('Ш', lc))
                    }
                    'E' -> sb.append(ch('Э', lc))
                    'I' -> sb.append(ch('Ы', lc))
                    else -> throw IllegalArgumentException("Illegal transliterated symbol '$ch' at position $i")
                }
                i++ // пропускаем постфикс
            } else { // одиночные символы
                when (ch) {
                    'A' -> sb.append(ch('А', lc))
                    'B' -> sb.append(ch('Б', lc))
                    'V' -> sb.append(ch('В', lc))
                    'G' -> sb.append(ch('Г', lc))
                    'D' -> sb.append(ch('Д', lc))
                    'E' -> sb.append(ch('Е', lc))
                    'Z' -> sb.append(ch('З', lc))
                    'I' -> sb.append(ch('И', lc))
                    'Y' -> sb.append(ch('Й', lc))
                    'K' -> sb.append(ch('К', lc))
                    'L' -> sb.append(ch('Л', lc))
                    'M' -> sb.append(ch('М', lc))
                    'N' -> sb.append(ch('Н', lc))
                    'O' -> sb.append(ch('О', lc))
                    'P' -> sb.append(ch('П', lc))
                    'R' -> sb.append(ch('Р', lc))
                    'S' -> sb.append(ch('С', lc))
                    'T' -> sb.append(ch('Т', lc))
                    'U' -> sb.append(ch('У', lc))
                    'F' -> sb.append(ch('Ф', lc))
                    'C' -> sb.append(ch('Ц', lc))
                    else -> sb.append(ch(ch, lc))
                }
            }
            i++ // переходим к следующему символу
        }
        return sb.toString()
    }

    private fun cyr2lat(ch: Char): String {
        return when (ch) {
            'А' -> "A"
            'Б' -> "B"
            'В' -> "V"
            'Г' -> "G"
            'Д' -> "D"
            'Е' -> "E"
            'Ё' -> "JO"
            'Ж' -> "ZH"
            'З' -> "Z"
            'И' -> "I"
            'Й' -> "Y"
            'К' -> "K"
            'Л' -> "L"
            'М' -> "M"
            'Н' -> "N"
            'О' -> "O"
            'П' -> "P"
            'Р' -> "R"
            'С' -> "S"
            'Т' -> "T"
            'У' -> "U"
            'Ф' -> "F"
            'Х' -> "KH"
            'Ц' -> "C"
            'Ч' -> "CH"
            'Ш' -> "SH"
            'Щ' -> "SHH"
            'Ъ' -> "JHH"
            'Ы' -> "IH"
            'Ь' -> "JH"
            'Э' -> "EH"
            'Ю' -> "JU"
            'Я' -> "JA"
            else -> ch.toString()
        }
    }


    fun cyr2lat(s: String?): String? {
        if (s == null || s.isEmpty()) {
            return s
        }
        val sb = StringBuilder(s.length * 2)
        for (ch in s.toCharArray()) {
            val upCh = Character.toUpperCase(ch)
            var lat = cyr2lat(upCh)
            if (ch != upCh) {
                lat = lat.lowercase(Locale.getDefault())
            }
            sb.append(lat)
        }
        return sb.toString()
    }

    /**
     * Вспомогательная функция для восстановления регистра
     */
    private fun ch(ch: Char, toLowerCase: Boolean): Char {
        return if (toLowerCase) Character.toLowerCase(ch) else ch
    }
}