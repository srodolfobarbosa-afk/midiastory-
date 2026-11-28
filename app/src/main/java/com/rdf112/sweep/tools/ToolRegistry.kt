package com.rdf112.sweep.tools

import android.net.Uri

/** Stable metadata used to list and route explicitly registered on-device tools. */
data class ToolDescriptor(
    val id: String,
    val title: String,
    val summary: String,
    val requiresUserConfirmation: Boolean = true,
)

/** Input URIs must come from an Android picker or another user-authorized source. */
data class ToolRequest(
    val sourceUris: List<Uri> = emptyList(),
    val destinationUri: Uri? = null,
)

sealed interface ToolResult {
    data class Completed(
        val outputUris: List<Uri> = emptyList(),
        val bytesFreedVerified: Long? = null,
    ) : ToolResult

    data object Cancelled : ToolResult

    data class Failed(val reason: FailureReason) : ToolResult
}

enum class FailureReason {
    INVALID_INPUT,
    PERMISSION_REQUIRED,
    UNSUPPORTED_FORMAT,
    PROCESSING_FAILED,
}

/** A local operation exposed through the allowlisted tool registry. */
interface MidiastoreTool {
    val descriptor: ToolDescriptor

    suspend fun execute(request: ToolRequest): ToolResult
}

/** Registry implementations must return only explicitly constructed, allowlisted tools. */
interface ToolRegistry {
    fun allTools(): List<MidiastoreTool>

    fun find(toolId: String): MidiastoreTool?
}
