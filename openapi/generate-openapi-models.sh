#!/bin/sh -e

# Script copied from iOS source and adjusted
# Please note that running this script will replace converters and models in app/src/main/java/com/babylon/wallet/android/data/gateway/generated.
# Model generation is not ideal and it has few compile errors and it is necessary to do manual replace of package names for both models and converters.
# Script also generates api files with retrofit calls, one file per endpoint, which I manually merged into one GatewayApi interface, and deleted separate API files.
# Still, it is faster to generate those models this way then translating by hand. I'm open to improvements and suggestions on best way to maintain this generation.
# For now, this seemed sufficient

GEN_TOOL=openapi-generator
if ! [ -x "$(command -v $GEN_TOOL)" ]; then
  echo "Error: '$GEN_TOOL' is not installed. Install it with `brew install openapi-generator`" >&2
  exit 1
fi


cd $(dirname "$0")
INPUTDIR=$PWD
API_SPEC="$INPUTDIR/gateway-api-schema.yml"
OUTPUTDIR="$PWD/temp_generated"
DESTINATION="../app/src/main/java/com/babylon/wallet/android/data/gateway/generated"

echo "🔮 Generating Gateway API models using '$GEN_TOOL' based on '$API_SPEC'"
echo "🎯 Destination for generated files: '$DESTINATION'"

rm -rf temp_generated
$GEN_TOOL generate -i "$API_SPEC" \
-g kotlin \
-o "$OUTPUTDIR" \
--additional-properties=serializationLibrary=kotlinx_serialization,library=jvm-retrofit2,packageName=com.babylon.wallet.android.data.gateway.generated,useCoroutines=true

echo "✨ Generation of models done, Removing some files we don't need."
cd "$OUTPUTDIR"
cd ..
cp $OUTPUTDIR/src/main/kotlin/com/babylon/wallet/android/data/gateway/generated/models/*.kt $DESTINATION/models
cp $OUTPUTDIR/src/main/kotlin/com/babylon/wallet/android/data/gateway/generated/infrastructure/*.kt $DESTINATION/infrastructure
cp $OUTPUTDIR/src/main/kotlin/com/babylon/wallet/android/data/gateway/generated/apis/*.kt $DESTINATION

rm -rf temp_generated
