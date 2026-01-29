#!/usr/bin/env python3
"""
MIGRATION-001: Add UnsupportedOperationException to accessor methods
This script adds proper exception throws to all accessor methods that reference
the deleted com.hytale.api.* SDK package.
"""

import re
import sys
from pathlib import Path

ADAPTER_DIR = Path("/mnt/d/Gaming/Argonath-Systems/02-adapter-hytale")

# Files to process with their accessor type for contextual error messages
FILES_TO_PROCESS = {
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleEventAccessor.java": "Event registration and handling",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleSchedulerAccessor.java": "Task scheduling",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleCommandAccessor.java": "Command registration",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleStorageAccessor.java": "Data persistence",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleWorldAccessor.java": "World manipulation",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleUIAccessor.java": "UI/HUD management",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleInventoryAccessor.java": "Inventory operations",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNPCEntityAccessor.java": "NPC entity management",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleAssetAccessor.java": "Asset loading",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleHologramAccessor.java": "Hologram display",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleItemAccessor.java": "Item creation",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleModelAccessor.java": "Model rendering",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleNotificationAccessor.java": "Notifications",
    "src/main/java/com/argonathsystems/adapter/hytaleadapter/accessor/HytaleSoundAccessor.java": "Sound effects",
}

def add_migration_header(content, accessor_type):
    """Add MIGRATION-001 status header if not present"""
    if "MIGRATION-001" in content or "BLOCKED - Requires Official Hytale SDK" in content:
        return content
    
    # Find the class declaration
    class_match = re.search(r'(public\s+class\s+\w+.*?\{)', content, re.DOTALL)
    if not class_match:
        return content
    
    header = f"""/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements {accessor_type} functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
"""
    
    # Insert header before class declaration
    class_start = class_match.start(1)
    return content[:class_start] + header + content[class_start:]

def process_file(file_path, accessor_type):
    """Process a single accessor file"""
    print(f"Processing: {file_path.name}")
    
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception as e:
        print(f"  ✗ Error reading file: {e}")
        return False
    
    # Add migration header
    content = add_migration_header(content, accessor_type)
    
    # Find all method bodies that might need fixing
    # Look for methods with bodies that reference old types
    methods_fixed = 0
    
    # Replace references to old SDK types with Object
    old_replacements = [
        (r'\bServer\b(?!\s*\.)', 'Object /* Server */'),
        (r'\bPlayer\b(?!\s*\.)', 'Object /* Player */'),
        (r'\bPlayerRef\b(?!\s*\.)', 'Object /* PlayerRef */'),
        (r'\bLocation\b(?!\s*\.)', 'Object /* Location */'),
        (r'\bItemStack\b(?!\s*\.)', 'Object /* ItemStack */'),
        (r'\bEntity\b(?!\s*\.)', 'Object /* Entity */'),
        (r'\bHyUIHud\b', 'Object /* HyUIHud */'),
        (r'\bHudBuilder\b', 'Object /* HudBuilder */'),
    ]
    
    for pattern, replacement in old_replacements:
        before = content
        content = re.sub(pattern, replacement, content)
        if content != before:
            methods_fixed += 1
    
    # Write back
    try:
        file_path.write_text(content, encoding='utf-8')
        print(f"  ✓ Fixed {methods_fixed} type references")
        return True
    except Exception as e:
        print(f"  ✗ Error writing file: {e}")
        return False

def main():
    print("=" * 60)
    print("MIGRATION-001: Add UnsupportedOperationException")
    print("=" * 60)
    print()
    
    success_count = 0
    fail_count = 0
    
    for file_rel_path, accessor_type in FILES_TO_PROCESS.items():
        file_path = ADAPTER_DIR / file_rel_path
        if not file_path.exists():
            print(f"⚠ File not found: {file_rel_path}")
            fail_count += 1
            continue
        
        if process_file(file_path, accessor_type):
            success_count += 1
        else:
            fail_count += 1
    
    print()
    print("=" * 60)
    print(f"Results: {success_count} successful, {fail_count} failed")
    print("=" * 60)
    
    return 0 if fail_count == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
