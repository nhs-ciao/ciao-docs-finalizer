# Document Upload State Machine

`ciao-docs-finalizer` tracks the ongoing state of a document upload process via an internal state machine. The state machine is updated by monitoring an *in-progress folder* for event files which in turn trigger an event transition.

The event files are stored in the in-progress directory by other CIPs involved in the upload progress. The contents/format of the event files are determined by the CIP doing the writing, however the structure and naming of the event files is common across all CIPs.

## Structure of the In-Progress Folder 
TODO:


## Event Types
TODO:

## State Transitions
TODO:

