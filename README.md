# Routing Packets over a Distributed P2P Network

##### CS455 - Distributed Systems - ASG1

##### Maxwell You

## Program Overview

MessagingNodes will connect to a Registry which will send routing information to each MessagingNode in the system.
The Registry informs each node to send **N<sub>r</sub>** messages to every other node in the system.
The MessagingNodes will use their routing information to make decisions about where to forward packets.
After all nodes have finished sending messages, the Registry will request statistics from the MessagingNodes.
The Registry will present this information in an organized table to allow the user to assess the program's 
correctness in forwarding packets.


## File Descriptions (by grouping):
### **node**
  - **MessagingNode**: Connects to a registry and receives routing table from it that specifies where it routes packets to.
            Will process commands from the Registry such as initiation of packet sending and collection of traffic summary.

  - **Registry**: Accepts registrations from MessagingNodes and constructs routing tables for all of them.
    Confirms that each node has set up connections with the nodes in their routing table successfully.
    Sends task initiation on user request and traffic summary requests when it confirms all nodes are done
    sending messages.

### **routing**
  - **RoutingTable**: Basically a HashMap that stores ID, IP:port pairs. Associates each node ID with the IP
        and port they are listening on. This table is sent to the nodes which allows them to connect to and
        later make routing decisions based on the nodes in their routing table.

### **transport**
  - **TCPConnection**: Holds references to a connection's socket and sender and receiver threads. This allows
        a clean implementation of 1 sender and receiver thread per connection instead of having one per message.

  - **TCPConnectionsCache**: A HashMap that stores IP:port, TCPConnection pairs. Allows easy lookup of the
        connection to an IP:port.

  - **TCPReceiverThread**: Handles the receipt of messages in a separate thread so the main thread of the
        Registry/MessagingNode can accept commands from the user. Deserializes the received message and carries
        out the appropriate actions based on the message contents. Sometimes the actions will involve sending
        messages to other nodes, which it will do by accessing the SenderThread associated with the connection
        it needs to send to.

  - **TCPSenderThread**: Handles sending of messages from the nodes. Has a message queue that buffers the messages
        to be sent. Appends a message length to the serialized data so the receiver knows how many bytes to read.

  - **TCPServerThread**: - Accepts connections and spawns a new socket for communications to take place. Spins up
        a TCPConnection and starts the sender and receiver threads of the new connection.

### **util**
  - **InteractiveCommandParser**: Processes the commands the user inputs. Allows the user to control the sending
        of packets and print useful info such as the list of nodes in the system, routing tables, and traffic diagnostics.

  - **StatisticsCollectorAndDisplay**: Collects the traffic summaries from all the nodes and prints them in a readable table.

### **wireformats**
  - **Event**: An interface that specifies that every message must have a type and marshalling method.

  - **EventFactory**: Unmarshalls messages based on their type. Returns an Event object of the specified
        message type.

  - **Node**: An interface that specifies that the nodes in the system must have a way to handle receipt of messages.
        The Registry/MessagingNode have a switch statement that calls an appropriate method based on the received
        message type.

    ### Messages MessagingNodes send to the Registry:

      - **OverlayNodeSendsRegistration**: Sends a registration request to the registry.

      - **OverlayNodeSendsDeregistration**: Sends a deregistration requst to the registry.

      - **NodeReportsOverlaySetupStatus**: Informs registry if it established connections with nodes in its
        routing table successfully or not.

      - **OverlayNodeSendsData**: Sends packet to a random MessagingNode in the system with a payload of a random
        integer. MessagingNodes that are not the src or dst will consult their routing tables to find the closest
        MessagingNode to forward the packet to.

      - **OverlayNodeReportsTaskFinished**: Informs registry when it is dont sending all messages. However, this does
        not confirm that all the messages have been received by the dst MessagingNodes yet. Messages could still
        be in transit by the time the registry receives this confirmation.

      - **OverlayNodeReportsTrafficSummary**: Sends the number of packets this node has sent, received, and relayed.
        The summations of the sent and received payloads are also sent

    ### Messages Registry sends to the MessagingNodes:

      - **RegistryReportsRegistrationStatus**: Checks if the registration request from the MessagingNode is valid by
        checking if its IP:port has aleady been registered and if the IP in the message matches the IP of the
        socket it sent the message on. Assigns a random unique ID to the MessagingNode between 0-127 (inclusive).

      - **RegistryReportsDeregistrationStatus**: Does the same as above except checks opposite conditions.

      - **RegistrySendsNodeManifest**: Constructs the routing table for all registered MessagingNodes. The entries
        in the table are 1, 2, 4, ..., 2^(N-1) hops away where N := number of routing table entries and hops are
        defined as the next ID in the list of registered nodes and the ID space wraps around
        e.g. nodes 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        routing table for 3: 4, 5, 7

      - **RegistryRequestsTaskInitiate**: When the user types 'start number-of-messages', this message will be sent
        to all the registered MessagingNodes telling them to send 'number-of-messages'.

      - **RegistryRequestsTrafficSummary**: When all nodes are done sending messages, the registry will send this
        message to all of the MessagingNodes. It will print the counters and trackers of each node and a
        cumulative summary of the counters and trackers.

### **Disclaimer**
My program does not guarantee correct functionality if registration/deregistration happens after the ***setup-overlay***
command has occurred. Multiple ***start number-of-messages*** commands can be run without having to restart the program, 
but if additional registrations occur in between ***start*** commands, the outcome of the program will likely crash. This
is because these registrations/deregistrations are occurring after the overlay has already been setup.
