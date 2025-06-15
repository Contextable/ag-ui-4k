## Naming Conventions

You'll notice classes like `AbstractAgent` and fields like `agentId` in the codebase. These names come from the AG-UI protocol specification. Despite the naming, remember that ag-ui-4k implements **clients** that connect to agents, not the agents themselves.

- `AbstractAgent`: Base class for client implementations
- `HttpAgent`: HTTP client implementation  
- `agentId`: Identifier for the client instance
- `runAgent()`: Method to connect to an agent

These names are retained for consistency with the AG-UI protocol.# ag-ui-4k Overview

## What is ag-ui-4k?

ag-ui-4k is a **client library** for Kotlin Multiplatform that enables applications to connect to and communicate with AI agents that implement the Agent User Interaction Protocol (AG-UI).

## What ag-ui-4k Does

- **Connects to AG-UI Agents**: Establishes connections to remote AI agents via HTTP/SSE
- **Handles Event Streams**: Processes real-time event streams from agents
- **Manages Conversations**: Tracks message history between users and agents
- **Provides Tools**: Allows agents to request actions through tool calls
- **Synchronizes State**: Maintains state synchronization between client and agent

## What ag-ui-4k Does NOT Do

- **Does NOT implement agents**: This is a client library only
- **Does NOT provide AI capabilities**: It connects to existing AI agents
- **Does NOT host agents**: It's purely a client-side connection library

## Architecture

```
┌─────────────────┐         ┌─────────────────┐
│   Your App      │         │   AI Agent      │
│                 │         │   (Remote)      │
│  ┌───────────┐  │  HTTP   │                 │
│  │ ag-ui-4k  │──┼─────────┤► AG-UI Protocol│
│  │  Client   │  │   SSE   │   Endpoint      │
│  └───────────┘  │         │                 │
└─────────────────┘         └─────────────────┘
```

## Use Cases

1. **Chat Applications**: Connect your chat UI to AI agents
2. **Virtual Assistants**: Build apps that interact with AI assistants
3. **Automation Tools**: Create tools that leverage AI agents for tasks
4. **Development Tools**: Build IDEs or tools that use AI for code assistance

## Key Components

### HttpAgent
The main client implementation that:
- Connects to AG-UI agents via HTTP
- Receives streaming responses via Server-Sent Events (SSE)
- Handles connection lifecycle and error recovery

### Event System
Processes various event types from agents:
- **Text Messages**: Streaming text responses
- **Tool Calls**: Agent requests for specific actions
- **State Updates**: Synchronization of agent state
- **Lifecycle Events**: Connection status and errors

### Message Management
- Tracks conversation history
- Supports multiple message types (user, assistant, system, tool)
- Maintains message ordering and relationships

## Getting Started

```kotlin
// 1. Create a client
val client = HttpAgent(HttpAgentConfig(
    url = "https://your-agent.example.com/agent",
    headers = mapOf("Authorization" to "Bearer token")
))

// 2. Send messages
client.addMessage(UserMessage(
    id = "1",
    content = "Hello, AI agent!"
))

// 3. Connect and receive responses
client.runAgent().collect { event ->
    when (event) {
        is TextMessageContentEvent -> print(event.delta)
        // Handle other events...
    }
}
```

## Platform Support

- ✅ **Android**: Full support with Ktor Android engine
- ✅ **iOS**: Full support with Ktor Darwin engine  
- ✅ **JVM**: Full support with Ktor Java engine
- 🚧 **JS/Browser**: Coming soon
- 🚧 **Native Desktop**: Coming soon

## Technology Stack

- **Kotlin 2.1.21**: With K2 compiler for optimal performance
- **Ktor 3.1.3**: Modern, coroutine-based HTTP client
- **kotlinx.serialization 1.8.1**: Fast, reflection-free JSON handling
- **Coroutines & Flow**: Efficient async and streaming support