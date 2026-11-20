# Copyright (C) 2026  Victor Eduardo (@victoresidoro)
#
# This file is part of AnyGest, based on HyperGesture.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

################################################################################
# The AnyGest build image, from a plain Debian: a JDK, the Android SDK and Gradle, nothing else.
# CI builds the `toolchain` stage to run tests and lint, and the default stage to produce the
# signed release APK. Nothing in it comes from another project's registry.

ARG DEBIAN_IMAGE="debian:trixie-slim@sha256:a29215f6a35e51e22adffa17f89e9d2ef06214e64a2bad10d765c46aea49f11f"
ARG ANDROID_CMDLINE_TOOLS="15859902"
ARG ANDROID_COMPILE_SDK="37.0"
ARG ANDROID_BUILD_TOOLS="37.0.0"
ARG GRADLE_VERSION="9.8.0"
ARG GRADLE_SHA256="bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c"

########################################
# Toolchain
FROM ${DEBIAN_IMAGE} AS toolchain
ARG ANDROID_CMDLINE_TOOLS
ARG ANDROID_COMPILE_SDK
ARG ANDROID_BUILD_TOOLS
ARG GRADLE_VERSION
ARG GRADLE_SHA256

ENV DEBIAN_FRONTEND=noninteractive \
  ANDROID_HOME="/opt/android-sdk" \
  ANDROID_SDK_ROOT="/opt/android-sdk" \
  JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:/opt/gradle/bin:${PATH}"

RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates openjdk-21-jdk-headless unzip wget \
  && rm -rf /var/lib/apt/lists/*

# There is no committed Gradle wrapper, so the build tool comes from here, checked by hash.
RUN wget -qO /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
  && echo "${GRADLE_SHA256}  /tmp/gradle.zip" | sha256sum -c - \
  && unzip -q /tmp/gradle.zip -d /opt \
  && mv "/opt/gradle-${GRADLE_VERSION}" /opt/gradle \
  && rm /tmp/gradle.zip \
  && gradle --version

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
  && wget -qO /tmp/android-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS}_latest.zip" \
  && unzip -q /tmp/android-tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
  && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
  && rm /tmp/android-tools.zip \
  && yes | sdkmanager --licenses >/dev/null \
  && sdkmanager --install "platform-tools" "platforms;android-${ANDROID_COMPILE_SDK}" \
  "build-tools;${ANDROID_BUILD_TOOLS}" >/dev/null

WORKDIR /workspace

########################################
# Release build. Signing is optional: without a keystore the build produces an unsigned
# release, so CI can still check that assembleRelease compiles.
FROM toolchain AS builder
ARG VERSION="1.0.0"
ARG KEYSTORE_PASSWORD=""
ARG KEYSTORE_ALIAS="hypergesture"

COPY . .

ENV KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD}" \
  KEYSTORE_ALIAS="${KEYSTORE_ALIAS}" \
  GRADLE_USER_HOME="/tmp/gradle"

RUN ./scripts/version.sh "${VERSION}" \
  && gradle --no-daemon assembleRelease

# The task's output name depends on whether signing material was present.
RUN mkdir -p /out \
  && cp "$(find app/build/outputs/apk/release -name '*.apk' | head -n 1)" /out/anygest.apk

########################################
# Only the APK leaves: this stage is FROM scratch, so neither the key nor its password
# is in a layer that could be published.
FROM scratch AS final
COPY --from=builder /out/anygest.apk /anygest.apk
