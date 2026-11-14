# HelloVoltexa — studio skeleton plugin

First plugin in the studio. Purpose: prove the build + load pipeline end to
end (Gradle build → unit smoke test → jar loads on a Paper server →
`/voltexa` smoke command responds).

- **Docs & test plan:** [../../docs/plugins/hello-voltexa/README.md](../../docs/plugins/hello-voltexa/README.md)
- **QA baseline profile for the test server:** [../../configs/baseline/README.md](../../configs/baseline/README.md)

## Build

```bash
gradle build        # requires JDK 21 (toolchain declared in build.gradle) and Gradle 8.5+
```

Produces `build/libs/hello-voltexa-0.1.0.jar`.
