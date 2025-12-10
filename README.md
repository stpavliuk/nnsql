# NNSQL Translator

Translator from normal SQL queries to the queries operated on 6th Normal Form (6NF) relations.

Includes translation for DDL and DML statements as well as SQL statements.

## Project Structure

The project consists of the following modules:
- **translator** - Main application module
- **sql-parser** - SQL query parsing
- **ddl-parser** - Data Definition Language parser
- **dml-parser** - Data Manipulation Language parser

## Prerequisites

- Java Development Kit (JDK) 25
- Gradle 9.1+ (or use the included Gradle wrapper)

## Building the Project

Build the entire project:
```bash
./gradlew build
```

Build a specific module:
```bash
./gradlew translator:build
```

Clean build artifacts:
```bash
./gradlew clean
```

## Running the Application

Run the main application:
```bash
./gradlew translator:run
```

Run the data import utility:
```bash
./gradlew import-data
```

## Testing

Run all tests:
```bash
./gradlew test
```

## Further work
- Implement additional SQL features and optimizations for 6NF queries
- Use LOAD for data import instead of translating INSERT statements
- Implement schema registry persistence and dynamic loading

