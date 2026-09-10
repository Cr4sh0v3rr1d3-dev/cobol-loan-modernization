#!/bin/bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME="C:\Users\Albedo\java\jdk-17.0.20+8"
./mvnw -q clean package
