#!/bin/bash
set -euo pipefail

FOLDER="custom-fonts"
CSS_FILE="noto-color-emoji.css"
URL="https://fonts.googleapis.com/css2?family=Noto+Color+Emoji:wght@400"

# Create directory and enter it
mkdir -p "$FOLDER"
cd "$FOLDER"

# Download CSS (use a browser User-Agent to request WOFF2)
curl -fsSL \
    -A "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36" \
    "$URL" -o "$CSS_FILE"

# Extract all WOFF2 URLs
mapfile -t URLS < <(grep -oE 'https://[^)]*\.woff2' "$CSS_FILE" | sort -u)

# Generate timestamp for cache busting
TIMESTAMP=$(date +%s)

# Download fonts and replace URLs in CSS
CSS=$(cat "$CSS_FILE")
COUNT=0

for FONT_URL in "${URLS[@]}"; do
    COUNT=$((COUNT + 1))
    FONT_FILE="noto-color-emoji.${COUNT}.woff2"

    echo "Downloading $FONT_FILE..."
    curl -fsSL "$FONT_URL" -o "$FONT_FILE"

    CSS=${CSS//"$FONT_URL"/"${FONT_FILE}?v=${TIMESTAMP}"}
done

# Save modified CSS
printf '%s\n' "$CSS" > "$CSS_FILE"

echo "Done! Downloaded $COUNT font files into $FOLDER/"
