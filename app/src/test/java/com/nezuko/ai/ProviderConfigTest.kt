package com.nezuko.ai

import com.nezuko.ai.ai.ProviderConfig
import com.nezuko.ai.ai.ProviderKind
import org.junit.Assert.*
import org.junit.Test

class ProviderConfigTest {
    @Test fun defaultProviderIdentifierIsNonEmpty() {
        val config = ProviderConfig(ProviderKind.GEMINI, "", "gemini-2.0-flash")
        assertEquals(ProviderKind.GEMINI, config.kind)
        assertTrue(config.model.isNotBlank())
        assertTrue(config.apiKey.isBlank())
    }
    @Test fun providerKindsAreStableNames() {
        assertEquals(ProviderKind.OPENROUTER, ProviderKind.valueOf("OPENROUTER"))
    }
}
