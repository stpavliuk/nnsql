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

Run lint checks (Qodana):
```bash
./gradlew lint
```

## TPCH setup and run

Generate TPCH fixtures for the integration tests:
```bash
python3 tpch-tests/scripts/setup_benchmark_data.py --scale-factor 1
```

The generator requires either `tpchgen-cli` or `uvx`. By default it writes the
fixtures to `tpch-tests/src/test/resources/data/<scale-factor>`.

Run the TPCH integration suite:
```bash
./gradlew :tpch-tests:tpchTest
```

The TPCH suite now runs against Postgres. By default it derives the connection
from `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` and connects to
`jdbc:postgresql://localhost:5432/$POSTGRES_DB`. You can override that with:

```bash
./gradlew :tpch-tests:tpchTest \
  -Dnnsql.tpch.jdbcUrl=jdbc:postgresql://localhost:5432/postgres \
  -Dnnsql.tpch.jdbcUser=postgres \
  -Dnnsql.tpch.jdbcPassword=postgres
```

Run a specific subset of TPCH queries:
```bash
./gradlew :tpch-tests:tpchTest -Dnnsql.tpch.queries=1,3-5
```

Use a different fixture scale factor:
```bash
./gradlew :tpch-tests:tpchTest -Dnnsql.tpch.scaleFactor=10
```

Useful TPCH test properties:
- `-Dnnsql.tpch.scaleFactor=<n>` selects `tpch-tests/src/test/resources/data/<n>`.
- `-Dnnsql.tpch.queries=1,3-5` runs only the selected TPCH queries.
- `-Dnnsql.tpch.nullRate=<rate>` controls the null injection rate for the translated 6NF target.
- `-Dnnsql.tpch.timingRuns=<n>` sets how many measured executions are averaged per query.
- `-Dnnsql.tpch.timingWarmupRuns=<n>` sets how many warmup executions run before timing.
- `-Dnnsql.tpch.jdbcUrl=<jdbc-url>` overrides the Postgres JDBC URL.
- `-Dnnsql.tpch.jdbcUser=<user>` overrides the Postgres JDBC user.
- `-Dnnsql.tpch.jdbcPassword=<password>` overrides the Postgres JDBC password.
- `-Dnnsql.tpch.reportPath=<path>` overrides the HTML report output path.

By default, each TPCH run writes a timestamped HTML report under
`build/reports/tpch/`, for example `query-report-20260506-153012-123.html`,
and also refreshes `build/reports/tpch/query-report.html` as the latest report.

## Current limitations

- Explicit `JOIN ... ON ...` syntax is not supported yet.
- This means TPC-H `Q13` is currently unsupported, because it relies on
  `LEFT OUTER JOIN orders ON c_custkey = o_custkey AND ...`.

## Further work
- Implement additional SQL features and optimizations for 6NF queries
- Use LOAD for data import instead of translating INSERT statements
- Implement schema registry persistence and dynamic loading
