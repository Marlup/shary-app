# Tag Update Feature Implementation Summary

## Features Implemented

### 1. **Update Tag Dialog** (`UpdateTagDialog.kt`)
- New composable component based on `AddNewTagDialog`
- Allows users to edit tag name and color
- Pre-populates with current tag values
- Includes color picker integration

### 2. **Tag Usage Validation**

#### FieldViewModel Enhancement
**File**: `FieldViewModel.kt:98-104`

Added method to check if a tag is in use:
```kotlin
fun isTagInUse(tag: Tag): Boolean {
    return _fields.value.any { it.tag == tag }
}
```

This method checks if any field currently uses the specified tag.

### 3. **TagPicker Updates** (`TagPicker.kt`)

#### Added Dependencies:
- `FieldViewModel` injection for tag usage validation
- State management for update dialog and error messages

#### Edit Button Logic (line 84-92):
```kotlin
IconButton(onClick = {
    // Check if tag is in use before allowing edit
    if (fieldViewModel.isTagInUse(tag)) {
        errorMessage = "Cannot update tag '${tag.toTagString()}' - it is currently in use by fields"
    } else {
        tagToUpdate = tag
        showUpdateDialog = true
    }
}) { Icon(Icons.Default.Edit, "Edit") }
```

#### Delete Button Logic (line 93-100):
```kotlin
IconButton(onClick = {
    // Check if tag is in use before allowing delete
    if (fieldViewModel.isTagInUse(tag)) {
        errorMessage = "Cannot delete tag '${tag.toTagString()}' - it is currently in use by fields"
    } else {
        tagViewModel.removeTag(tag.toTagString())
    }
}) { Icon(Icons.Default.Delete, "Delete") }
```

#### UpdateTagDialog Integration (line 133-152):
- Shows dialog when Edit button is clicked (if tag is not in use)
- Pre-fills current tag name and color
- Calls `tagViewModel.updateTag()` on confirmation
- Updates selected tag if it was the one being edited
- Properly cleans up state on dismiss

#### Error Dialog (line 154-166):
- Displays user-friendly error message
- Prevents tag modification when in use
- Clear explanation of why the operation failed

## Validation Rules

### Tag Cannot Be Updated If:
- Any field in the database currently uses that tag
- Shows error: "Cannot update tag '[tag name]' - it is currently in use by fields"

### Tag Cannot Be Deleted If:
- Any field in the database currently uses that tag
- Shows error: "Cannot delete tag '[tag name]' - it is currently in use by fields"

## User Experience Flow

### Updating a Tag:
1. User clicks Edit icon next to a tag in the TagPicker
2. System checks if tag is in use
3. **If in use**: Error dialog appears explaining tag cannot be updated
4. **If not in use**: UpdateTagDialog opens with current values
5. User modifies name and/or color
6. User clicks "Update"
7. Tag is updated in repository
8. If the tag was currently selected, the selection is updated
9. Dialog closes

### Deleting a Tag:
1. User clicks Delete icon next to a tag in the TagPicker
2. System checks if tag is in use
3. **If in use**: Error dialog appears explaining tag cannot be deleted
4. **If not in use**: Tag is immediately deleted from repository

## Files Created
1. `UpdateTagDialog.kt` - New dialog component for updating tags

## Files Modified
1. `TagPicker.kt` - Added edit functionality, validation, and error handling
2. `FieldViewModel.kt` - Added `isTagInUse()` validation method
3. `RequestScreen.kt` - Fixed import for RequestEvent (unrelated build fix)

## Benefits

### Data Integrity:
- Prevents accidental modification/deletion of tags in active use
- Maintains referential integrity between fields and tags
- Prevents orphaned tag references

### User Experience:
- Clear, immediate feedback when operations are not allowed
- Intuitive edit dialog matching the create dialog pattern
- Prevents data loss from unintended tag modifications

### Developer Experience:
- Reusable validation logic in FieldViewModel
- Consistent dialog patterns (Add vs Update)
- Clean separation of concerns

## Testing Notes

To test the validation:
1. Create a tag
2. Assign it to a field
3. Try to edit the tag → Should show error
4. Try to delete the tag → Should show error
5. Remove the tag from all fields
6. Try to edit/delete again → Should work

## Future Enhancements

Possible improvements:
1. Batch tag update: Update tag name/color and automatically update all fields using it
2. Tag replacement: Replace all instances of Tag A with Tag B before deletion
3. Show count of fields using a tag in the error message
4. Confirmation dialog before deleting unused tags
