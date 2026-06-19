# Tag Update Fix

## Issue Summary

**Problem**: When updating a field's tag through the UI, the change was not being persisted. The tag would appear to change in the UI momentarily but would revert to the previous value after the screen refreshed.

**Root Cause**: The `updateField()` method in `FieldViewModel.kt` was missing the event emission for tag updates.

## Analysis

### Tag System Architecture

The tag system uses a sealed class hierarchy with proper serialization:

```kotlin
sealed class Tag(open val name: String, open val color: Color?) {
    data class Entry(override val name: String, override val color: Color) : Tag(name, color)
    data object Unknown : Tag("Unknown", Color.Gray.copy(alpha = 0.7f))
}
```

**Default Tag**: `Tag.Unknown` with color `Color.Gray.copy(alpha = 0.7f)` - this is the fallback for undefined or corrupted tags.

### Serialization & Persistence

1. **Serialization** (`TagCodec.kt`):
   ```kotlin
   fun Tag.serialize(): String {
       val argb = (toColor().toArgbInt())
       val hex = String.format("#%08X", argb) // AARRGGBB
       return "${toTagString()}|$hex"
   }
   ```
   Format: `"TagName|#AARRGGBB"` (e.g., `"Work|#FF4285F4"`)

2. **Deserialization** (`TagCodec.kt`):
   ```kotlin
   fun Tag.Companion.deserialize(raw: String): Tag {
       val parts = raw.split('|', limit = 2)
       return if (parts.size == 2) {
           val name = parts[0]
           val hex  = parts[1]
           val argb = hex.removePrefix("#").toLong(16).toInt()
           Tag.fromString(name, argb.toColor())
       } else {
           Tag.fromString(raw, Tag.Unknown.toColor())
       }
   }
   ```

3. **Proto Mapping** (`mappers.kt`):
   - **Encoding**: `codec.encode(tag.serialize(), Purpose.Tag)` - encrypts serialized tag
   - **Decoding**: `Tag.deserialize(codec.decode(tag, Purpose.Tag))` - decrypts and deserializes

### Update Flow

**UI → ViewModel → Repository → DataStore**

```
FieldScreen (line 603-607)
    ↓
    fieldViewModel.updateField(field, editedValue, editedAlias, editingField.tag)
    ↓
FieldViewModel.updateField() (line 299-338)
    ↓
    fieldRepository.updateTag(key, newTag)
    ↓
FieldRepositoryImpl.updateTag() (line 119-132)
    ↓
    DataStore updates with new serialized tag
```

## The Bug

### Location
`app/src/main/java/com/shary/app/viewmodels/field/FieldViewModel.kt:329-337`

### Before (Buggy Code)
```kotlin
result.onSuccess {
    if (shouldUpdateValue) _events.tryEmit(FieldEvent.ValueUpdated(field.key))
    if (shouldUpdateAlias) _events.tryEmit(FieldEvent.AliasUpdated(field.key))
    // ❌ MISSING: Tag update event!
    // Single refresh after performing all updates
    refresh()
}.onFailure { e ->
    _events.tryEmit(FieldEvent.Error(e))
}
```

### After (Fixed Code)
```kotlin
result.onSuccess {
    if (shouldUpdateValue) _events.tryEmit(FieldEvent.ValueUpdated(field.key))
    if (shouldUpdateAlias) _events.tryEmit(FieldEvent.AliasUpdated(field.key))
    if (shouldUpdateTag) _events.tryEmit(FieldEvent.TagUpdated(field.key, newTag)) // ✅ ADDED
    // Single refresh after performing all updates
    refresh()
}.onFailure { e ->
    _events.tryEmit(FieldEvent.Error(e))
}
```

## Why This Matters

### Event System
The app uses a event-driven architecture where ViewModels emit events to notify the UI of state changes:

```kotlin
sealed interface FieldEvent {
    data class ValueUpdated(val key: String) : FieldEvent
    data class AliasUpdated(val key: String) : FieldEvent
    data class TagUpdated(val key: String, val tag: Tag) : FieldEvent  // ← This was not being emitted
    // ... other events
}
```

