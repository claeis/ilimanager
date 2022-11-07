# ilimanager - creates/updates INTERLIS repository index files

## Features
- creates/updates ilimodels.xml from ili-files
- creates/updates ilidata.xml from xtf/itf-files
- clones repositories

## License
ilimanager is licensed under the LGPL (Lesser GNU Public License).

## System Requirements
For the current version of ilivalidator, you will need a JRE (Java Runtime Environment) installed on your system, version 1.8 or later. Any OpenJDK based JRE will do.
The JRE (Java Runtime Environment) can be downloaded from the Website <http://www.java.com/>.

## Software Download 
TBD

## Installing ilimanager
To install the ilimanager, choose a directory and extract the distribution file there. 

## Running ilimanager
The ilimanager can be started with

    java -jar ilimanager.jar [options]

## Building from source
To build the `ilimanager.jar`, use

    gradle build

To build a binary distribution, use

    gradle bindist

### Development dependencies
* JDK 1.8 or higher (OpenJDK will do)
* Gradle
* Python and docutils installed (`pip install docutils`)
    * rst2html command is used by `userdoc` gradle task
    * rst2html location can be provided in file _user.properties_

## Documentation
[docs/ilimanager.rst](docs/ilimanager.rst)
