# SustainKG API #

REST API server for communication between SustainKG [front end repository](https://github.com/greenguy33/sustainKG) and backend database server. This software connects to an instance of the database [Ontotext GraphDB](https://www.ontotext.com/products/graphdb/) where the data is stored.

## Installation ##
A Scala project that can be run locally through SBT (Scala Build Tool), as a precompiled .jar, or in a docker container.

### Requirements ###
#### Local
- SBT 
	- Minimum version is 1.1.4
		- [Official SBT Setup Guide](https://www.scala-sbt.org/release/docs/Setup.html)

#### Docker
- Docker
    - Most recent stable release, minimum version is 17.06.0
      - [Official Docker Website Getting Started](https://docs.docker.com/engine/getstarted/step_one/)
      - [Official Docker Installation for Windows](https://docs.docker.com/docker-for-windows/install/)
    - **Enable File Sharing**:  Configure file sharing in Docker for the directory or drive that will contain the source code ([Windows configuration](https://docs.docker.com/docker-for-windows/#file-sharing))
    - **Runtime Memory**: (Mac and Windows only) If using **Windows** or **Mac**, we recommend docker VM to be configured with at least 6GB of runtime memory ([Mac configuration](https://docs.docker.com/docker-for-mac/#advanced), [Windows configuration](https://docs.docker.com/docker-for-windows/#advanced)).  By default, docker VM on Windows or Mac starts with 2G runtime memory.

### Configuration ###
Copy `turboAPI.properties.template` to `turboAPI.properties`.  Update passwords and repository names as necessary.


## Build & Run ##

## From Precompiled .jar ##
SBT assembly can be used to create a precompiled jar file:
```
$ sbt
> assembly
```
The .jar can then be started with the command `java -jar [jar_name]`. It shoud lbe placed in the same directory as the .properties file. API will be accessible on port 8089.

### Local Build & Run ###
```sh
$ cd sustainKG-API
$ sbt
> jetty:start
```
API will be accessible on port 8080.

### Docker ###
#### Build
```
sudo docker build -t sustainkg-sbt .
```
#### Run
```
nohup sudo docker run --net sustainKGnetwork --ip 172.18.0.3 -p 8080:8080 --cap-add=NET_ADMIN sustainkg-sbt &> nohup_sustainkgsbt.out &
```

This runs `sbt ~"jetty:start"` in the context of a docker container.  May take several minutes to compile.

### Running with HTTPS
By default, HTTPS is enabled in the build.sbt file, which means you need to configure certificates. HTTPS can also be disabled by commenting the following two lines in build.sbt:

```
enablePlugins(JettyPlugin)
containerArgs := Seq("--config", "jetty.xml")
```

The port for HTTPS can be set in the file jetty.xml. This is also where the keystore information will be entered. To set up HTTPS, please follow the below steps which assume you already have a private key and certificate from a Certificate Authority. Make sure you keep the passwords that you set.

1. Make a keypair
```
openssl pkcs12 -export -in my_cert.cer -inkey my_key.key -out pkcs.p12 -name keypairout
```
2. Create the keystore and import the keypair
```
keytool -importkeystore -srckeystore sustainkg-keystore -destkeystore sustainkg-keystore -deststoretype pkcs12
```
3. Update jetty.xml with the correct information for the following fields: KeyStorePath, KeyStorePassword, KeyManagerPassword, TrustStorePath, TrustStorePassword
(note all paths/passwords can be the same that you set when you created the keystore)
