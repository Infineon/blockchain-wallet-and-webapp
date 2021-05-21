# Server

The server has 2 components:

- A web front-end (webpage) that is capable of generating blockchain payment transactions on client web browser without the involvement of any remote entity. The transaction will eventually be converted into a QR code, waiting to be signed.

- A back-end that takes care of business logics and interacts with Ethereum network (e.g., to broadcast transactions)

# Windows (preferred)

Go through the following steps to build and run the server:

- Download [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) and install it.

- Download the project.

- Open the project using IntelliJ IDEA. The software will resolve and download all project dependencies, wait for the process to complete (check the status at the bottom right).

- Before you can build the project, navigate to the top menu: File > Project Structure > Project Settings > Project. Click on the Project SDK dropdown menu and choose openjdk-16; if the item does not exist, choose Add SDK > Download JDK > Oracle OpenJDK (Version 16) > click on the download button. Now wait for the process to complete.

- To start the server, navigate to the top right: click on the Maven tab > click on the logo "m". Now type in `mvn spring-boot:run` to build and start the server.

- You can now access the website at https://localhost:8443. Log in with:
  - username: infineon
  - password: password

- To stop the server, navigate to the top menu: Run > click on the Stop button.

# Linux distribution - Ubuntu 

## Build

Install Maven:

```

$ sudo apt install maven

```

Download and Build the project:

```

$ git clone https://github.com/Infineon/blockchain-wallet-and-webapp -b server
$ cd blockchain-wallet-and-webapp
$ mvn package

```

JAR file location: `target/server-0.0.1-SNAPSHOT.jar`

## Run

Install openJDK:

```

$ sudo apt install openjdk-9-jre

```

Launch the server. You can now access the website at https://localhost:8443.

```

$ sudo java -jar server-0.0.1-SNAPSHOT.jar

```

Log in with:
- username: infineon
- password: password

# License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
