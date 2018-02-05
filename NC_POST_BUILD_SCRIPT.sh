#!/usr/bin/env bash
set -e # Exit (and fail) immediately if any command in this scriptfails

# Check that our APK files are not bigger than they should be
python tools/metrics/apk_size.py

git reset --hard
./gradlew app:test

# skip CODECOV
#bash <(curl -s https://codecov.io/bash) -t $CODECOV_TOKEN
