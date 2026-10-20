package com.garfiec.librechat.feature.chat.util

import com.garfiec.librechat.core.common.EndpointConstants
import com.garfiec.librechat.core.model.config.ModelSpec
import com.garfiec.librechat.core.model.config.ModelSpecs
import com.garfiec.librechat.core.model.config.StartupConfig

/**
 * The selection a server's model spec asks new chats to open on.
 *
 * [prioritized] specs open every new chat, ahead of the user's last-used model. The others are
 * soft defaults: they apply only when the user has no usable selection of their own.
 */
internal data class SpecSeed(
    val endpoint: String,
    val model: String,
    val prioritized: Boolean,
    val spec: String,
)

/**
 * Port of upstream's `getDefaultModelSpec` (`client/src/utils/endpoints.ts`) combined with the
 * `shouldApplyModelSpec` gate in `useNewConvo.ts`, which decides whether its answer is used.
 *
 * [lastSpec] is upstream's stored `LAST_CONVO_SETUP.spec`: the spec name of the user's last pick,
 * null when that pick was a raw model. "Yields to the user's prior selection" for a soft default
 * with no stored spec is decided by the seeder (a usable last-used wins), not here.
 */
internal fun resolveDefaultModelSpec(config: StartupConfig?, lastSpec: String? = null): SpecSeed? =
    resolveDefaultModelSpec(config?.modelSpecs, config?.interfaceConfig?.modelSelect ?: true, lastSpec)

internal fun resolveDefaultModelSpec(specs: ModelSpecs?, modelSelect: Boolean, lastSpec: String?): SpecSeed? {
    if (specs == null || specs.list.isEmpty()) return null
    val prioritized = specs.prioritize == true || !modelSelect

    val hard = specs.list.firstOrNull { it.isDefault }
    if (hard != null) {
        // Upstream returns it either way, but only applies it under these conditions.
        return if (prioritized) hard.toSeed(prioritized = true) else null
    }
    val soft = specs.list.firstOrNull { it.softDefault }
    // Upstream applies a stored spec on every new chat, whatever `prioritize` says.
    val last = lastSpec?.let { name -> specs.list.firstOrNull { it.name == name } }
    if (last != null && last.name != soft?.name) {
        last.toSeed(prioritized = true)?.let { return it }
    }
    if (soft != null) {
        // With the selector off there is no prior selection to yield to. `prioritize` does not
        // upgrade a soft default: upstream checks the soft branch first.
        return soft.toSeed(prioritized = !modelSelect)
    }
    return if (prioritized) specs.list.first().toSeed(prioritized = true) else null
}

internal fun ModelSpec.toSeed(prioritized: Boolean = false): SpecSeed? {
    if (name.isBlank()) return null
    val preset = preset ?: return null
    val agentId = preset.agentId?.takeIf { it.isNotBlank() }
    if (agentId != null) return SpecSeed(EndpointConstants.AGENTS, agentId, prioritized, name)
    val endpoint = preset.endpoint?.takeIf { it.isNotBlank() } ?: return null
    val model = preset.model?.takeIf { it.isNotBlank() } ?: return null
    return SpecSeed(endpoint, model, prioritized, name)
}

/**
 * Whether the picker lists [endpoint]'s raw models and agents next to the named specs. Mirrors
 * the `addedEndpoints` filter in upstream's `useEndpoints`. Under `enforce` with no
 * `addedEndpoints`, nothing raw is listed: web lists everything there, and every pick fails.
 */
internal fun ModelSpecs?.showsRawEndpoint(endpoint: String): Boolean =
    this == null || (addedEndpoints.isEmpty() && !enforce) || endpoint in addedEndpoints

/**
 * Whether the server rejects any send that names no spec (`buildEndpointOption.js`). It has no
 * exception for `addedEndpoints`, so raw rows shown under it can't be sent to.
 */
internal val ModelSpecs?.requiresSpec: Boolean
    get() = this != null && enforce && list.isNotEmpty()
