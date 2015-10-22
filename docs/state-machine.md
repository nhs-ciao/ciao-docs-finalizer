# Document Upload State Machine

[ciao-docs-finalizer](../README.md) tracks the ongoing state of a document upload process via an internal state machine. The state machine is updated by monitoring an *in-progress folder* for event files which in turn trigger an event transition.

The event files are stored in the in-progress directory by other CIPs involved in the upload progress. The contents/format of the event files are determined by the CIP doing the writing, however the directory structure and naming of the event files is common across all CIPs.

## Structure of the In-Progress Folder 

The root folder contains multiple sub-folder, where each sub-folder corresponds to an on-going document upload process.

The name of the sub-folder matches the unique id of the document upload process. For example:

> -	`${ROOT_FOLDER}/`
>	- `37eef483-f506-41e1-9058-cd4cd64a5280/`
>	- `69ade68e-28ea-496c-8324-8cc3f186e96d/`
>	- `760aadb6-7744-4fef-867e-6078a07b5cbd/`
>	- etc...

Each document upload process folder contains:

> -	`input/`
>	-	`${SOURCE_FILE_NAME}`
> -	`control/`
>	-	`completed-folder`
>	-	`error-folder`
>	-	`wants-inf-ack`
>	-	`wants-bus-ack`
> -	`events/`
>	-	`${TIMESTAMP}-${EVENT_TYPE}`
>	-	`${TIMESTAMP}-${EVENT_TYPE}`
>	-	`${TIMESTAMP}-${EVENT_TYPE}`
>	-	etc...

Files are expected to be written by a single process and not subsequently updated.

### Input

The input folder is used to store a copy of the original input file prior to any transformations. The contents of this folder are not directly used or interpreted by `ciao-docs-finalizer`.

### Control

The control folder is used to store control/configuration details of the upload process.

`completed-folder` and `error-folder` are single-line text files defining where the in-progress folder should be moved to on completion and on error (subject to suitable completion actions being configured in `ciao-docs-finalizer`).

`wants-inf-ack` and `wants-bus-ack` are optional files defining whether an ITK infrastructure response and/or an ITK business response has been requested. The file contents are not read - the existence of the files is enough to determine if the acknowledgements have been requested. These files are associated with ITK transports and are added by the transport if applicable.

### Event Types

The events folder represents a log/journal of key events throughout the document upload process and is used to drive the `ciao-docs-finalizer` state machine.

Each event file is names using the pattern: `${TIMESTAMP}-${EVENT_TYPE}`.

The timestamp format is a modified version of [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) (suitable for file names):
-	`yyyyMMdd-HHmmssSSS` - *** (in UTC timezone) ***

Event files are processed in order of increasing `${TIMESTAMP}`.

The recognised event types are:
-	`document-parsed`
-	`document-preparation-timeout`
-	`document-preparation-failed`
-	`bus-message-sending`
-	`document-send-timeout`
-	`bus-message-send-failed`
-	`bus-message-sent`
-	`inf-response-timeout`
-	`inf-ack-received`
-	`inf-nack-received`
-	`bus-ack-received`
-	`bus-nack-received`
-	`bus-response-timeout`

> Additional details of the event types are available in the [Event](../src/main/java/uk/nhs/ciao/docs/finalizer/state/Event.class) class.

## States

The possible states of a document upload process are:
-	`PARSING`
-	`PREPARING`
-	`SENDING`
-	`WAITING_INF_AND_BUS_RESPONSE`
-	`WAITING_INF_RESPONSE`
-	`WAITING_BUS_RESPONSE`
-	`FAILED`
-	`SUCCEEDED`

> Additional details of the states are available in the [State](../src/main/java/uk/nhs/ciao/docs/finalizer/state/State.class) class.

## Camel Route

`ciao-docs-parser` provides the [InProgressFolderManagerRoute](https://github.com/nhs-ciao/ciao-docs-parser/blob/master/ciao-docs-parser-model/src/main/java/uk/nhs/ciao/docs/parser/route/InProgressFolderManagerRoute.java) class to support storing control and event files in the in-progress directory.
