# Contributing to Melodix

Thank you for your interest in contributing to **Melodix**! This document
outlines the guidelines and best practices for contributing to the project.

## Code of Conduct

By participating, you agree to uphold a respectful, inclusive and
constructive environment. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Reporting Bugs

We track bugs using
[GitHub Issues](https://github.com/Cosm1cBug/Melodix/issues). Before creating
a new issue, search existing ones to avoid duplicates. When reporting a bug,
include:

- Melodix version
- Android version and device
- Steps to reproduce
- Log output (`adb logcat | grep -i melodix`)

## Suggesting Features

Feature suggestions are also handled through
[GitHub Issues](https://github.com/Cosm1cBug/Melodix/issues). Describe the
problem the feature solves and, if possible, how you imagine it working.

## Pull Requests

1. Fork the repository and create a feature branch
2. Keep changes focused and well described
3. Follow the existing code style (ktlint-friendly Kotlin)
4. Test your changes with `./gradlew assembleFossDebug`
5. Open a PR against `main` with a clear summary

## Build

Requirements: JDK 21 and Android SDK (platform 36).

```bash
git clone https://github.com/Cosm1cBug/Melodix.git
cd Melodix
chmod +x gradlew && ./gradlew assembleFossDebug
```

Thank you for contributing to Melodix!
