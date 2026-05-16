package cz.handy.feature.asr

/**
 * Sherpa-onnx `acceptWaveform` očekává **mono float32** v rozsahu zhruba **[-1,1]** @ 16 kHz
 * (stejný scaling jako oficiální Android demo).
 */
fun ShortArray.asSherpaWaveformMono16kHz(): FloatArray = FloatArray(size) { idx -> this[idx] / SHERPA_PCM16_FLOAT_SCALE }

internal const val SHERPA_PCM16_FLOAT_SCALE = 32768f
