# ag-ui-4k Library

This directory contains the main ag-ui-4k library source code and build infrastructure.

## Structure

- `src/commonMain/` - Shared code for all platforms
- `src/androidMain/` - Android-specific implementations
- `src/iosMain/` - iOS-specific implementations
- `src/jvmMain/` - JVM-specific implementations
- `src/commonTest/` - Shared test code
- `build.gradle.kts` - Build configuration
- `settings.gradle.kts` - Gradle settings
- `gradle.properties` - Build properties

## Building

All build commands should be run from this directory:

```bash
# Build all targets
./gradlew build

# Run tests
./gradlew test

# Run specific platform tests
./gradlew androidTest
./gradlew iosSimulatorArm64Test
./gradlew jvmTest

# Run static analysis
./gradlew detekt

# Clean build artifacts
./gradlew clean
```

## Publishing

To publish to Maven repositories:

```bash
# Publish to local Maven repository
./gradlew publishToMavenLocal

# Publish to remote repository (requires credentials)
./gradlew publish
```

## Development

When developing:
1. Always work from this directory as the root
2. Use `./gradlew` instead of system gradle
3. IDE should open this directory as the project root

## Modules

- `com.contextable.agui4k.core` - Core protocol types and base abstractions
- `com.contextable.agui4k.client` - Client implementations (HttpAgent, etc.)
- `com.contextable.agui4k.tools` - Tool execution framework
- `com.contextable.agui4k.agent-sdk` - High-level client SDK