#!/bin/bash
# MIGRATION-001: Fix accessor files to remove old SDK references
# This script updates accessor implementation files to throw UnsupportedOperationException
# with detailed SDK requirement messages instead of using the deleted com.hytale.api.* package

set -e

ADAPTER_DIR="/mnt/d/Gaming/Argonath-Systems/02-adapter-hytale"
BACKUP_DIR="$ADAPTER_DIR/docs/migration-001/accessor-backups-$(date +%Y%m%d-%H%M%S)"

echo "========================================="
echo "MIGRATION-001: Accessor SDK Fix Script"
echo "========================================="
echo ""
echo "Creating backup directory: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# List of files to process
FILES=(
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleEventAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleSchedulerAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleCommandAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleStorageAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleWorldAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleUIAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleInventoryAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNPCEntityAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleAssetAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleHologramAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleItemAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleModelAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNotificationAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleSoundAccessor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/HytaleAdapterPlugin.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/HytaleAdapterProvider.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/HytalePlatform.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/listener/HytaleAdapterEventListener.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/thread/HytaleWorldExecutor.java"
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/util/ComponentHelper.java"
    "src/main/java/com/argonathsystems/adapter/hytale/ui/ActionBarAdapter.java"
    "src/main/java/com/argonathsystems/adapter/hytale/ui/CombatFramesAdapter.java"
    "src/main/java/com/argonathsystems/adapter/hytale/ui/DialoguePageAdapter.java"
    "src/main/java/com/argonathsystems/adapter/hytale/ui/QuestBookPageAdapter.java"
    "src/main/java/com/argonathsystems/adapter/hytale/ui/VendorPageAdapter.java"
)

echo "Files to process: ${#FILES[@]}"
echo ""

# Backup all files first
echo "Step 1: Backing up files..."
for file in "${FILES[@]}"; do
    if [ -f "$ADAPTER_DIR/$file" ]; then
        backup_path="$BACKUP_DIR/$file"
        mkdir -p "$(dirname "$backup_path")"
        cp "$ADAPTER_DIR/$file" "$backup_path"
        echo "  ✓ Backed up: $file"
    else
        echo "  ⚠ File not found: $file"
    fi
done

echo ""
echo "Step 2: Removing old SDK import statements..."
cd "$ADAPTER_DIR"

# Remove old SDK imports from all files
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        # Remove com.hytale.api.* imports
        sed -i '/^import com\.hytale\.api\./d' "$file"
        echo "  ✓ Cleaned imports: $file"
    fi
done

echo ""
echo "========================================="
echo "Backup complete: $BACKUP_DIR"
echo "Old SDK imports removed from ${#FILES[@]} files"
echo ""
echo "⚠️  NOTE: Files will not compile until proper UnsupportedOperationException"
echo "implementations are added. This requires manual review of each accessor's"
echo "interface contract and implementing proper exception messages."
echo ""
echo "Next steps:"
echo "1. Manually implement UnsupportedOperationException for each accessor method"
echo "2. Run: mvn clean compile"
echo "3. Fix remaining compilation errors"
echo "========================================="
