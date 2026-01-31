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

# NO LONGER USED

CONFIG_FILE="local.conf"

# Auto-detect ds720 environment
if hostname | grep -q -i "ds720"; then
    CONFIG_FILE="ds720.conf"
fi

# Check if the first argument is a .conf file
if [[ $# -gt 0 ]] && [[ "$1" == *.conf ]]; then
    CONFIG_FILE="$1"
    shift
fi

# Verify configuration file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file '$CONFIG_FILE' not found."
    exit 1
fi

# Exit immediately if a command exits with a non-zero status.
set -euo pipefail

echo "Starting Installation Process using build config: $CONFIG_FILE"

# 1. Build Authentication Service
echo "================================================================"
echo "1. Building Authentication Service..."
./BuildAll.sh "$CONFIG_FILE" auth

# 2. Build Downloader Service
echo "================================================================"
echo "2. Building Downloader Service..."
./BuildAll.sh "$CONFIG_FILE" downloader

# 3. Build YouTube Hub
echo "================================================================"
echo "3. Building YouTube Hub..."
./BuildAll.sh "$CONFIG_FILE" youtube

echo "================================================================"
echo "Installation Complete."
