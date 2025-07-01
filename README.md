# ag-ui-4k - AG-UI Protocol Client for Kotlin Multiplatform

A Kotlin Multiplatform (KMP) client library for connecting to AI agents that implement the Agent User Interaction Protocol (AG-UI).

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-blue.svg?logo=kotlin)](http://kotlinlang.org)

## Overview

ag-ui-4k is a multiplatform **client library** that enables applications to connect to and communicate with AI agents using the AG-UI protocol. This library provides a type-safe, coroutine-based API for real-time, event-driven interactions with AI services.

**Important**: ag-ui-4k is a client-side library for connecting TO agents, not for implementing agents themselves. See [OVERVIEW.md](OVERVIEW.md) for more details.

Built with the latest Kotlin technology stack including the K2 compiler, Ktor 3, and kotlinx.serialization 1.8, ag-ui-4k delivers exceptional performance and developer experience across all supported platforms.

### Features

- 🚀 **Multiplatform Support**: Connect to AG-UI agents from Android, iOS, JVM, and more
- 🔄 **Real-time Streaming**: Event-driven architecture with Kotlin Flows for receiving agent responses
- 🛡️ **Type Safety**: Fully typed events and messages from the AG-UI protocol
- ⚡ **Coroutine-based**: Built on Kotlin Coroutines for efficient async operations
- 🔧 **Extensible**: Easy to create custom client implementations
- 📦 **Lightweight**: Minimal dependencies, powered by Ktor
- 🎯 **K2 Compiler**: Optimized with Kotlin 2.1.21's K2 compiler for better performance
- 🆕 **Latest Stack**: Built with Ktor 3.1.3 and kotlinx.serialization 1.8.1

## Requirements

- Kotlin 2.1.21 or higher
- Java 11 or higher
- Gradle 8.5 or higher
- Android API 21+ (for Android targets)
- iOS 13+ (for iOS targets)

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.contextable:ag-ui-4k:0.1.0")
}
```

### Maven

```xml
<dependency>
    <groupId>com.contextable</groupId>
    <artifactId>ag-ui-4k</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

```kotlin
import com.contextable.agui4k.client.HttpAgent
import com.contextable.agui4k.core.types.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Create an HTTP client to connect to an AG-UI agent
val client = HttpAgent {
    url = "https://your-agent-endpoint.com/agent"
    headers = mapOf(
        "Authorization" to "Bearer your-api-key"
    )
}

// Send a message to the agent
client.addMessage(
    UserMessage(
        id = "1",
        content = "Hello, how can you help me today?"
    )
)

// Connect to the agent and handle responses
scope.launch {
    client.runAgent {
        tools = listOf(
            Tool(
                name = "confirmAction",
                description = "Ask user to confirm an action",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "action" to mapOf(
                            "type" to "string",
                            "description" to "The action to confirm"
                        )
                    ),
                    "required" to listOf("action")
                )
            )
        )
    }.collect { event ->
        when (event) {
            is TextMessageContentEvent -> {
                // Handle streaming text from the agent
                println("Agent: ${event.delta}")
            }
            is ToolCallStartEvent -> {
                // Agent is requesting to use a tool
                println("Tool call: ${event.toolCallName}")
            }
            is StateSnapshotEvent -> {
                // Agent state has been updated
                println("State updated: ${event.snapshot}")
            }
            // Handle other events...
        }
    }
}
```

## Project Structure

The project is organized with the main library code and build infrastructure in the `library` directory:

- `/library` - Main library module with all source code and build files
- `/docs` - Documentation and guides
- `/examples` - Example applications (coming soon)
- `/.github` - GitHub Actions CI/CD configuration

All development work happens in the `library` directory. See the [library README](library/README.md) for detailed build instructions.

## Architecture

The library follows a clean architecture with clear separation of concerns:

- **Core Module**: Protocol definitions, event types, and base client abstractions
- **Client Module**: Concrete client implementations (HttpAgent) for connecting to agents
- **Platform Modules**: Platform-specific optimizations and integrations

### Key Components

1. **AbstractAgent**: Base class for client implementations
2. **HttpAgent**: HTTP-based client using Server-Sent Events (SSE) to connect to AG-UI agents
3. **Event System**: Strongly typed events for receiving agent responses
4. **State Management**: Efficient state synchronization with snapshots and deltas from the agent

## Supported Platforms

- ✅ Android (API 21+)
- ✅ iOS (iOS 13+)
- ✅ JVM (Java 11+)
- 🚧 JS (Browser) - Coming soon
- 🚧 Native (Linux, macOS, Windows) - Coming soon

## Documentation

For detailed documentation, please visit our [Wiki](https://github.com/contextable/agui4k/wiki).

### Important Documents

- [OVERVIEW](OVERVIEW.md) - Understanding what AGUI4K is (and isn't)
- [CHANGELOG](CHANGELOG.md) - Version history and release notes
- [CONTRIBUTING](CONTRIBUTING.md) - How to contribute to the project
- [PERFORMANCE](PERFORMANCE.md) - Performance optimization guide

### Examples

Check out the [examples](examples/) directory for:
- Android sample app
- iOS sample app
- Multiplatform chat application

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

1. Clone the repository
2. Open in IntelliJ IDEA or Android Studio (open the `library` directory as the project)
3. Build using one of these methods:
   - **From library directory**: `cd library && ./gradlew build`
   - **Using helper script**: `./build.sh build` (Unix) or `build.bat build` (Windows)
4. Run tests: `./build.sh test` or `cd library && ./gradlew test`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Inspired by the [AG-UI Protocol](https://github.com/ag-ui/protocol)
- Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- Powered by [Ktor](https://ktor.io/) for networking