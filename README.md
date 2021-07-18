# SustainKG API #

REST API server for communication between SustainKG [front end repository](https://github.com/greenguy33/sustainKG) and backend database server.

## Installation ##
A Scala project that can either be run locally through SBT (Scala Build Tool), or run in a docker container.

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
Copy `turboAPI.properties.template` to `turboAPI.properties`.  Update passwords as necessary.


## Build & Run ##

### Local Build & Run ###
```sh
$ cd sustainKG-API
$ sbt
> jetty:start
```

### Docker ###
#### Build
```
docker-compose build
```


#### Run
```
docker-compose up
```

This runs `sbt ~"jetty:start"` in the context of a docker container.  May take several minutes to compile.
