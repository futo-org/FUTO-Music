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
./gradlew --stacktrace bundlePlaystoreRelease

VERSION="$(git describe --tags)"

echo "Deploying music artifacts to Cloudflare R2..."
upload_apk_latest_and_versioned "./app/build/outputs/bundle/playstoreRelease/app-playstore-release.aab" "app-playstore-release.aab"


echo "Done."
