package cz.handy.feature.voiceid.io

import java.io.File

/** Načte surové PCM16 little‑endian mono z disku (stejný formát jako u zápisu vět v aplikaci). */
object Pcm16LittleEndianIo {
    fun readMonoLeShorts(file: File): ShortArray {
        val bytes = file.readBytes()
        require(bytes.isNotEmpty()) { "Prázdný audio soubor." }
        require(bytes.size % 2 == 0) { "Neplatná délka PCM." }
        return ShortArray(bytes.size / 2) { i ->
            val j = i * 2
            val lo = bytes[j].toInt() and 0xFF
            val hi = bytes[j + 1].toInt()
            ((hi shl 8) or lo).toShort()
        }
    }
}
