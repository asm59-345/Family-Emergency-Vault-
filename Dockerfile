# =======================================================================
#                      FAMILY EMERGENCY VAULT - ANDROID BUILD DOCKERFILE
# =======================================================================
# This Dockerfile sets up a complete, reproducible environment containing
# JDK 21, the Android SDK, Build Tools, and Platform APIs required to
# compile, test, and package the Family Emergency Vault Android App.
# =======================================================================

# ----------------- STAGE 1: BASE SYSTEM SETUP -----------------
# We use Eclipse Temurin (Ubuntu-based LTS OpenJDK 21) as our parent image.
# AGP 9.1.1 and Kotlin 2.2 require Java 21+.
FROM eclipse-temurin:21-jdk-jammy AS builder

LABEL description="Reproducible developer environment container for Family Emergency Vault Android Application."
LABEL version="1.0"

# Non-interactive apt installation
ENV DEBIAN_FRONTEND=noninteractive

# Install critical system tools and utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    unzip \
    wget \
    zip \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# ----------------- STAGE 2: ANDROID SDK INSTALLATION -----------------
# Set environment variables for the Android SDK coordinates
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Download and install Android CMDLINE Tools (latest stable)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -f /tmp/cmdline-tools.zip

# Auto-accept all license agreements required by Android SDK
RUN yes | sdkmanager --licenses

# Download appropriate Platforms & Build-Tools
# Family Emergency Vault targets Android SDK 36 (minorApiLevel 1 requires platforms;android-36.1 and build-tools 36.1.0/36.0.0)
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-36.1" \
    "platforms;android-36" \
    "platforms;android-34" \
    "build-tools;36.1.0" \
    "build-tools;36.0.0" \
    "build-tools;34.0.0"

# ----------------- STAGE 3: APPLICATION COMPILATION -----------------
# Create workspace directory
WORKDIR /workspace

# Copy local repository files into the docker image
COPY . .

# Ensure Gradle wrapper is executable
RUN chmod +x ./gradlew

# Configure Android SDK location for deterministic build
RUN echo "sdk.dir=/opt/android-sdk" > /workspace/local.properties

# Generate standard Android debug keystore required by app/build.gradle.kts (debugConfig signingConfig)
# using standard Android debug credentials (storepass android, alias androiddebugkey, keypass android),
# ensuring host keystores/secrets are never copied into the container image.
RUN if [ ! -f /workspace/debug.keystore ]; then \
        keytool -genkey -v -keystore /workspace/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"; \
    fi

# Pre-build to cache dependencies using the project's Gradle wrapper
RUN ./gradlew dependencies :app:dependencies --no-daemon

# Compile & Assemble the debug APK
RUN ./gradlew assembleDebug --no-daemon

# Run unit tests to verify build integrity
RUN ./gradlew testDebugUnitTest --no-daemon

# Output directory for artifacts
VOLUME ["/workspace/app/build/outputs/apk"]

# Default entrypoint builds a fresh clean APK using the project wrapper
CMD ["./gradlew", "assembleDebug", "--no-daemon"]
