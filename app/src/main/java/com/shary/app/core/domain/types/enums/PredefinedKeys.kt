package com.shary.app.core.domain.types.enums

enum class PredefinedKey(val key: String) {
    // Datos personales
    FIRST_NAME("first-name"),
    LAST_NAME("last-name"),
    FULL_NAME("full-name"),
    NICKNAME("nickname"),
    GENDER("gender"),
    DATE_OF_BIRTH("date-of-birth"),
    NATIONALITY("nationality"),
    DNI("dni"),
    PASSPORT("passport"),
    TAX_ID("tax-id"),
    SOCIAL_SECURITY_NUMBER("social-security-number"),
    MARITAL_STATUS("marital-status"),

    // Contacto
    EMAIL("email"),
    PHONE("phone"),
    MOBILE("mobile"),
    ADDRESS("address"),
    ZIP_CODE("zip-code"),
    CITY("city"),
    STATE("state"),
    COUNTRY("country"),
    WEBSITE("website"),

    // Cuenta y acceso
    USERNAME("username"),
    PASSWORD("password"),
    TOKEN("token"),
    OTP("otp"),
    SECURITY_QUESTION("security-question"),
    SECURITY_ANSWER("security-answer"),
    AUTH_PROVIDER("auth-provider"),

    // Profesional / empresa
    COMPANY_NAME("company-name"),
    POSITION("position"),
    PROFESSION("profession"),
    BUSINESS_ID("business-id"),
    VAT_NUMBER("vat-number"),
    COMPANY_EMAIL("company-email"),
    DEPARTMENT("department"),
    OFFICE_LOCATION("office-location"),

    // Finanzas
    IBAN("iban"),
    SWIFT("swift"),
    BANK_NAME("bank-name"),
    BANK_ACCOUNT("bank-account"),
    CREDIT_CARD_NUMBER("credit-card-number"),
    EXPIRATION_DATE("expiration-date"),
    CVV("cvv"),
    PAYPAL_EMAIL("paypal-email"),
    CRYPTO_WALLET("crypto-wallet"),
    MONTHLY_INCOME("monthly-income"),
    EXPENSES("expenses"),

    // Vehículos
    VEHICLE_BRAND("vehicle-brand"),
    VEHICLE_MODEL("vehicle-model"),
    VEHICLE_PLATE("vehicle-plate"),
    LICENSE_NUMBER("license-number"),
    VEHICLE_YEAR("vehicle-year"),

    // Educación
    SCHOOL_NAME("school-name"),
    DEGREE("degree"),
    FIELD_OF_STUDY("field-of-study"),
    GRADUATION_YEAR("graduation-year"),
    STUDENT_ID("student-id"),

    // Redes sociales y apps
    INSTAGRAM("instagram"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    LINKEDIN("linkedin"),
    TIKTOK("tiktok"),
    DISCORD("discord"),
    GITHUB("github"),
    STACKOVERFLOW("stackoverflow"),
    TELEGRAM("telegram"),
    SLACK("slack"),
    SIGNAL("signal"),
    SPOTIFY("spotify"),
    NETFLIX("netflix"),
    APPLE_ID("apple-id"),
    GOOGLE_ACCOUNT("google-account"),

    // Archivos y referencias
    FILE_PATH("file-path"),
    FILE_NAME("file-name"),
    DOCUMENT_ID("document-id"),
    SPREADSHEET_LINK("spreadsheet-link"),
    DRIVE_FOLDER("drive-folder"),

    // Claves API y técnicas
    API_KEY("api-key"),
    CLIENT_ID("client-id"),
    CLIENT_SECRET("client-secret"),
    REFRESH_TOKEN("refresh-token"),
    ENDPOINT("endpoint"),
    SERVER_URL("server-url"),

    // Encuestas o preguntas
    QUESTION("question"),
    ANSWER("answer"),
    VOTE("vote"),
    COMMENT("comment"),
    SURVEY_ID("survey-id"),
    RESPONSE_DATE("response-date"),

    // Salud
    BLOOD_TYPE("blood-type"),
    ALLERGIES("allergies"),
    MEDICAL_CONDITION("medical-condition"),
    EMERGENCY_CONTACT("emergency-contact"),

    // Otros
    NOTES("notes"),
    LABEL("label"),
    TAGS("tags"),
    FAVORITE_COLOR("favorite-color"),
    HOBBY("hobby"),
    PET_NAME("pet-name");

    override fun toString(): String = key
}
