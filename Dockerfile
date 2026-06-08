# =======================================================================
#                      FAMILY EMERGENCY VAULT - ANDROID BUILD DOCKERFILE
# =======================================================================
# This Dockerfile sets up a complete, reproducible environment containing
# JDK 17, the Android SDK, Build Tools, and Platform APIs required to
# compile, test, and package the Family Emergency Vault Android App.
# =======================================================================

# ----------------- STAGE 1: BASE SYSTEM SETUP -----------------
# We use Eclipse Temurin (Ubuntu-based LTS OpenJDK) as our parent image
FROM eclipse-temurin:17-jdk-jammy AS builder

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
# Coordinates can be fetched or verified via: https://developer.android.com/studio#command-tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -f /tmp/cmdline-tools.zip

# Auto-accept all license agreements required by Android SDK
RUN yes | sdkmanager --licenses

# Download appropriate Platforms & Build-Tools
# Family Emergency Vault targets Android SDK/api-level 36 and minapi 24
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

# ----------------- STAGE 3: APPLICATION COMPILATION -----------------
# Create workspace directory
WORKDIR /workspace

# Copy local repository files into the docker image
COPY . .

# Since this workspace doesn't use the standard gradle wrapper checked-in,
# we download and cache Gradle within the Docker image to make builds self-contained.
ENV GRADLE_VERSION=8.6
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm -f /tmp/gradle.zip
ENV PATH=${PATH}:/opt/gradle-${GRADLE_VERSION}/bin

# Pre-build to cache dependencies and verify project integrity
RUN gradle dependencies --no-daemon

# Compile & Assemble the debug/release APK
RUN gradle assembleDebug --no-daemon

# Output directory for artifacts
VOLUME ["/workspace/app/build/outputs/apk"]

# Default entrypoint builds a fresh clean APK
CMD ["gradle", "assembleDebug", "--no-daemon"]
