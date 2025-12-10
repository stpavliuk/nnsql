package nnsql;

import nnsql.ddl.DDLTranslator;
import nnsql.dml.DMLTranslator;
import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;

public class Main {
    static void main(String[] input) {
        try {
            if (input.length < 1) {
                throw new IllegalArgumentException(
                    "You should provide the option: print, translate-ddl, translate-dml, translate-query");
            }

            var operation = input[0];
            if (input.length < 2) {
                throw new IllegalArgumentException(
                    "You should provide a query/statement for operation: " + operation);
            }

            var statement = input[1];

            switch (operation) {
                case "translate-ddl" -> {
                    var schemaRegistry = new SchemaRegistry();
                    var translator = new DDLTranslator(schemaRegistry);
                    String translated = translator.translate(statement);
                    System.out.println(translated);
                }

                case "translate-dml" -> {
                    var schemaRegistry = new SchemaRegistry();
                    var translator = new DMLTranslator(schemaRegistry);
                    String translated = translator.translate(statement);
                    System.out.println(translated);
                }

                case "translate-query" -> {
                    var schemaRegistry = new SchemaRegistry();
                    var translator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
                    String translated = translator.translate(statement);
                    System.out.println(translated);
                }

                default -> throw new IllegalArgumentException(
                    "Wrong operation provided. Use: print, translate-ddl, translate-dml, translate-query");
            }
        } catch (Throwable e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
