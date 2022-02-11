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
docker-compose build
```
#### Run
```
docker-compose up
```

This runs `sbt ~"jetty:start"` in the context of a docker container.  May take several minutes to compile.

## Commands

The API currently supports the following commands:

POST to `/getUserGraph`: accepts a username/password as a JSON formatted string and returns JSON graph data of that user's graph

Example body: 

```
{
    "user": "some_user",
    "password": "my_password"
}
```

POST to `/postUserGraph`: accepts a username and JSON graph data and overwrites the user's existing graph with the new graph in the database.

Example body:

```
{
    "user": "some_user",
    "nodes": [
        {
            "type": "node",
            "id": "0",
            "label": "Concept",
            "properties": {
                "name": "Innovation"
            },
	    "x":"23.975",
	    "y":"-46.876"
        },
        {
            "type": "node",
            "id": "1",
            "label": "Concept",
            "properties": {
                "name": "Globalization"
            },
	    "x":"63.209",
	    "y":"-49.292"
        }],
    "links": [
        {
            "type": "link",
            "id": "0",
            "label": "benefits",
            "source": "0",
            "target": "1",
            "citation": "www.mycitation.edu"
        }
    ]
}
```

POST to `/createNewUser`: accepts a username and password and creates a new user with an empty graph, if the username does not already exist. Note that a Code 200 will be returned if the request is accepted and a 204 code will be returned if the user already exists.

Example body:
```
{
    "user": "some_user_2",
    "password" : "my_password"
}
```

GET to `/getAllConcepts`: No body; returns a list of all concepts in the database in JSON format.

POST to `/getAllNodeConnections`: accepts a node name and returns all incoming and outgoing connections to that node in JSON format.

Example body:
```
{
    "node":"Apple"
}
```

GET to `/getGraphStatistics`: No body; returns a list of all user graphs as well as node and link counts.
