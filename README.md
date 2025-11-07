AG-UI-4K_README.md
# AG-UI-4K - Agent User Interaction Protocol Client for Kotlin

**Note: This repo is now archived. The latest version can be found in the main AG-UI repo at https://github.com/ag-ui-protocol/ag-ui/tree/main/sdks/community/kotlin**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20%7C%20JVM-lightgrey)](https://kotlinlang.org/docs/multiplatform.html)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)

A production-ready Kotlin Multiplatform client library for connecting applications to AI agents that implement the [Agent User Interaction Protocol (AG-UI)](https://docs.ag-ui.com/).

## 🎯 Why AG-UI-4K?

AG-UI-4K enables seamless integration with AI agents across all major platforms through a unified, type-safe API. 

- **True Multiplatform**: Write once, run everywhere with platform-specific optimizations
- **Modern Kotlin**: Leveraging coroutines, flows, and the latest K2 compiler for exceptional performance

## ✨ Features

### Core Capabilities
- 🔄 **Real-time Streaming**: Event-driven architecture with Kotlin Flows for live agent responses
- 🛡️ **Type Safety**: Fully typed protocol implementation with compile-time guarantees
- ⚡ **High Performance**: Optimized with K2 compiler and efficient state management
- 🔧 **Extensible Tools**: Comprehensive framework for extending agent capabilities
- 📊 **State Management**: Automatic synchronization with JSON Patch support
- 🔒 **Secure**: Built-in authentication options (Bearer, API Key, Basic Auth)

### Technical Excellence
- 📦 **Modular Architecture**: Clean separation between core, client, tools, and SDK layers
- 🎯 **Latest Tech Stack**: Kotlin 2.1.21, Ktor 3.x, kotlinx.serialization 1.8.x
- 🧪 **Well Tested**: Unit tests and Android-specific platform tests
- 📝 **Fully Documented**: Extensive KDocs, example Chat App

## 🚀 Quick Start

### Installation

Add the SDK to your project:

```kotlin
// For high-level agent interactions
dependencies {
    implementation("com.contextable:agui4k-agent-sdk:0.2.1")
}

// For direct protocol access (advanced users)
dependencies {
    implementation("com.contextable:agui4k-client:0.2.1")
    implementation("com.contextable:agui4k-core:0.2.1")
}
```

### Basic Usage

```kotlin
import com.contextable.agui4k.sdk.*
import kotlinx.coroutines.flow.collect

// Create a stateless agent
val agent = AgUi4KAgent("https://your-agent-api.com/agent") {
    bearerToken = "your-api-token"
    systemPrompt = "You are a helpful AI assistant"
}

// Send a message and receive streaming responses
agent.sendMessage("What's the weather like?").collect { state ->
    println("State updated: $state")
}
```

### Conversational Agent

```kotlin
// Create a stateful agent that maintains conversation history
val chatAgent = StatefulAgUi4KAgent("https://your-agent-api.com/agent") {
    bearerToken = "your-api-token"
    systemPrompt = "You are a friendly conversational AI"
    initialState = buildJsonObject {
        put("userName", "Alice")
        put("preferences", buildJsonObject {
            put("language", "en")
        })
    }
}

// Have a conversation
chatAgent.chat("Hello!").collect { /* ... */ }
chatAgent.chat("What's my name?").collect { state ->
    // Agent remembers the conversation context
}
```

### Tool Integration

```kotlin
// Create an agent with tools
val agent = agentWithTools(
    url = "https://your-agent-api.com/agent",
    toolRegistry = toolRegistry {
        addTool(WeatherToolExecutor())
        addTool(CalculatorToolExecutor())
        addTool(ConfirmationToolExecutor(uiHandler))
    }
) {
    bearerToken = "your-api-token"
}

// Agent can now use tools during conversation
agent.sendMessage("What's 15% tip on $85.50?").collect { state ->
    // Agent will use calculator tool automatically
}
```

## 📐 Architecture

AG-UI-4K follows a clean, modular architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Your Application                           │
├─────────────────────────────────────────────────────────────┤
│                   agui4k-agent-sdk                           │
│  ┌─────────────┐  ┌───────────────────┐  ┌──────────────┐  │
│  │ AgUi4KAgent │  │StatefulAgUi4KAgent│  │   Builders   │  │
│  └─────────────┘  └───────────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────────┤
│        agui4k-client              │      agui4k-tools        │
│  ┌────────────┐  ┌─────────────┐ │ ┌──────────────────────┐ │
│  │ HttpAgent  │  │AbstractAgent│ │ │    ToolRegistry      │ │
│  ├────────────┤  ├─────────────┤ │ ├──────────────────────┤ │
│  │EventVerifier│ │DefaultApply │ │ │   ToolExecutor      │ │
│  │ SseParser  │  │   Events    │ │ │ToolExecutionManager │ │
│  └────────────┘  └─────────────┘ │ └──────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    agui4k-core                               │
│            Protocol Types & Event Definitions                │
└─────────────────────────────────────────────────────────────┘
```

### Module Overview

- **agui4k-core**: Protocol definitions, event types, and message structures
- **agui4k-client**: Low-level client infrastructure and transport implementations
- **agui4k-tools**: Tool execution framework and built-in tool executors
- **agui4k-agent-sdk**: High-level APIs for common agent interaction patterns

## 🎯 Supported Platforms

| Platform | Status | Minimum Version | Notes |
|----------|--------|-----------------|-------|
| Android | ✅ Stable | API 26+ | Full feature support |
| iOS | ✅ Stable | iOS 13+ | Native performance (not tested beyond unit tests) |
| JVM | ✅ Stable | Java 11+ | Desktop |

## 📚 Documentation

### Essential Guides

- [API Reference](https://contextable.github.io/ag-ui-4k/) - Complete API documentation

### Example Applications
- [Chat Application](examples/chatapp) - Basic chat client
- [Tool Examples](examples/tools) - Custom tool implementations

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/contextable/ag-ui-4k.git
   cd ag-ui-4k
   ```

2. **Build the project**
   ```bash
   cd library
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```

4. **Generate documentation**
   ```bash
   ./gradlew dokkaHtml
   ```

## 🔄 Version Compatibility

| AG-UI-4K | Kotlin | Ktor | AG-UI Protocol |
|----------|--------|------|----------------|
| 0.2.x | 2.1.21+ | 3.1.x | 1.0 |

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built on the [AG-UI Protocol](https://github.com/ag-ui-protocol/ag-ui) specification
- Powered by [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- Networking by [Ktor](https://ktor.io/)
- Serialization with [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

## 📬 Support

- **Issues**: [GitHub Issues](https://github.com/contextable/ag-ui-4k/issues)
