package nnsql.ddl;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import nnsql.Translator;
import nnsql.query.SchemaRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record DDLTranslator(SchemaRegistry schemaRegistry) implements Translator {

    public String translate(String ddlScript) {
        try {
            var statements = CCJSqlParserUtil.parseStatements(ddlScript);
            return statements.stream()
                    .filter(CreateTable.class::isInstance)
                    .map(CreateTable.class::cast)
                    .map(this::translateTable)
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DDL: " + ddlScript, e);
        }
    }

    private String translateTable(CreateTable create) {
        var tableName = create.getTable().getName();
        var columns = create.getColumnDefinitions();

        var pkColumn = findPrimaryKeyColumn(columns);

        var columnNames = columns.stream().map(ColumnDefinition::getColumnName).toList();
        var columnTypes = columns.stream()
                .collect(Collectors.toMap(
                        ColumnDefinition::getColumnName,
                        col -> col.getColDataType().toString()));

        schemaRegistry.registerTable(
                tableName,
                columnNames,
                columnTypes,
                pkColumn.map(List::of).orElse(List.of()));

        var stmts = new ArrayList<String>();
        var idType = pkColumn
                .map(pk -> columnTypes.getOrDefault(pk, Format.VARCHAR_64))
                .orElse(Format.VARCHAR_64);

        stmts.add(Format.idTable(tableName, idType));
        for (var col : columns) {
            stmts.add(Format.attributeTable(
                    tableName, col.getColumnName(), idType, col.getColDataType().toString()));
        }

        return String.join(";\n", stmts) + ";";
    }

    private Optional<String> findPrimaryKeyColumn(List<ColumnDefinition> columns) {
        for (var col : columns) {
            if (isPrimaryKey(col.getColumnSpecs())) {
                return Optional.of(col.getColumnName());
            }
        }
        return Optional.empty();
    }

    private boolean isPrimaryKey(List<String> specs) {
        if (specs == null)
            return false;
        var joined = String.join(" ", specs).toUpperCase();
        return joined.contains("PRIMARY KEY");
    }
}
