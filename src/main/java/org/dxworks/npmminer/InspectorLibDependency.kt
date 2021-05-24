package org.dxworks.npmminer

data class InspectorLibDependency(
    val name: String,
    var version: String?,
    val provider: String = "npm"
)
