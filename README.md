# spdx-edit
SpdxEdit is a simple SPDX 2.1 editor/builder, which can be used to edit existing SPDX 2.1 files or to generate new ones. 

SpdxEdit uses [Spdx Tools](https://github.com/spdx/tools) to read, edit, and write RDF-formatted SPDX files. To run, extract the zip file, navigate to the `bin` directory, and run `spdx-edit` (on Mac or *nix) or `spdx-edit.bat` (on Windows). To build the application yourself, you can clone this repository and run ```./gradlew assemble```.

SpdxEdit requires [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).

SpdxEdit is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

###Known limitations:
Only one extracted license per file is supported (SPDX spec supports unlimited).
Standard licenses found in files not presently supported
