# AntiSplit M

Android app to merge/"AntiSplit" split APKs (APKS/XAPK/APKM) to a regular .APK file

This project is a simple GUI implementation of Merge utilities from [REAndroid APKEditor](https://github.com/REAndroid/APKEditor).

Some other apps that can perform this task like Apktool M, AntiSplit G2, NP Manager are all closed source. In addition, some older apps have a large problem in not removing the information about splits in the APK from the AndroidManifest.xml. If a merged/non-split APK contains this information it will cause an "App not installed" error on some devices. Fortunately the implementation by REAndroid fixes this issue.

### Note

All features of this app with better speed and other functions have been implemented in my new app <a href="https://github.com/AbdurazaaqMohammed/MP-Manager">MP Manager</a>

## Usage

Video - https://youtu.be/Vk566iMG6Gs

There are 3 ways to open a split APK to be merged:

- Share the file and select AntiSplit M in the share menu
- Press (open) the file and select AntiSplit M in available options
- Open the app from launcher and press the first button then select the split APK file.

There is also a menu in the app that allows selecting an app from those installed on the device as a split APK. Please try this method if you have problems with selecting a downloaded split APK.

Note: An APK must be signed in order to install it (unless you use tool like [Core Patch](https://github.com/LSPosed/CorePatch)). If you are planning to further modify the APK, you only need to sign it after the modifications (Apps like ReVanced Manager will sign it for you). Some apps verify the signature of the APK or take other measures to check if the app was modified, which may cause it to crash on startup.

## Screenshots

| Main screen                                 | Settings                               | Selecting from installed apps                                 |
| ------------------------------------------- | -------------------------------------- | ------------------------------------------------------------- |
| ![Main screen](screenshot/2.2.7_home.jpeg) | ![Settings](screenshot/2.2.7_settings.jpeg) | ![Selecting from installed apps](screenshot/2.2.7_applist.jpeg) |

| Dialog allowing splits selection   | Processing                                 | Result                             |
| ---------------------------------- | ------------------------------------------ | ---------------------------------- |
| ![Dialog](screenshot/2.2.7_splits_list.jpeg) | ![Processing](screenshot/2.2.7_processing.jpeg) | ![Result](screenshot/2.2.7_success.jpeg) |

## Used projects

⭐ [APKEditor](https://github.com/REAndroid/APKEditor) by REAndroid, what makes it all possible

- [Android port](https://github.com/MuntashirAkon/apksig-android) of apksig library by MuntashirAkon to sign APKs

## Permissions

- Storage permissions - to be able to save files to the same directory as a split APK (this is an option in the app, the storage permission will only be requested upon selecting it)
- QUERY_ALL_PACKAGES - to list apps installed on the device (see "Selecting from installed apps" in screenshots above)
- REQUEST_INSTALL_PACKAGES - to show an install button allowing prompt to install an app after merging it
- Internet permission - to check update for the app (can be disabled in settings)

Feel free to request a build of the app with any of these permissions removed.
