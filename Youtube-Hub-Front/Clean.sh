#!/bin/bash

# The MIT License (MIT)
#
# Copyright (c) 2025 Che-Hung Lin
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# Service and Image Definitions
SVC_FRONTEND="youtube-hub-frontend"
IMG_FRONTEND="youtube-hub-frontend"

# Exit immediately if a command exits with a non-zero status.
set -euo pipefail

clean_all() {
  echo "Stopping and removing Docker containers and networks..."
  # Use docker-compose down to stop and remove containers and networks.
  # The --remove-orphans flag cleans up any containers not defined in the compose file.
  # The || true prevents the script from exiting if the containers don't exist.
  docker-compose down --remove-orphans || true
  echo "Stopping and removing Docker containers and networks...done!"

  echo "Removing Docker images..."
  docker image rm ${IMG_FRONTEND} 2>/dev/null || true
  echo "Removing Docker images...done!"
}

if [ $# -eq 0 ]; then
  set -- "all"
fi

DO_ALL=false
DO_FRONTEND=false

for arg in "$@"; do
  case "${arg}" in
    frontend) DO_FRONTEND=true ;;
    all) DO_ALL=true ;;
    help)
      echo "Usage: $0 {frontend|all|help} [more args...]"
      exit 0
      ;;
    *)
      echo "Unknown argument: ${arg}"
      echo "Usage: $0 {frontend|all|help} [more args...]"
      exit 1
      ;;
  esac
done

if [[ "${DO_ALL}" == "true" ]]; then
  clean_all
else
  if [[ "${DO_FRONTEND}" == "true" ]]; then
    clean_frontend
  fi
fi

exit 0
