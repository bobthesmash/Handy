package cz.handy.feature.nlu

import kotlinx.coroutines.runBlocking

internal fun UtteranceNluParser.blockingParse(utterance: String): NluResult =
    runBlocking {
        parse(utterance)
    }
