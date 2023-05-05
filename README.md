# Samv2 Medication Server

This project will expose the SAMv2 medication database through FHIR endpoints. 
The project will have following large phases: 
- A proof of concept that will implement an endpoint for the AMP - information.
- Automation of the PoC, exposing it through a domain for testing and automating the updates on SAM 
in conjunction with [the sam to SQLite project](https://github.com/JensPenny/SamToSqlite)
- A further implementation that will expand on the packaging, still within the base concepts
- An extension for the AMP - information, documented on simplifier.net
- Further extensions on for ex. the packaging

## Building and starting
1. Download the source code
2. Build the project through maven with the command `mvn jetty:run`

### Configuration
- SQLite database location configuration: see class `be.fhir.penny.db.SQLiteDbProvider`
- Server configuration: //todo

## Extra documentation
Further resources with regards to the thinking process are available in the docs-folder