Oracle APEX Maven plugin
========================

A Maven plugin for Oracle Application Express development

##About

The primary goal of the Oracle APEX Maven plugin is to streamline and facilitate the build process for APEX applications.

Currently available tasks:
* Import an APEX application in a target workspace. <code>import</code>
* Run [Natural Docs](http://www.naturaldocs.org/) to generate technical documentation based on comments in your code. <code>run-natural-docs</code>

The following tasks are currently being worked on:
* Check your database objects and code for naming violations using the [API_NAMING_CONVENTION](https://github.com/nbuytaert1/orcl-naming-convention) package.
* Extract table and column comments in Natural Docs format.
* Compile a target database schema and invalid database objects notification.
* Automatically generate TAPI (Table API) packages.

##Installation

- Download the latest release of the Oracle APEX Maven plugin.
- Unzip the downloaded archive file.
- Open a terminal window and change the directory to the unzipped orclapex-maven-plugin folder.
- Install the JAR file in your Maven repository: <code>mvn install:install-file -Dfile=orclapex-maven-plugin-1.0.1.jar -DpomFile=orclapex-maven-plugin-1.0.1-pom.xml</code>

##Blog posts

* http://apexplained.wordpress.com/2014/04/08/introducing-the-oracle-apex-maven-plugin/

##License

See LICENSE.md