# AG-UI-4K Documentation

This directory contains documentation generation setup for the AG-UI-4K library.

## Documentation Generation

The project uses [Dokka](https://kotlinlang.org/docs/dokka-introduction.html) to generate comprehensive API documentation from KDoc comments.

### Local Generation

To generate documentation locally:

```bash
cd library
./gradlew dokkaGenerate
```

This will generate HTML documentation for each module in:
- `agui4k-core/build/dokka/html/`
- `agui4k-client/build/dokka/html/`
- `agui4k-agent-sdk/build/dokka/html/`
- `agui4k-tools/build/dokka/html/`

### GitHub Pages

Documentation is automatically generated and published to GitHub Pages when changes are pushed to the `main` branch.

The GitHub Actions workflow:
1. Builds the library
2. Generates Dokka documentation for all modules
3. Combines the documentation into a single site
4. Publishes to GitHub Pages

### Documentation Structure

The published documentation includes:
- **agui4k-core**: Core types, events, and protocol definitions
- **agui4k-client**: Client implementations, transport layer, and state management
- **agui4k-agent-sdk**: High-level stateful and stateless agent SDKs
- **agui4k-tools**: Tool system, error handling, and execution management

### Manual Workflow Trigger

You can manually trigger documentation generation by:
1. Going to the Actions tab in GitHub
2. Selecting the "Documentation" workflow
3. Clicking "Run workflow"

## Development

When adding new public APIs:
1. Add comprehensive KDoc comments
2. Include parameter descriptions (`@param`)
3. Include return value descriptions (`@return`)
4. Document any exceptions (`@throws`)
5. Add usage examples where helpful

The documentation will be automatically updated on the next push to `main`.