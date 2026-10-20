package com.garfiec.librechat.feature.chat.viewmodel

import com.garfiec.librechat.core.data.endpoint.EndpointDispatch
import kotlin.test.Test
import kotlin.test.assertEquals

class QueuedModelSpecTest {
    @Test
    fun queuedSpecSurvivesComposerEditingWithoutInferringFromTheModel() {
        val queued = QueuedMessage(
            localId = "queued",
            text = "hello",
            endpoint = "custom",
            model = "shared-model",
            modelSpec = "first-spec",
            agentId = null,
            dispatch = EndpointDispatch(null, null, null),
        )
        val composer = queued.toComposerSnapshot()
        val next = queued.copy(localId = "next", modelSpec = "second-spec")
        assertEquals("first-spec", composer.modelSpec)
        assertEquals("first-spec", queued.modelSpec)
        assertEquals("second-spec", next.toComposerSnapshot().modelSpec)
        assertEquals(composer.model, next.model)
    }
}
