#!/bin/sh
set -eu

R2_ENDPOINT="https://$CF_R2_ACCOUNT_ID.r2.cloudflarestorage.com"
PREFIX="music"

r2_cp() {
  src="$1"
  key="$2"
  cache_control="$3"
  content_type="$4"

  AWS_ACCESS_KEY_ID="$CF_R2_ACCESS_KEY_ID" \
  AWS_SECRET_ACCESS_KEY="$CF_R2_SECRET_ACCESS_KEY" \
  AWS_DEFAULT_REGION=auto \
  aws s3 cp "$src" "s3://$CF_R2_BUCKET/$key" \
    --endpoint-url "$R2_ENDPOINT" \
    --only-show-errors \
    --cache-control "$cache_control" \
    --content-type "$content_type"
}

upload_apk_latest_and_versioned() {
  src="$1"
  filename="$2"

  r2_cp "$src" "$PREFIX/$VERSION/$filename" \
    "public, max-age=31536000, immutable" \
    "application/vnd.android.package-archive"

  r2_cp "$src" "$PREFIX/$filename" \
    "no-store" \
    "application/vnd.android.package-archive"
}

echo "Building content..."
./gradlew --stacktrace --info assembleUnstableRelease

VERSION="$(git describe --tags)"

ls "./app/build/outputs/apk/unstable/release/"

echo "Deploying music artifacts to Cloudflare R2..."
upload_apk_latest_and_versioned "./app/build/outputs/apk/stable/release/app-unstable-release.apk" "app-unstable-release.apk"

tmp_version="$(mktemp)"
printf '%s\n' "$VERSION" > "$tmp_version"
r2_cp "$tmp_version" "$PREFIX/$VERSION/version-unstable.txt" \
  "public, max-age=31536000, immutable" \
  "text/plain; charset=utf-8"
r2_cp "$tmp_version" "$PREFIX/version-unstable.txt" \
  "no-store" \
  "text/plain; charset=utf-8"
rm -f "$tmp_version"

tmp_changelog="$(mktemp)"
git tag -l --format='%(contents)' "$VERSION" > "$tmp_changelog"
r2_cp "$tmp_changelog" "$PREFIX/changelogs-unstable/$VERSION" \
  "public, max-age=31536000, immutable" \
  "text/plain; charset=utf-8"
rm -f "$tmp_changelog"

echo "Done."