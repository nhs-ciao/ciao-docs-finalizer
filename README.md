# ciao-docs-finalizer

*CIP to detect completion of a document upload and handle any finalization*

## Introduction

The purpose of this CIP is to monitor an *in-progress* folder which contains events associated with a document upload (typically originating from [ciao-docs-parser](https://github.com/nhs-ciao/ciao-docs-parser)). The CIP maintains an internal state machine for each document upload, and once completion is detected (success or otherwise) a configurable action is performed.

`ciao-docs-finalizer` is built on top of [Apache Camel](http://camel.apache.org/) and [Spring Framework](http://projects.spring.io/spring-framework/), and can be run as a stand-alone Java application, or via [Docker](https://www.docker.com/).

Each application hosts a Camel [route](http://camel.apache.org/routes.html), following the structure:

> input folder (event files) -\> state-machine transition -\> *(on completion)*: run action

The details of the monitored folder and completion actions are specified at runtime through a combination of [ciao-configuration](https://github.com/nhs-ciao/ciao-utils) properties and Spring XML files.

**Supported completion actions:**
-	`MoveToCompletedFolder` - Moves the document's directory to the folder specified in the `./control/completed-folder` file
-	`MoveToErrorFolder` - Moves the document's directory to the folder specified in the `./control/error-folder` file

For more advanced usages, a custom action can be integrated by implementing the TransitionListener Java interface and providing a suitable spring XML configuration on the classpath.

**In-progress Folder:**
>	*The event files use a common naming format written by the CIPs involved in a document upload process. The contents and format of the event files is determined by the component writing the event and is not interpreted by this CIP.*

-	A detailed description of the in-progress folder and associated state machine is available in [docs/state-machine.md](docs/state-machine.md).

Configuration
-------------

For further details of how ciao-configuration and Spring XML interact, please see [ciao-core](https://github.com/nhs-ciao/ciao-core).

### Spring XML

On application start-up, a series of Spring Framework XML files are used to construct the core application objects. The created objects include the main Camel context, input/output components, routes and any intermediate processors.

The configuration is split into multiple XML files, each covering a separate area of the application. These files are selectively included at runtime via CIAO properties, allowing alternative technologies and/or implementations to be chosen. Each imported XML file can support a different set of CIAO properties.

The Spring XML files are loaded from the classpath under the [META-INF/spring](./src/main/resources/META-INF/spring) package.

**Core:**

-   `beans.xml` - The main configuration responsible for initialising properties, importing additional resources and starting Camel.

**Repositories:**

> An `IdempotentRepository' is configured to enable [multiple consumers](http://camel.apache.org competing-consumers.html) access the same folder concurrently.

- 'repository/memory.xml' - An in-memory implementation suitable for use when there is only a single consumer, or multiple-consumers are all contained within the same JVM instance.
- 'repository/hazelcast.xml' - A grid-based implementation backed by [Hazelcast](http://camel.apache.org/hazelcast-component.html). The component is hosted entirely within the JVM process and uses a combination of multicast and point-to-point networking to maintain a cross-server data grid.

**Processors:**

-   `processors/default.xml` - Creates a file poller for the in-progress folder and an associated state-machine to handle detected event transitions.

**Messaging:**

-   `messaging/activemq.xml` - Configures ActiveMQ as the JMS implementation for input/output queues.
-   `messaging/activemq-embedded.xml` - Configures an internal embedded ActiveMQ as the JMS implementation for input/output queues. *(For use during development/testing)*

### CIAO Properties

At runtime ciao-docs-finalizer uses the available CIAO properties to determine which Spring XML files to load, which Camel routes to create, and how individual routes and components should be wired.

**Camel Logging:**

-	`camel.log.mdc` - Enables/disables [Mapped Diagnostic Context](http://camel.apache.org/mdc-logging.html) in Camel. If enabled, additional Camel context properties will be made available to Log4J and Logstash. 
-	`camel.log.trace` - Enables/disables the [Tracer](http://camel.apache.org/tracer.html) interceptor for Camel routes.
-	`camel.log.debugStreams` - Enables/disables [debug logging of streaming messages](http://camel.apache.org/how-do-i-enable-streams-when-debug-logging-messages-in-camel.html) in Camel.

**Spring Configuration:**

-   `repositoryConfig` - Selects which repository configuration to load:
	`repositories/${repositoryConfig}.xml`

-   `processorConfig` - Selects which processor configuration to load:
    `processors/${processorConfig}.xml`

-   `messagingConfig` - Selects which messaging configuration to load:
    `messaging/${messagingConfig}.xml`

**Route Configuration:**
-   `inProgressFolderPollPeriod` - Time in millis between polling attempts on the in-progress folder.
-   `inProgressFolder` - The folder to monitor for document upload progress events.
-   `documentPreparationTimeout` - Maximum time in millis to wait for a document to complete the preparation stage (prior to sending) before raising a timeout event.
-   `documentSendTimeout` - Maximum time in millis to wait for confirmation  that the document has been sent before raising a timeout event.
-   `infResponseTimeout` - Maximum time in millis to wait for an infrastructure response before raising a timeout event.
-   `busResponseTimeout` - Maximum time in millis to wait for a business response before raising a timeout event.
-   `idempotentActions` - Boolean flag which selects if an idempotent checks should be performed before performing state transition actions. This is used to ensure that only one node in a cluster performs the action.
-   `actions` - Specifies which action to perform when a document upload transitions to a particular state. The format is one mapping per line, where a mapping has the form: `to={EVENT_NAME} > {ACTION_NAME}`.

**Default Processor​:**

>   The default processor configuration does not currently support any additional properties.

### Example
```INI
# Camel logging
camel.log.mdc=true
camel.log.trace=false
camel.log.debugStreams=false

# Select which processor config to use (via dynamic spring imports)
processorConfig=default

# Select which idempotent repository config to use (via dynamic spring imports)
repositoryConfig=hazelcast
# repositoryConfig=memory

# Hazelcast settings (if repositoryConfig=hazelcast)
hazelcast.group.name=ciao-docs-finalizer
hazelcast.group.password=ciao-docs-finalizer-pass
hazelcast.network.port=5701
hazelcast.network.join.multicast.group=224.2.2.3
hazelcast.network.join.multicast.port=54327

inProgressFolderPollPeriod=5000
inProgressFolder=./in-progress

documentPreparationTimeout=60000
documentSendTimeout=180000
infResponseTimeout=300000
busResponseTimeout=17280000

idempotentActions=true
actions=\
	to=SUCCEEDED > MoveToCompletedFolder	\n\
	to=FAILED > MoveToErrorFolder
```

Building and Running
--------------------

To pull down the code, run:

	git clone https://github.com/nhs-ciao/ciao-docs-finalizer.git
	
You can then compile the module via:

	mvn clean install -P bin-archive

This will compile a number of related modules - the main CIP module is `ciao-docs-finalizer`, and the full binary archive (with dependencies) can be found at `target\ciao-docs-finalizer-{version}-bin.zip`. To run the CIP, unpack this zip to a directory of your choosing and follow the instructions in the README.txt.

The CIP requires access to various file system directories and network ports (dependent on the selected configuration):

**etcd**:
 -  Connects to: `localhost:2379`

**ActiveMQ**:
 -  Connects to: `localhost:61616`

**Hazelcast**:
 -  Multicast discovery: `224.2.2.3:54327`
 -  Listens on: `*:5701` (If port is already taken, the port number is incremented until a free port is found)

**Filesystem**:
 -  If etcd is not available, CIAO properties will be loaded from: `~/.ciao/`
 -	The CIP will monitor event files in the folder specified by the `inProgressFolder` property.
