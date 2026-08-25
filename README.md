## Project setup

After cloning the repository, run:

```bash
./gradlew installGitHooks
```

Apply formatting

```bash
./gradlew spotlessApply
```

Verify checkstyle

```bash
./gradlew check
```

Generate Jacoco report

```bash
./gradlew build jacocoTestReport
```
