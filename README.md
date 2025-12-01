# 📲 Shary App (Android Version)

Shary is a lightweight Android app for storing, managing, and securely sharing custom data fields between users. Designed with privacy and portability in mind, Shary enables encrypted exchange of structured data using usernames, shared secrets, and modern Android development practices.

---

## Features

- 🔐 (TODO) **Secure User Creation**: Generates RSA keypairs and hashed secrets during sign-up.
- 🧠 **Field Management**: Add, edit, and delete key-value data fields with aliases.
- 📤 **Data Sharing**: Share selected fields via:
  - Email
  - SMS
  - WhatsApp
  - Telegram
  - (TODO) Cloud based temporary sharing storage
- 🗂 **Requests Management**: Send field requests.
- 📁 **File Visualizer**: View other user's downloaded (JSON) files with dynamic tables.
- 🔄 (TODO) **Cloud Sync**: Store encrypted user profiles and shared data on Cloud.
- 📦 **Persistence**: Local encrypted storage using Proto DataStore.

---

## 🧱 Project Structure

<details>
<summary>Click to expand</summary>

```
app
│   MainActivity.kt
│
├───app
│       App.kt
│       AppFileProvider.java
│
├───controller
│       Controller.kt
│
├───core
│   │   Session.kt
│   │
│   ├───constants
│   │       Constants.kt
│   │
│   ├───dependencyContainer
│   │       DependencyContainer.kt
│   │
│   └───enums
│           StatusDataSentDb.kt
│
├───data
│   └───model
│           Field.kt
│           User.kt
│
├───datastore
│       FieldListDataStore.kt
│       FieldListSerializer.kt
│       UserDataStore.kt
│       UserListSerializer.kt
│
├───repositories
│   ├───impl
│   │       FieldRepositoryImpl.kt
│   │       UserRepositoryImpl.kt
│   │
│   └───interface
│           FieldRepository.kt
│           UserRepository.kt
│
├───security
│   │   CryptographyManager.kt
│   │   DeterministicRNG.kt
│   │   SecurityConstants.kt
│   │
│   └───securityUtils
│           SecurityUtils.kt
│
├───services
│   ├───cloud
│   │       CloudService.kt
│   │       Constants.kt
│   │       ICloudService.kt
│   │       Utils.kt
│   │
│   ├───email
│   │       Constants.kt
│   │       EmailService.kt
│   │
│   ├───field
│   │       FieldService.kt
│   │
│   ├───file
│   │       FileService.kt
│   │
│   ├───messaging
│   │       TelegramService.kt
│   │       WhatsAppService.kt
│   │
│   ├───requestField
│   │       RequestFieldService.kt
│   │
│   └───user
│           UserService.kt
│
├───ui
│   ├───screens
│   │   ├───commonConstants
│   │   ├───fields
│   │   │   │   FieldsScreen.kt
│   │   │   │
│   │   │   ├───constants
│   │   │   │       Constants.kt
│   │   │   │
│   │   │   └───utils
│   │   │           AddFieldDialog.kt
│   │   │           FieldRowSelectable.kt.disabled
│   │   │           SendFieldsDialog.kt
│   │   │
│   │   ├───fileVisualizer
│   │   │       FileVisualizerScreen.kt
│   │   │
│   │   ├───home
│   │   │   │   HomeScreen.kt
│   │   │   │
│   │   │   └───utils
│   │   │           AppNavigator.kt
│   │   │           Screen.kt
│   │   │           ShareFieldsGenericButton.kt
│   │   │           TopBar.kt
│   │   │
│   │   ├───login
│   │   │       LoginScreen.kt
│   │   │
│   │   ├───logup
│   │   │       LogupScreen.kt
│   │   │
│   │   ├───requests
│   │   │   │   RequestsScreen.kt
│   │   │   │
│   │   │   └───utils
│   │   │           AddRequestDialog.kt
│   │   │           SendRequestDialog.kt
│   │   │
│   │   ├───users
│   │   │       AddUserDialog.kt
│   │   │       UserRowSelectable.kt.disabled
│   │   │       UsersScreen.kt
│   │   │
│   │   └───utils
│   │           ComposableHider.kt
│   │           FieldsMatchingDialog.kt
│   │           FilterBox.kt
│   │           FunctionUtils.kt
│   │           GoBackHomeLayout.kt
│   │           ItemRow.kt
│   │           PasswordOutlinedTextField.kt
│   │           rowSearcher.kt
│   │           SelectableRow.kt
│   │
│   └───theme
│           Color.kt
│           Theme.kt
│           Type.kt
│
├───utils
│       BiometricAuthManager.kt
│       DateUtils.kt
│       UtilsFunctions.kt
│       ValidationUtils.kt
│
└───viewmodels
    │   ViewModelFactory.kt
    │
    ├───field
    │       FieldViewModel.kt
    │       FieldViewModelFactory.disabled
    │
    └───user
            UserViewModel.kt
```
</details>

---

## (TODO) 🔐 Security Notes

- RSA keys are generated on-device and on-the-fly.
- Shared secrets are never stored in plain text.
- Only encrypted payloads are transmitted to Firebase.
- Signature verification occurs in cloud functions before field delivery.

---

## 📦 Installation (Dev Build)

```bash
git clone https://github.com/Marlup/shary-android.git
cd shary-android
```

1. Open in **Android Studio Arctic Fox or later**.
2. Sync Gradle & Run on physical/emulated Android device.

---

## ✅ TODO

- [ ] End-to-end encryption between users
- [ ] QR-code or NFC shared secret transfer (WIP)
- [ ] Background sync support
- [ ] Field categories or grouping
- [ ] Export entire profile as encrypted file

---

## 🤝 Contributing

Contributions are welcome! Please follow standard GitHub flow:  
1. Fork  
2. Create feature branch  
3. PR with clear description

---

## 🧠 About

Shary was created as a privacy-respecting alternative for portable data exchange between users. It's ideal for minimalists, travelers, or anyone needing fast, offline-capable field sharing.
