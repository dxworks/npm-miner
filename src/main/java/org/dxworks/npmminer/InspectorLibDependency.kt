package org.dxworks.npmminer

data class InspectorLibDependency(
    val name: String,
    val version: String?,
    val provider: String = "npm"
)
