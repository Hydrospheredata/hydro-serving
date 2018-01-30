## Runtimes

ML Lambda creates a `/model` folder in runtime docker container.

The structure of `/model` is:
 - `/model/files` -- folder with exported model files
 - `contract.protobin` -- [ModelContract](/docs/contracts.md) protobuf message.
 
NB: the structure may change.