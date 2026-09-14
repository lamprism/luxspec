package com.lamprism.luxspec.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfigSpecDslTest {
    @Test
    fun buildsThroughTheKotlinReceiverDsl() {
        val spec = ConfigSpecDsl.spec("server.name", ConfigCodec.string()) {
            defaultValue("local")
            sensitive()
        }

        assertEquals("local", spec.defaultValue)
        assertTrue(spec.isSensitive)
    }
}
