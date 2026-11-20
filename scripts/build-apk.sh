#!/usr/bin/env sh
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

# Builds the release APK in a container and leaves anygest.apk in the repository root.
#
# Usage: build-apk.sh [version]
# Reads KEYSTORE_PASSWORD (and an optional .env) to sign; without it the APK is unsigned.
# EXTRA_BUILD_ARGS adds flags to docker buildx, which is how CI passes its layer cache.
set -e

cd "$(dirname "$0")/../"

if [ -f "${PWD}/.env" ]; then
  # shellcheck disable=SC1091 # .env is optional
  . "${PWD}/.env"
fi

version="${1#v}"
if [ -z "${version}" ]; then
  version="1.0.0"
fi

# shellcheck disable=SC2086 # EXTRA_BUILD_ARGS is a list of flags on purpose
docker buildx build \
  -f docker/build.Dockerfile \
  --build-arg VERSION="${version}" \
  --build-arg KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}" \
  ${EXTRA_BUILD_ARGS:-} \
  --output type=local,dest=. \
  .
