package com.contextable.agui4k.tools.builtin

import com.contextable.agui4k.core.types.Tool
import com.contextable.agui4k.core.types.ToolCall
import com.contextable.agui4k.tools.*
import kotlinx.serialization.json.*

/**
 * Built-in tool executor for file access operations.
 * 
 * This tool allows agents to read, write, and list files through a controlled
 * interface with proper security restrictions.
 */
class FileAccessToolExecutor(
    private val fileHandler: FileHandler,
    private val allowedOperations: Set<FileOperation> = setOf(FileOperation.READ, FileOperation.LIST)
) : AbstractToolExecutor(
    tool = Tool(
        name = "file_access",
        description = "Access files and directories with read, write, and list operations",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("operation") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add("read")
                        add("write") 
                        add("list")
                        add("exists")
                    })
                    put("description", "The file operation to perform")
                }
                putJsonObject("path") {
                    put("type", "string")
                    put("description", "The file or directory path")
                }
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "Content to write (only for write operation)")
                }
                putJsonObject("encoding") {
                    put("type", "string")
                    put("description", "File encoding (default: UTF-8)")
                    put("default", "UTF-8")
                }
            }
            putJsonArray("required") {
                add("operation")
                add("path")
            }
        }
    )
) {
    
    override suspend fun executeInternal(context: ToolExecutionContext): ToolExecutionResult {
        // Parse the tool call arguments
        val args = try {
            Json.parseToJsonElement(context.toolCall.function.arguments).jsonObject
        } catch (e: Exception) {
            return ToolExecutionResult.failure("Invalid JSON arguments: ${e.message}")
        }
        
        // Extract parameters
        val operationStr = args["operation"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult.failure("Missing required parameter: operation")
        
        val path = args["path"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult.failure("Missing required parameter: path")
        
        val content = args["content"]?.jsonPrimitive?.content
        val encoding = args["encoding"]?.jsonPrimitive?.content ?: "UTF-8"
        
        // Parse operation
        val operation = try {
            FileOperation.valueOf(operationStr.uppercase())
        } catch (e: Exception) {
            return ToolExecutionResult.failure("Invalid operation: $operationStr. Must be read, write, list, or exists")
        }
        
        // Check if operation is allowed
        if (operation !in allowedOperations) {
            return ToolExecutionResult.failure("Operation '$operationStr' is not allowed")
        }
        
        // Validate operation-specific requirements
        if (operation == FileOperation.WRITE && content == null) {
            return ToolExecutionResult.failure("Content is required for write operation")
        }
        
        // Create file request
        val request = FileRequest(
            operation = operation,
            path = path,
            content = content,
            encoding = encoding,
            toolCallId = context.toolCall.id,
            threadId = context.threadId,
            runId = context.runId
        )
        
        // Execute file operation through handler
        return try {
            val response = fileHandler.handleFileRequest(request)
            
            val resultJson = buildJsonObject {
                put("operation", operationStr)
                put("path", path)
                put("success", response.success)
                
                when (operation) {
                    FileOperation.READ -> {
                        if (response.success && response.content != null) {
                            put("content", response.content)
                            put("size", response.content.length)
                        }
                    }
                    FileOperation.WRITE -> {
                        if (response.success) {
                            put("bytesWritten", response.bytesWritten ?: 0)
                        }
                    }
                    FileOperation.LIST -> {
                        if (response.success && response.entries != null) {
                            putJsonArray("entries") {
                                response.entries.forEach { entry ->
                                    addJsonObject {
                                        put("name", entry.name)
                                        put("type", entry.type)
                                        put("size", entry.size)
                                        if (entry.lastModified != null) {
                                            put("lastModified", entry.lastModified)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FileOperation.EXISTS -> {
                        put("exists", response.exists ?: false)
                    }
                }
                
                if (!response.success && response.error != null) {
                    put("error", response.error)
                }
            }
            
            if (response.success) {
                ToolExecutionResult.success(
                    result = resultJson,
                    message = response.message ?: "File operation completed successfully"
                )
            } else {
                ToolExecutionResult.failure(
                    message = response.error ?: "File operation failed",
                    result = resultJson
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult.failure("File operation failed: ${e.message}")
        }
    }
    
    override fun validate(toolCall: ToolCall): ToolValidationResult {
        val args = try {
            Json.parseToJsonElement(toolCall.function.arguments).jsonObject
        } catch (e: Exception) {
            return ToolValidationResult.failure("Invalid JSON arguments: ${e.message}")
        }
        
        val errors = mutableListOf<String>()
        
        // Check required fields
        val operationStr = args["operation"]?.jsonPrimitive?.content
        if (operationStr.isNullOrBlank()) {
            errors.add("Missing or empty required parameter: operation")
        } else {
            // Validate operation
            try {
                val operation = FileOperation.valueOf(operationStr.uppercase())
                if (operation !in allowedOperations) {
                    errors.add("Operation '$operationStr' is not allowed")
                }
                
                // Validate operation-specific requirements
                if (operation == FileOperation.WRITE) {
                    val content = args["content"]?.jsonPrimitive?.content
                    if (content.isNullOrEmpty()) {
                        errors.add("Content is required for write operation")
                    }
                }
            } catch (e: Exception) {
                errors.add("Invalid operation: $operationStr. Must be read, write, list, or exists")
            }
        }
        
        val path = args["path"]?.jsonPrimitive?.content
        if (path.isNullOrBlank()) {
            errors.add("Missing or empty required parameter: path")
        }
        
        return if (errors.isEmpty()) {
            ToolValidationResult.success()
        } else {
            ToolValidationResult.failure(errors)
        }
    }
    
    override fun getMaxExecutionTimeMs(): Long? {
        // File operations should be fast, 30 seconds max
        return 30_000L
    }
}

/**
 * File operation types.
 */
enum class FileOperation {
    READ,
    WRITE, 
    LIST,
    EXISTS
}

/**
 * Request for file operations.
 */
data class FileRequest(
    val operation: FileOperation,
    val path: String,
    val content: String? = null,
    val encoding: String = "UTF-8",
    val toolCallId: String,
    val threadId: String? = null,
    val runId: String? = null
)

/**
 * Response from file operations.
 */
data class FileResponse(
    val success: Boolean,
    val content: String? = null,
    val bytesWritten: Int? = null,
    val entries: List<FileEntry>? = null,
    val exists: Boolean? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * File or directory entry.
 */
data class FileEntry(
    val name: String,
    val type: String, // "file" or "directory"
    val size: Long,
    val lastModified: String? = null
)

/**
 * Interface for handling file operations.
 * 
 * Implementations should provide the actual file system access with proper
 * security restrictions and sandboxing.
 */
interface FileHandler {
    /**
     * Handle a file operation request.
     * 
     * @param request The file operation request
     * @return The operation response
     * @throws Exception if the operation fails
     */
    suspend fun handleFileRequest(request: FileRequest): FileResponse
}

/**
 * Mock file handler for testing that simulates file operations.
 */
class MockFileHandler : FileHandler {
    private val mockFiles = mutableMapOf<String, String>()
    
    init {
        // Add some mock files
        mockFiles["/readme.txt"] = "This is a sample readme file."
        mockFiles["/config.json"] = """{"setting": "value", "enabled": true}"""
        mockFiles["/data/users.csv"] = "id,name,email\n1,John,john@example.com\n2,Jane,jane@example.com"
    }
    
    override suspend fun handleFileRequest(request: FileRequest): FileResponse {
        return when (request.operation) {
            FileOperation.READ -> {
                val content = mockFiles[request.path]
                if (content != null) {
                    FileResponse(
                        success = true,
                        content = content,
                        message = "File read successfully"
                    )
                } else {
                    FileResponse(
                        success = false,
                        error = "File not found: ${request.path}"
                    )
                }
            }
            
            FileOperation.WRITE -> {
                mockFiles[request.path] = request.content ?: ""
                FileResponse(
                    success = true,
                    bytesWritten = request.content?.length ?: 0,
                    message = "File written successfully"
                )
            }
            
            FileOperation.LIST -> {
                val pathPrefix = if (request.path.endsWith("/")) request.path else "${request.path}/"
                val entries = mockFiles.keys
                    .filter { it.startsWith(pathPrefix) && it != request.path }
                    .map { path ->
                        val relativePath = path.removePrefix(pathPrefix)
                        val isDirectory = relativePath.contains("/")
                        val name = if (isDirectory) {
                            relativePath.substringBefore("/")
                        } else {
                            relativePath
                        }
                        
                        FileEntry(
                            name = name,
                            type = if (isDirectory) "directory" else "file",
                            size = if (isDirectory) 0 else mockFiles[path]?.length?.toLong() ?: 0,
                            lastModified = "2024-01-01T12:00:00Z"
                        )
                    }
                    .distinctBy { it.name }
                
                FileResponse(
                    success = true,
                    entries = entries,
                    message = "Directory listed successfully"
                )
            }
            
            FileOperation.EXISTS -> {
                val exists = mockFiles.containsKey(request.path)
                FileResponse(
                    success = true,
                    exists = exists,
                    message = if (exists) "File exists" else "File does not exist"
                )
            }
        }
    }
}