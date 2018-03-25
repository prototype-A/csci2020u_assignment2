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
`cd host`

Keep track of which Java processes you currently have running:  
`pgrep java`

**Either**:  
Launch the file host in the current terminal:  
`cd build/libs && java -jar host.jar`  
Then open another terminal in the current directory and run the rest of the commands in that window.  
**OR**  
Run the file host as a background process (logs will be logged to "./log.txt"):  
`./launch_host`

Navigate to the client's executable jar directory:  
`cd ../client/build/libs`

Run the client program:  
`java -jar client.jar`

Terminating the file host after you're done:  
`pgrep java`  
`kill <process>`  
where process is the number that was not listed before.
