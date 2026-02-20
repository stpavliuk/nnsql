package nnsql;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.lang.IO.println;

class TPCDSQueryTranslationTest {
    private QueryTranslator translator;

    @BeforeEach
    void setUp() {
        SchemaRegistry schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("customer", List.of(
            "c_customer_sk",
            "c_customer_id",
            "c_current_cdemo_sk",
            "c_current_hdemo_sk",
            "c_current_addr_sk",
            "c_first_shipto_date_sk",
            "c_first_sales_date_sk",
            "c_salutation",
            "c_first_name",
            "c_last_name",
            "c_preferred_cust_flag",
            "c_birth_day",
            "c_birth_month",
            "c_birth_year",
            "c_birth_country",
            "c_login",
            "c_email_address",
            "c_last_review_date_sk"
        ));

        translator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
    }

    @Test
    void testCountWithOrConditions() {
        // language=sql
        println(translator.translate("""
            SELECT
                count(c_customer_id) as canada_academics
            FROM
                customer
            WHERE
                (c_salutation = 'Dr.' OR c_salutation = 'Prof.')
                OR c_birth_country = 'Canada';
            """));
    }

    @Test
    void testCountWithNotAndIsNull() {
        // language=sql
        println(translator.translate("""
            SELECT
                count(c_customer_sk)
            FROM
                customer
            WHERE
                NOT (c_birth_year = 1980) or c_birth_year is null;
            """));
    }
}
