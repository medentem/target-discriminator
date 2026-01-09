#!/bin/bash
# Sync media from submodule to Android assets directory
# This script copies media files from the git submodule to the Android assets folder
# Run this before building the Android app

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$(dirname "$SCRIPT_DIR")"
MEDIA_DIR="$ANDROID_DIR/media"
ASSETS_DIR="$ANDROID_DIR/app/src/main/assets"

echo "========================================="
echo "Syncing media from submodule to Android assets..."
echo "========================================="

# Check if media submodule exists
if [ ! -d "$MEDIA_DIR" ]; then
    echo "ERROR: Media submodule directory not found at $MEDIA_DIR"
    echo "Please run: git submodule update --init --recursive"
    exit 1
fi

# Ensure submodule is initialized and updated
if [ ! -d "$MEDIA_DIR/.git" ]; then
    echo "Initializing submodule..."
    cd "$ANDROID_DIR"
    git submodule update --init --recursive
fi

# Update submodule to latest commit (skip in CI environments or if at specific commit)
# In CI, submodule is already at the correct commit
if [ -z "$CI" ]; then
    echo "Updating submodule to latest..."
    cd "$MEDIA_DIR"
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    if [ "$CURRENT_BRANCH" != "HEAD" ]; then
        # Only pull if we're on a branch (not detached HEAD)
        git pull origin main 2>/dev/null || git pull origin master 2>/dev/null || echo "Warning: Could not pull latest changes"
    else
        echo "Submodule is at a specific commit, skipping pull"
    fi
    cd "$ANDROID_DIR"
else
    echo "CI environment detected, using submodule at current commit"
fi

# Create assets directory structure
echo "Creating assets directory structure..."
mkdir -p "$ASSETS_DIR/photos/threat"
mkdir -p "$ASSETS_DIR/photos/non_threat"
mkdir -p "$ASSETS_DIR/videos/threat"
mkdir -p "$ASSETS_DIR/videos/non_threat"

# Copy media files
echo "Copying media files..."
if command -v rsync &> /dev/null; then
    # Use rsync for better performance and incremental updates
    rsync -av --delete \
        "$MEDIA_DIR/photos/threat/" "$ASSETS_DIR/photos/threat/"
    rsync -av --delete \
        "$MEDIA_DIR/photos/non_threat/" "$ASSETS_DIR/photos/non_threat/"
    rsync -av --delete \
        "$MEDIA_DIR/videos/threat/" "$ASSETS_DIR/videos/threat/"
    rsync -av --delete \
        "$MEDIA_DIR/videos/non_threat/" "$ASSETS_DIR/videos/non_threat/"
else
    # Fallback to cp
    echo "Using cp (rsync not available)..."
    rm -rf "$ASSETS_DIR/photos" "$ASSETS_DIR/videos"
    cp -r "$MEDIA_DIR/photos" "$ASSETS_DIR/"
    cp -r "$MEDIA_DIR/videos" "$ASSETS_DIR/"
fi

# Count files
PHOTO_COUNT=$(find "$ASSETS_DIR/photos" -type f 2>/dev/null | wc -l | tr -d ' ')
VIDEO_COUNT=$(find "$ASSETS_DIR/videos" -type f 2>/dev/null | wc -l | tr -d ' ')

echo "========================================="
echo "Media sync complete!"
echo "Photos: $PHOTO_COUNT files"
echo "Videos: $VIDEO_COUNT files"
echo "========================================="

