# spdx-edit
SpdxEdit is a simple SPDX 2.0 editor/builder, which can be used to edit existing SPDX 2.0 files or to generate new ones. 

SpdxEdit uses [Spdx Tools](https://github.com/spdx/tools) to read, edit, and write RDF-formatted SPDX files. SpdxEdit requires version 2.0.2 of SpdxTools, which is not published to Maven Central. To build, first clone the SpdxTools repository and run ```mvn make install```. Then, you can clone this repository and run ```mvn clean package```.

SpdxEdit is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
