package nnsql.ddl;

import nnsql.Tranlator;
import nnsql.query.SchemaRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record DDLTranslator(SchemaRegistry schemaRegistry) implements Tranlator {

    public String translate(String ddlScript) {
        var tableDefs = DDLParser.parse(ddlScript);
        var statements = new ArrayList<String>();

        for (var tableDef : tableDefs) {
            statements.add(translateTable(tableDef));
        }

        return String.join("\n\n", statements);
    }

    private String translateTable(DDLParser.TableDef tableDef) {
        schemaRegistry.registerTable(
            tableDef.name(),
            tableDef.columns().stream().map(DDLParser.ColumnDef::name).toList(),
            tableDef.columns().stream()
                     .collect(Collectors.toMap(DDLParser.ColumnDef::name, DDLParser.ColumnDef::dataType)),
            tableDef.primaryKeyColumn().map(List::of).orElse(List.of())
        );

        var statements = new ArrayList<String>();
        statements.add(createIdTable(tableDef));

        for (var column : tableDef.columns()) {
            statements.add(createAttributeTable(tableDef, column));
        }

        return String.join(";\n", statements) + ";";
    }

    private String createIdTable(DDLParser.TableDef tableDef) {
        var idType = determineIdType(tableDef);
        return Format.idTable(tableDef.name(), idType);
    }

    private String createAttributeTable(DDLParser.TableDef tableDef, DDLParser.ColumnDef column) {
        var idType = determineIdType(tableDef);
        return Format.attributeTable(tableDef.name(), column.name(), idType, column.dataType());
    }

    private String determineIdType(DDLParser.TableDef tableDef) {
        return tableDef
            .primaryKeyColumn()
            .map(pkCol -> tableDef
                .columns()
                .stream()
                .filter(col -> col.name().equals(pkCol))
                .findFirst()
                .map(DDLParser.ColumnDef::dataType)
                .orElse(Format.VARCHAR_64))
            .orElse(Format.VARCHAR_64);
    }
}
