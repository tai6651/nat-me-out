# NAT me out

TCP (and UDP) port exposer for service behind NAT network.
This tools will help to fix the issue that some service is running behind NAT or corporate proxy.


## Component
This application come with 2 operation mode
1) public side - This is to run the server in publicly accessible network
2) nat side - This is to run server in NAT(ed) network.

Issue of service behind NAT is that it can't be access from public internet.
This tool will act like tunnel to expose service on different network location.


## Getting Started

Follow the below instructions to build, test and run the project on your local machine.

### Prerequisites
    
The followings is needed to build, test and run the project.


- [Oracle's JDK 11.0.5](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)
- [Git](https://git-scm.com/downloads)
- [Maven 3.6.2 or later](https://maven.apache.org/download.cgi)


## Clone

Get started by cloning this repository using git command.


```
git clone https://github.com/tai6651/nat-me-out.git
```


## Build jar file

And build the project jar file using the following maven command.

```
mvn package 
```


## Run

Run project using the following command to run server for public side

```
java -jar target/<projectname-version>.jar public
```

or using the following command to run server for NAT side

```
java -jar target/<projectname-version>.jar nat
```


## Configuration Reference
| Option                                                          | Description                                                                                       |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| publicSide.tcp[].publicSidePort                                 | TCP Port to listen for public side                                                                |

## Command line argument
| Option                                                          | Description                                                                                       |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| -mode                                                           | Operation mode choose between nat, or public                                                      |


## License

This project is licensed under the Apache License, Version 2.0


