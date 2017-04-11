[workflow]: img/workflow.png "padfs workflow"
[database]: img/databaseSchema.png "padfs database"

Authors
======
* Fabio Lucattini
* Matteo Mazza

Introduction
======

PADFS is a distributed, multi-tenant, persistent, hierarchical file system.
The user’s files stored in the file system can be organized in a hierarchical way through directories. Moreover the users can share their folders and files with one or more users specifying, for each user, a particular permission.
The service is exposed to the users as a REST web service and through a web interface.

Workflow
======
![padfs project workflow][workflow]

Project structure
======

The project is organized in 4 main packages:
1. **padfsThreads**

 * Padfs: Responsible of booting the system.

 * HeartBit: It monitor the PADFS network to maintain an updated network status.

 * Shutdown: It intercept the system interrupt to gracefully shutdown the node.

 * GarbageCollector: It monitors the PADFS local data and remove the no more required data.

 * Rest Interface: The Rest Interface component lets the interaction from the outside with the PADFS-node. It implements a server listening for incoming rest-messages.

 * PrepareOp: The PrepareOp component continuously reads job-operations from the queue, execute some preliminary steps and finally send forward the job-operation.

 * Consensus: The consensus component continuously reads initialized job-operations through green socket and it tries to reach an agreement between other PADFS-nodes to finalize them. Moreover it continuously reads consensus-messages through red socket to complete the consensus algorithm and to receive agreed job-operations to be finalized.

 * CompleteOp: The CompleteOp component continuously read job-operations through yellow socket and executes their final steps.

 * Garbage Collector: The Garbage Collector component periodically scans the local file-system to discover no more needed data that can be deleted.

 * Heartbit: The Heartbit component periodically queries other PADFS-nodes to maintain updated the network status.

 * File Manager: The File Manager component periodically scans the PADFS-nodes to check and to maintain available the replicas of the data managed by its PADFS-node.

 * FileManager: It monitors the PADFS network to guarantee the replication of the user data.

 * Consensus: It communicates with the other PADFS-nodes to agree on the proposed operations.

 * CompleteOp: It executes the finalization phases of all operations.

 * PrepareOp: It executes the initialization phases of all operations.

2. **jobManagement**: This package contains the classes representing PADFS operations. At the arrival of a message from the net, the rest interface create one or more instances of this classes.
These classes can be classified into 2 sub-packages:
 * consensus: The classes instances of this package are created in the RestInterface component and reach the Consensus component through red sockets described in the workflow paragraph.
 The jobManagement.consensus instances are:
    * Prepare: Manage the receive of Prepare messages and send the Reply messages to the server that has started the new consensus request.
    * Propose: Manage the receive of Propose messages.
    * Accept: Manage the receive of Accept messages.

 * jobOperation: The classes instances of this package are created in the RestInterface component and reach the PrepareOp component through the blue sockets described in the workflow paragraph. The PrepareOp thread, for each jobOperation read, computes the preliminary operations defined by the jobOperation itself and forwards it to the Consensus component. The Consensus thread transmits the operation to all the involved PADFS-nodes in a jobManagement.consensus.Prepare object and it agrees on the execution of the jobOperation. Then the jobOperation reaches the CompleteOp component and its execution can be completed.
 The jobOperations are subdivided in 3 sub-packages:
    * clientOp: It contains all the operations that a client can request.
    * manageOp: It contains all the internal operations used to maintain the integrity of the PADFS-node.
    * serverOp: It contains all the operations that a PADFS-node can request to other PADFS-nodes.

3. **system**: This package contains the classes utilized to define components that are used by other packages:
  * consensus: it maintains the status variables necessary to the consensus component.
  * containers: it defines the internal data structure utilized in the system. For instance User.java contains all the information needed to define a user like username and password.
  * logger: it implements the logging system.
  * managementOp: it includes all the system routines necessary to maintain the PADFS shared status.
  * managers: it implements all the interfaces that the system use to interact with external component like DBMS, local file system and configuration file.
  * merkeTree: it contains the metadata contained in the distributed file system.

4. **restInterface**: This package contains the classes that defines the rest interface. It includes the wrappers needed to represent JSON network messages as java objects. They are subdivided in the sub-packages:
  * consensus
  * manageOp
  * op


Consistency model
======
The PADFS consistency model is the sequential consistency.
The execution of a sequence of operations is sequentially consistent if the results of the execution is the same as if the operations were executed in some sequential order, and the operations of each individual node appear in this sequence in the order specified by the node.

In PADFS, each operation done on the same file is sequentialized because that operations are performed inside the same consensusGroup that approves one operation at the time. Thus each PADFS-node belonging to the consensusGroup, executes the approved operations in the same order.

Data replication
======
PADFS eventually leads to store 3 copies of each file. The replication of the data is achieved creating 3 copies of each file stored with a PUT operation. Moreover, for each file there are 3 PADFS-nodes designated to periodically check the availability of the copies. This PADFS-nodes will create new copies in case of failures or network partitions attempting to maintain 3 available copies in each network partition. If more than 3 copies are present in the network, PADFS will eventually delete the excess copies.

Availability and Partition Tolerance
======
PADFS is partition tolerant, it continues to operate also in highly unreliable network.
In order to grant the consistency model, it does not provide a 100% availability.
From a client perspective, the main operations on PADFS are uploading and downloading files and these are the requirements for their availability:

* Upload

  The upload operation is available if the client is in the network-subset that contains the majority (at least 50%+1) of PADFS-nodes that are involved in the execution of the consensus problem for this operation.

* Download

  The download operation is available if the client is in a network-subset in which there is at least one available PADFS-node that manages the file and at least one available PADFS-node that manage the metadata of that file.

Load balance
======
Load balance is implemented as a distribution of: data, metadata and computation among the PADFS-nodes.

The uploaded data by the users and their replicas are distributed among the PADFS-nodes. Our goal is to maintain the same percentage of disk occupancy on each node, siìo the uploaded data is stored in the server with fewer disk occupancy.

Database structure
======

PADFS utilizes a database, together to the local file system, to store all the data and metadata necessary to maintain the client files and the PADFS status.
The data is partially shared with all the PADFS-nodes.
The tables in the db are:
* users
* servers
* filesManaged
* directoryListing
* filesHosted
* tmpFiles
* consensusGroups

![padfs project database][database]
