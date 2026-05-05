package nnsql.tpch.framework;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import nnsql.query.SchemaRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;

final class TpchSchemaRegistryFactory {

    private TpchSchemaRegistryFactory() {
    }

    static SchemaRegistry create(TpchFixtureSet fixtureSet) throws IOException {
        var schemaRegistry = new SchemaRegistry();
        var statements = parseStatements(Files.readString(fixtureSet.schemaPath(), StandardCharsets.UTF_8));

        for (var statement : statements) {
            if (!(statement instanceof CreateTable createTable)) {
                continue;
            }

            var tableName = createTable.getTable().getName();
            var columns = createTable.getColumnDefinitions();
            var columnNames = columns.stream()
                .map(ColumnDefinition::getColumnName)
                .toList();
            var columnTypes = new LinkedHashMap<String, String>();
            for (var column : columns) {
                columnTypes.put(column.getColumnName(), column.getColDataType().toString());
            }

            schemaRegistry.registerTable(
                tableName,
                columnNames,
                columnTypes,
                fixtureSet.primaryKeys(tableName)
            );
        }

        return schemaRegistry;
    }

    private static Statements parseStatements(String sql) {
        try {
            return CCJSqlParserUtil.parseStatements(sql);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse TPCH schema.sql", e);
        }
    }
}
