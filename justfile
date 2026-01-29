# Justfile for Hytale Adapter
# Platform Adapter (sa-hytale-adapter)
# ✅ ONLY module allowed to import Hytale classes

# Default recipe
default: build

# Project variables
project_name := "hytale-adapter"
maven_artifact := "hytale-adapter"
version := "1.0.0-SNAPSHOT"

# Hytale mods directory
hytale_mods_dir := if os() == "windows" { 
    env_var("APPDATA") + "/Hytale/UserData/Mods" 
} else { 
    env_var("HOME") + "/.hytale/mods" 
}

# ============================================================
# BUILD COMMANDS
# ============================================================

# Build the project
build:
    @echo "🔨 Building Hytale Adapter..."
    mvn package -DskipTests
    @echo "✅ Build complete: target/hytale-adapter-1.0.0-SNAPSHOT.jar"

# Clean build artifacts
clean:
    @echo "🧹 Cleaning build artifacts..."
    mvn clean
    @echo "✅ Clean complete"

# Full rebuild
rebuild: clean build

# Compile without packaging
compile:
    @echo "🔨 Compiling sources..."
    mvn compile

# Build shaded JAR with all dependencies
build-shaded:
    @echo "🔨 Building shaded JAR..."
    mvn package -DskipTests
    @echo "✅ Shaded JAR: target/hytale-adapter-1.0.0-SNAPSHOT.jar"

# ============================================================
# TEST COMMANDS
# ============================================================

# Run all tests
test:
    @echo "🧪 Running tests..."
    mvn test
    @echo "✅ Tests complete"

# Run tests with coverage report
test-coverage:
    @echo "🧪 Running tests with coverage..."
    mvn test jacoco:report
    @echo "📊 Coverage report: target/site/jacoco/index.html"

# Run a specific test class
test-class CLASS:
    @echo "🧪 Running test class: ..."
    mvn test -Dtest=

# ============================================================
# INSTALL & DEPLOY
# ============================================================

# Install to local Maven repository
install:
    @echo "📦 Installing to local Maven repo..."
    mvn install -DskipTests
    @echo "✅ Installed: com.argonathsystems.adapter:hytale-adapter:1.0.0-SNAPSHOT"

# Deploy JAR to Hytale mods folder
deploy: build
    @echo "🚀 Deploying to Hytale mods folder..."
    @mkdir -p ""
    cp "target/hytale-adapter-1.0.0-SNAPSHOT.jar" "/"
    @echo "✅ Deployed to: /hytale-adapter-1.0.0-SNAPSHOT.jar"

# Remove from Hytale mods folder
undeploy:
    @echo "🗑️ Removing from Hytale mods folder..."
    rm -f "/hytale-adapter-*.jar"
    @echo "✅ Removed"

# ============================================================
# HYTALE SERVER MANAGEMENT
# ============================================================

# Install HytaleServer.jar to local Maven repo (uses $HYTALE_SERVER_JAR env var)
install-hytale-server:
    #!/usr/bin/env bash
    set -e
    
    # Check if HYTALE_SERVER_JAR is set
    if [ -z "$HYTALE_SERVER_JAR" ]; then
        echo "❌ Error: HYTALE_SERVER_JAR environment variable not set"
        echo "Please source set_env-*.sh first:"
        echo "  source set_env-anduril.sh  # or set_env-sauron.sh"
        exit 1
    fi
    
    # Check if JAR exists
    if [ ! -f "$HYTALE_SERVER_JAR" ]; then
        echo "❌ Error: HytaleServer.jar not found at: $HYTALE_SERVER_JAR"
        exit 1
    fi
    
    echo "📦 Installing HytaleServer.jar to local Maven repo..."
    echo "   Source: $HYTALE_SERVER_JAR"
    
    mvn install:install-file \
        -Dfile="$HYTALE_SERVER_JAR" \
        -DgroupId="com.hypixel.hytale" \
        -DartifactId="HytaleServer-parent" \
        -Dversion="1.0-SNAPSHOT" \
        -Dpackaging=jar
    
    echo "✅ HytaleServer.jar installed to local Maven repository"
    echo "   GroupId: com.hypixel.hytale"
    echo "   ArtifactId: HytaleServer-parent"
    echo "   Version: 1.0-SNAPSHOT"

# ============================================================
# CODE QUALITY
# ============================================================

# Run linting/code style checks
lint:
    @echo "🔍 Running code style checks..."
    mvn checkstyle:check

# Run all quality checks
check: lint test

# ============================================================
# DOCUMENTATION
# ============================================================

# Generate JavaDoc
docs:
    @echo "📚 Generating JavaDoc..."
    mvn javadoc:javadoc
    @echo "📖 JavaDoc: target/site/apidocs/index.html"

# ============================================================
# UTILITY
# ============================================================

# Show project info
info:
    @echo "📋 Project: Hytale Adapter"
    @echo "   Type: Platform Adapter (sa)"
    @echo "   Group: com.argonathsystems.adapter"
    @echo "   Artifact: hytale-adapter"
    @echo "   Version: 1.0.0-SNAPSHOT"
    @echo "   License: MIT"
    @echo ""
    @echo "✅ HYTALE IMPORTS ALLOWED in this module"
    @echo "   Hytale Mods Dir: "

# Show dependency tree
deps:
    mvn dependency:tree

# Show available recipes
help:
    @just --list