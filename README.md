# DEPRECATED

**This repository is deprecated and will no longer be maintained. The preferred protocol is [WalletConnect](https://walletconnect.com/). Please refer to https://github.com/Infineon/secora-blockchain-walletconnect.**

# Bridging Hardware Wallet and Web Application

This repository contains an Android app and a web server to showcase the interaction of hardware wallets (commonly known as a cold wallet) and Ethereum network. The cold wallet being used here is an Infineon Blockchain contactless card ([Blockchain Security 2Go Starterkit](https://www.infineon.com/cms/en/product/evaluation-boards/blockchainstartkit/) or [SECORA™ Blockchain](https://www.infineon.com/blockchain)). To learn about the features of Blockchain Security 2Go Starterkit, please visit [here](https://github.com/Infineon/Blockchain/tree/master/doc).

On one hand, a cold wallet provides a safe “vault” to protect blockchain keys. On the other hand, having the notoriety that greater the security means lesser the usability. This is due to the nature of a cold wallet, a separated piece of security-focused hardware with limited connectivity.

To improve the usability of a cold wallet and to unlock its potential, an Android app has been developed to address the limitation on connectivity. The app enables a cold wallet to connect with any web-based application digitally via QR code. With this capability, a transaction can be generated and signed wholly at clients' end seamlessly.

# Repository Directions
- Android repo, please switch to [android](https://github.com/Infineon/blockchain-wallet-and-webapp/tree/android) branch.
- Server repo, please switch to [server](https://github.com/Infineon/blockchain-wallet-and-webapp/tree/server) branch.

# Demo Video

![demo](https://github.com/Infineon/blockchain-wallet-and-webapp/blob/master/media/demo.gif)

# License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.