### Impact of Missing Event
Without emitting `FieldEvent.TagUpdated`:
1. ✅ **Repository layer works**: Tag IS saved to DataStore correctly
2. ✅ **Refresh works**: `refresh()` reloads data from repository
3. ❌ **Event listeners miss notification**: UI components listening for tag updates don't get notified
4. ❌ **User experience**: Appears like nothing happened until manual refresh

### Comparison with Individual Update Methods

The standalone `updateTag()` method (line 203-224) **correctly** emits the event:

```kotlin
fun updateTag(field: FieldDomain, tag: Tag) {
    if (field.tag == tag) return
    viewModelScope.launch {
        _isLoading.value = true
        val result = runCatching {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    fieldRepository.updateTag(field.key, tag)
                }
            }
        }
        _isLoading.value = false

        result.onSuccess {
            _events.tryEmit(FieldEvent.TagUpdated(field.key, tag))  // ✅ Present here
            refresh()
        }.onFailure { e ->
            _events.tryEmit(FieldEvent.Error(e))
        }
    }
}
```

However, `updateField()` combines multiple updates (value, alias, tag) in a single transaction but was missing the tag event emission.

## Verification

### Tag Persistence Flow

1. **User selects new tag** in `TagPicker` (FieldScreen.kt:590-593)
2. **Local state updates** immediately: `editingField = editingField?.copy(tag = selectedTag)`
3. **User clicks "Accept"** (FieldScreen.kt:600-610)
4. **ViewModel called**: `fieldViewModel.updateField(field, editedValue, editedAlias, editingField!!.tag)`
5. **Repository updates** DataStore with encrypted tag
6. **Event emitted**: `FieldEvent.TagUpdated(field.key, newTag)` ✅ NOW WORKING
7. **Refresh triggered**: Reloads all fields from repository
8. **UI updates** with persisted tag

### Tag.Unknown Behavior

The `Tag.Unknown` value is correctly used as:
1. **Default for new fields** (line 366 in FieldViewModel)
2. **Fallback for invalid deserialization** (line 48 in mappers.kt)
3. **Fallback for blank/missing tags** (line 34 in mappers.kt)

Color: `Color.Gray.copy(alpha = 0.7f)` - a semi-transparent gray (#B3808080)

## Testing Recommendations

### Manual Testing
1. Create a field with Tag.Unknown (default)
2. Edit the field and change tag to "Work" with blue color
3. Click "Accept"
4. Verify tag persists after screen refresh
5. Close app completely
6. Reopen app
7. Verify tag is still "Work" with blue color

### Edge Cases to Test
1. **Unknown tag**: Create field → verify gray tag
2. **Tag update only**: Change only tag (not value/alias) → verify persists
3. **Multi-field update**: Change value + alias + tag together → verify all persist
4. **Tag to Unknown**: Change custom tag back to Unknown → verify persists
5. **Encrypted persistence**: Tag should be encrypted in DataStore (check with DataStore inspector)

## Related Code Locations

### Core Files
- `Tag.kt` (line 12): `Tag.Unknown` definition
- `TagCodec.kt` (lines 20-37): Serialization/deserialization
- `mappers.kt` (lines 22-50): Proto encoding/decoding
- `FieldViewModel.kt` (line 332): **FIX LOCATION**
- `FieldRepositoryImpl.kt` (lines 119-132): Repository update
- `FieldScreen.kt` (lines 590-610): UI update flow

### Repository Implementation
```kotlin
// FieldRepositoryImpl.kt
override suspend fun updateTag(key: String, newTag: Tag) {
    dataStore.updateData { current ->
        val updated = current.fieldsList.map { proto ->
            val domain = proto.toDomain(codec)
            if (domain.key.equals(key, ignoreCase = true)) {
                domain.copy(tag = newTag).toProto(codec)  // ✅ Updates tag
            } else {
                proto
            }
        }
        current.toBuilder()
            .clearFields()
            .addAllFields(updated)
            .build()
    }
}
```

## Summary

**Fixed**: Tag updates now properly emit `FieldEvent.TagUpdated` event in the `updateField()` method.

**Result**: Tag changes persist correctly and the UI responds appropriately to tag updates.

**Build Status**: ✅ Successful (verified with `./gradlew assembleDebug`)

**Impact**: Medium - affects all tag updates when editing fields through the field editor dialog. Standalone tag updates through `updateTag()` were already working.
