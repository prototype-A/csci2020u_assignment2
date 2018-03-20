# File Sharer

<p>CSCI 2020U Assignment 2</p>

<p>Share files over the network</p>


## How to Compile

### Requisites
* Repo cloned/downloaded as zip and extracted
* Java is installed (Tested using OpenJDK 8)
* Gradle is installed (Tested using Gradle 4.4.1)

### Commands

In the cloned/downloaded repo's directory:  

Compile the host:  
`gradle buildhost`

Compile the client:  
`gradle buildclient`

Navigate to the host's executable jar directory:  
`cd host/build/libs`

Run the host program:  
`java -jar host.jar`

Navigate to the client's executable jar directory:  
`cd ../../../client/build/libs`

Run the client program:  
`java -jar client.jar`
