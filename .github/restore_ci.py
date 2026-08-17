from pathlib import Path

Path('.github/workflows/ci.yml').write_text('''name: CI

on:
  push:
    branches:
      - main
      - 'agent/**'
  pull_request:
    branches:
      - main

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout
        uses: actions/checkout@v6

      - name: Set up Java 25
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: '25'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          cache-provider: basic

      - name: Run tests
        run: ./gradlew test --no-daemon

      - name: Upload test reports on failure
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: gradle-test-reports
          path: |
            build/reports/tests/test/
            build/test-results/test/
          if-no-files-found: warn
''', encoding='utf-8')
