package cz.handy.feature.voiceid.io

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class Pcm16LittleEndianIoTest {
    @Test
    fun readsLittleEndianMono16() {
        val t = File.createTempFile("pcm", ".raw")
        try {
            t.writeBytes(byteArrayOf(0x34.toByte(), 0x12.toByte(), 0x78.toByte(), 0x56.toByte()))
            assertContentEquals(shortArrayOf(0x1234, 0x5678), Pcm16LittleEndianIo.readMonoLeShorts(t))
        } finally {
            t.delete()
        }
    }

    @Test
    fun rejectsEmpty() {
        val t = File.createTempFile("pcm", ".raw")
        try {
            assertFailsWith<IllegalArgumentException> {
                Pcm16LittleEndianIo.readMonoLeShorts(t)
            }
        } finally {
            t.delete()
        }
    }
}
