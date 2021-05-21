# Android

Android Device Requirements:

- Android 8.0 (API level 26) or above
- NFC enabled
- Camera for QR code scanning
- Internet connectivity

The Android app is acting as a bridge to connect a cold wallet with any web application by using the camera to capture a QR code and the NFC to interface with a cold wallet for transaction signing. The QR code comprises a raw transaction and a web service endpoint for the app to send the digital signature to.

Build and install the app on your phone using the [Android Studio]( https://developer.android.com/studio) via a USB cable (enable USB debugging on your phone).<br/>
*Tips: If you encountered the failure of "Failed to install the following Android SDK packages as some licences have not been accepted.", a quick fix is to navigate to the top menu: Tools > SDK Manager > SDK Tools (tab), tick the "Android SDK Command-line Tools" and follow the instruction to complete the installation, that should fix the failure.*

It is possible for an app to reach a locally hosted server (https://localhost:8443). Assuming the server and the Android Studio is running on a same machine, keep the USB cable connected and set up port forwarding using the Google Chrome [remote debugging](https://developer.chrome.com/docs/devtools/remote-debugging/local-server/) feature with the following configuration. 

![port-forward](https://github.com/Infineon/blockchain-wallet-and-webapp/blob/android/media/port-forward.png)

# License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.