package com.example.melody.data.session

import javax.inject.Inject

interface EpochClock {
    fun nowSeconds(): Long
}

class SystemEpochClock @Inject constructor() : EpochClock {
    override fun nowSeconds(): Long = System.currentTimeMillis() / 1000L
}
