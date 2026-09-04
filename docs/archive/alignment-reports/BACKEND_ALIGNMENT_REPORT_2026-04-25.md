Implemented. The backend-alignment changes are now in code.

  What changed

  - Added mandatory request correlation header propagation (X-Request-Id) to cloud calls via headers builder:
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/Utils.kt
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/CloudServiceImpl.kt
  - Added contract constants (CLOUD_SCHEMA_VERSION, HEADER_X_REQUEST_ID):
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/Constants.kt
  - Added schema_version to upload payload model and ensured serialization emits defaults:
      - app/src/main/java/com/shary/app/core/domain/types/valueobjects/DataPayload.kt
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/CloudServiceImpl.kt
  - Reworked decision handling to parse structured ACK/error envelopes and map backend-coded failures:
      - ACK fields parsed: status, reason_code, decision_id, request_id, schema_version
      - Error fields parsed: error_code, message, request_id, retry_after_seconds
      - Handles unsupported_schema_version as non-retryable contract mismatch
      - Handles rate_limited/429 with retry metadata
      - Keeps /request_decision canonical + /payload_decision fallback (with explicit fallback logging)
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/CloudServiceImpl.kt
      - app/src/main/java/com/shary/app/infrastructure/services/cloud/CloudIdentityExceptions.kt
  - Updated inbox decision flows to stop swallowing backend failures and enforce cooldown on rate-limit:
      - app/src/main/java/com/shary/app/viewmodels/field/FieldViewModel.kt
      - app/src/main/java/com/shary/app/viewmodels/request/RequestViewModel.kt
  - Updated user-facing cloud error mapping for new backend-contract exceptions:
      - app/src/main/java/com/shary/app/ui/screens/utils/CloudErrorMessage.kt

  Tests added

  - app/src/test/java/com/shary/app/infrastructure/services/cloud/CloudUtilsTest.kt
  - app/src/test/java/com/shary/app/ui/screens/utils/CloudErrorMessageTest.kt
  - app/src/test/java/com/shary/app/core/domain/types/valueobjects/DataPayloadSerializationTest.kt

  Validation run

  - ./gradlew :app:compileDebugKotlin passed.
  - ./gradlew :app:testDebugUnitTest --tests "*CloudUtilsTest" --tests "*CloudErrorMessageTest" --tests "*DataPayloadSerializationTest" passed.