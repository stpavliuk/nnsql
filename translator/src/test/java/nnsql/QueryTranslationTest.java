package nnsql;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryTranslationTest {
    private QueryTranslator translator;

    @BeforeEach
    void setUp() {
        SchemaRegistry schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));
        schemaRegistry.registerTable("S", List.of("B", "C"));
        schemaRegistry.registerTable("T", List.of("D", "E"));

        schemaRegistry.registerTable(
            "customer",
            List.of(
                "c_custkey",
                "c_name",
                "c_address",
                "c_nationkey",
                "c_phone",
                "c_acctbal",
                "c_mktsegment",
                "c_comment"
            )
        );

        translator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
    }

    @Test
    void testMultipleQueryTranslations() {
        assertQueryTranslation(
            // language=sql
            "SELECT R.A FROM R WHERE R.B = 5",
            // language=sql
            """
                WITH all_ids_product_0 AS (
                    SELECT R__ID.id || '_0' AS id,
                           R__ID.id AS id1
                    FROM R__ID AS R__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                    SELECT all_ids_product_0.id, R_A.v
                    FROM all_ids_product_0, R_A
                    WHERE all_ids_product_0.id1 = R_A.id
                ),
                product_0_R_B AS (
                    SELECT all_ids_product_0.id, R_B.v
                    FROM all_ids_product_0, R_B
                    WHERE all_ids_product_0.id1 = R_B.id
                ),
                filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id AND product_0_R_B.v = 5.0)
                ),
                filter_1_R_A AS (
                    SELECT product_0_R_A.*
                    FROM product_0_R_A JOIN filter_1_id ON filter_1_id.id = product_0_R_A.id
                ),
                return_2_id AS (
                    SELECT id FROM filter_1_id
                ),
                return_2_attr_R_A AS (
                    SELECT id, v FROM filter_1_R_A
                )
                SELECT return_2_attr_R_A.v AS R_A
                FROM return_2_id
                JOIN return_2_attr_R_A ON return_2_id.id = return_2_attr_R_A.id;\
                """
        );

        assertQueryTranslation(
            // language=sql
            "SELECT DISTINCT R.A FROM R, S WHERE R.B = S.B",
            // language=sql
            """
                WITH all_ids_product_0 AS (
                    SELECT R__ID.id || '_' || S__ID.id || '_0' AS id,
                           R__ID.id AS id1,
                           S__ID.id AS id2
                    FROM R__ID AS R__ID, S__ID AS S__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                    SELECT all_ids_product_0.id, R_A.v
                    FROM all_ids_product_0, R_A
                    WHERE all_ids_product_0.id1 = R_A.id
                ),
                product_0_R_B AS (
                    SELECT all_ids_product_0.id, R_B.v
                    FROM all_ids_product_0, R_B
                    WHERE all_ids_product_0.id1 = R_B.id
                ),
                product_0_S_B AS (
                    SELECT all_ids_product_0.id, S_B.v
                    FROM all_ids_product_0, S_B
                    WHERE all_ids_product_0.id2 = S_B.id
                ),
                filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE EXISTS (SELECT * FROM product_0_R_B, product_0_S_B WHERE product_0_R_B.id = product_0_id.id AND product_0_S_B.id = product_0_id.id AND product_0_R_B.v = product_0_S_B.v)
                ),
                filter_1_R_A AS (
                    SELECT product_0_R_A.*
                    FROM product_0_R_A JOIN filter_1_id ON filter_1_id.id = product_0_R_A.id
                ),
                return_2_id AS (
                    SELECT id FROM filter_1_id
                ),
                return_2_attr_R_A AS (
                    SELECT id, v FROM filter_1_R_A
                ),
                duplelim_3_id AS (
                    SELECT return_2_id.id
                    FROM return_2_id
                    WHERE NOT EXISTS (
                      SELECT * FROM return_2_id R1
                      WHERE R1.id < return_2_id.id
                        AND (EXISTS (SELECT * FROM return_2_attr_R_A TEMP1, return_2_attr_R_A TEMP2 WHERE TEMP1.id = return_2_id.id AND TEMP2.id = R1.id AND TEMP1.v = TEMP2.v) OR NOT EXISTS (SELECT * FROM return_2_attr_R_A WHERE return_2_attr_R_A.id = return_2_id.id OR return_2_attr_R_A.id = R1.id))
                    )
                ),
                duplelim_3_attr_R_A AS (
                    SELECT return_2_attr_R_A.*
                    FROM return_2_attr_R_A JOIN duplelim_3_id ON duplelim_3_id.id = return_2_attr_R_A.id
                )
                SELECT duplelim_3_attr_R_A.v AS R_A
                FROM duplelim_3_id
                JOIN duplelim_3_attr_R_A ON duplelim_3_id.id = duplelim_3_attr_R_A.id;\
                """
        );

        assertQueryTranslation(
            // language=sql
            "SELECT R.A, SUM(S.C) AS total FROM R, S WHERE R.B = S.B GROUP BY R.A",
            // language=sql
            """
                    WITH all_ids_product_0 AS (
                    SELECT R__ID.id || '_' || S__ID.id || '_0' AS id,
                           R__ID.id AS id1,
                           S__ID.id AS id2
                    FROM R__ID AS R__ID, S__ID AS S__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                    SELECT all_ids_product_0.id, R_A.v
                    FROM all_ids_product_0, R_A
                    WHERE all_ids_product_0.id1 = R_A.id
                ),
                product_0_R_B AS (
                    SELECT all_ids_product_0.id, R_B.v
                    FROM all_ids_product_0, R_B
                    WHERE all_ids_product_0.id1 = R_B.id
                ),
                product_0_S_B AS (
                    SELECT all_ids_product_0.id, S_B.v
                    FROM all_ids_product_0, S_B
                    WHERE all_ids_product_0.id2 = S_B.id
                ),
                product_0_S_C AS (
                    SELECT all_ids_product_0.id, S_C.v
                    FROM all_ids_product_0, S_C
                    WHERE all_ids_product_0.id2 = S_C.id
                ),
                filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE EXISTS (SELECT * FROM product_0_R_B, product_0_S_B WHERE product_0_R_B.id = product_0_id.id AND product_0_S_B.id = product_0_id.id AND product_0_R_B.v = product_0_S_B.v)
                ),
                filter_1_R_A AS (
                    SELECT product_0_R_A.*
                    FROM product_0_R_A JOIN filter_1_id ON filter_1_id.id = product_0_R_A.id
                ),
                filter_1_S_C AS (
                    SELECT product_0_S_C.*
                    FROM product_0_S_C JOIN filter_1_id ON filter_1_id.id = product_0_S_C.id
                ),
                group_2_id AS (
                    SELECT filter_1_id.id
                    FROM filter_1_id
                    WHERE NOT EXISTS (
                        SELECT * FROM filter_1_id R1
                        WHERE R1.id < filter_1_id.id AND EXISTS (SELECT * FROM filter_1_R_A a1, filter_1_R_A a2 WHERE a1.id = filter_1_id.id AND a2.id = R1.id AND a1.v = a2.v)
                    )
                ),
                group_2_R_A AS (
                    SELECT filter_1_R_A.*
                    FROM filter_1_R_A JOIN group_2_id ON group_2_id.id = filter_1_R_A.id
                ),
                group_2_total AS (
                    SELECT group_2_id.id, SUM(filter_1_S_C.v) AS v
                    FROM group_2_id, filter_1_id input_id, filter_1_S_C
                    WHERE filter_1_S_C.id = input_id.id AND EXISTS (SELECT * FROM filter_1_R_A g1, filter_1_R_A g2 WHERE g1.id = input_id.id AND g2.id = group_2_id.id AND g1.v = g2.v)
                    GROUP BY group_2_id.id
                ),
                return_3_id AS (
                SELECT id FROM group_2_id
                ),
                return_3_attr_R_A AS (
                    SELECT id, v FROM group_2_R_A
                ),
                return_3_attr_total AS (
                    SELECT id, v FROM group_2_total
                )
                SELECT return_3_attr_R_A.v AS R_A, return_3_attr_total.v AS total
                    FROM return_3_id
                    JOIN return_3_attr_R_A ON return_3_id.id = return_3_attr_R_A.id
                    JOIN return_3_attr_total ON return_3_id.id = return_3_attr_total.id;\
                """
        );

        assertQueryTranslation(
            // language=sql
            "SELECT T.D FROM T WHERE T.E IS NULL",
            // language=sql
            """
                    WITH all_ids_product_0 AS (
                    SELECT T__ID.id || '_0' AS id,
                           T__ID.id AS id1
                    FROM T__ID AS T__ID
                    ),
                    product_0_id AS (
                    SELECT id FROM all_ids_product_0
                    ),
                    product_0_T_D AS (
                    SELECT all_ids_product_0.id, T_D.v
                    FROM all_ids_product_0, T_D
                    WHERE all_ids_product_0.id1 = T_D.id
                    ),
                    product_0_T_E AS (
                    SELECT all_ids_product_0.id, T_E.v
                    FROM all_ids_product_0, T_E
                    WHERE all_ids_product_0.id1 = T_E.id
                    ),
                    filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE NOT EXISTS (SELECT * FROM product_0_T_E WHERE product_0_T_E.id = product_0_id.id)
                    ),
                    filter_1_T_D AS (
                    SELECT product_0_T_D.*
                    FROM product_0_T_D JOIN filter_1_id ON filter_1_id.id = product_0_T_D.id
                    ),
                    return_2_id AS (
                    SELECT id FROM filter_1_id
                    ),
                    return_2_attr_T_D AS (
                    SELECT id, v FROM filter_1_T_D
                    )
                    SELECT return_2_attr_T_D.v AS T_D
                    FROM return_2_id
                    JOIN return_2_attr_T_D ON return_2_id.id = return_2_attr_T_D.id;\
                """);

        assertQueryTranslation(
            // language=sql
            "SELECT R.A FROM R WHERE R.A > R.B",
            // language=sql
            """
                WITH all_ids_product_0 AS (
                SELECT R__ID.id || '_0' AS id,
                       R__ID.id AS id1
                FROM R__ID AS R__ID
                ),
                product_0_id AS (
                SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                SELECT all_ids_product_0.id, R_A.v
                FROM all_ids_product_0, R_A
                WHERE all_ids_product_0.id1 = R_A.id
                ),
                product_0_R_B AS (
                SELECT all_ids_product_0.id, R_B.v
                FROM all_ids_product_0, R_B
                WHERE all_ids_product_0.id1 = R_B.id
                ),
                filter_1_id AS (
                SELECT product_0_id.id
                FROM product_0_id
                WHERE EXISTS (SELECT * FROM product_0_R_A, product_0_R_B WHERE product_0_R_A.id = product_0_id.id AND product_0_R_B.id = product_0_id.id AND product_0_R_A.v > product_0_R_B.v)
                ),
                filter_1_R_A AS (
                SELECT product_0_R_A.*
                FROM product_0_R_A JOIN filter_1_id ON filter_1_id.id = product_0_R_A.id
                ),
                return_2_id AS (
                SELECT id FROM filter_1_id
                ),
                return_2_attr_R_A AS (
                SELECT id, v FROM filter_1_R_A
                )
                SELECT return_2_attr_R_A.v AS R_A
                FROM return_2_id
                JOIN return_2_attr_R_A ON return_2_id.id = return_2_attr_R_A.id;\
                """
        );
    }

    @Test
    void testTPCHSchema() {
        assertQueryTranslation(
            // language=sql
            """
                SELECT * FROM customer
                WHERE c_acctbal > 5000
                """,
            // language=sql
            """
                WITH all_ids_product_0 AS (
                    SELECT customer__ID.id || '_0' AS id,
                           customer__ID.id AS id1
                    FROM customer__ID AS customer__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_customer_c_acctbal AS (
                    SELECT all_ids_product_0.id, customer_c_acctbal.v
                    FROM all_ids_product_0, customer_c_acctbal
                    WHERE all_ids_product_0.id1 = customer_c_acctbal.id
                ),
                filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE EXISTS (SELECT * FROM product_0_customer_c_acctbal WHERE product_0_customer_c_acctbal.id = product_0_id.id AND product_0_customer_c_acctbal.v > 5000.0)
                ),
                return_2_id AS (
                    SELECT id FROM filter_1_id
                )
                SELECT * FROM return_2_id;
                """
        );

        assertQueryTranslation(
            // language=sql
            """
                SELECT c_mktsegment AS mkt, COUNT(c_custkey) AS seg FROM customer
                WHERE c_nationkey = 15 AND c_acctbal > (SELECT AVG(c_acctbal) AS avg_accball FROM customer
                    WHERE c_acctbal > 0.00 AND c_nationkey = 15)
                GROUP BY c_mktsegment
                HAVING seg > 500
                """,
            // language=sql
            """
                WITH all_ids_product_0 AS (
                    SELECT customer__ID.id || '_0' AS id,
                           customer__ID.id AS id1
                    FROM customer__ID AS customer__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_customer_c_custkey AS (
                    SELECT all_ids_product_0.id, customer_c_custkey.v
                    FROM all_ids_product_0, customer_c_custkey
                    WHERE all_ids_product_0.id1 = customer_c_custkey.id
                ),
                product_0_customer_c_nationkey AS (
                    SELECT all_ids_product_0.id, customer_c_nationkey.v
                    FROM all_ids_product_0, customer_c_nationkey
                    WHERE all_ids_product_0.id1 = customer_c_nationkey.id
                ),
                product_0_customer_c_acctbal AS (
                    SELECT all_ids_product_0.id, customer_c_acctbal.v
                    FROM all_ids_product_0, customer_c_acctbal
                    WHERE all_ids_product_0.id1 = customer_c_acctbal.id
                ),
                product_0_customer_c_mktsegment AS (
                    SELECT all_ids_product_0.id, customer_c_mktsegment.v
                    FROM all_ids_product_0, customer_c_mktsegment
                    WHERE all_ids_product_0.id1 = customer_c_mktsegment.id
                ),
                all_ids_product_2 AS (
                    SELECT customer__ID.id || '_0' AS id,
                           customer__ID.id AS id1
                    FROM customer__ID AS customer__ID
                ),
                product_2_id AS (
                    SELECT id FROM all_ids_product_2
                ),
                product_2_customer_c_nationkey AS (
                    SELECT all_ids_product_2.id, customer_c_nationkey.v
                    FROM all_ids_product_2, customer_c_nationkey
                    WHERE all_ids_product_2.id1 = customer_c_nationkey.id
                ),
                product_2_customer_c_acctbal AS (
                    SELECT all_ids_product_2.id, customer_c_acctbal.v
                    FROM all_ids_product_2, customer_c_acctbal
                    WHERE all_ids_product_2.id1 = customer_c_acctbal.id
                ),
                filter_3_id AS (
                    SELECT product_2_id.id
                    FROM product_2_id
                    WHERE (EXISTS (SELECT * FROM product_2_customer_c_acctbal WHERE product_2_customer_c_acctbal.id = product_2_id.id AND product_2_customer_c_acctbal.v > 0.0)) AND (EXISTS (SELECT * FROM product_2_customer_c_nationkey WHERE product_2_customer_c_nationkey.id = product_2_id.id AND product_2_customer_c_nationkey.v = 15.0))
                ),
                filter_3_customer_c_acctbal AS (
                    SELECT product_2_customer_c_acctbal.*
                    FROM product_2_customer_c_acctbal JOIN filter_3_id ON filter_3_id.id = product_2_customer_c_acctbal.id
                ),
                group_4_id AS (
                    SELECT MIN(id) AS id
                    FROM filter_3_id
                ),
                group_4_avg_accball AS (
                    SELECT group_4_id.id, AVG(filter_3_customer_c_acctbal.v) AS v
                    FROM group_4_id, filter_3_id input_id, filter_3_customer_c_acctbal
                    WHERE filter_3_customer_c_acctbal.id = input_id.id
                    GROUP BY group_4_id.id
                ),
                return_5_attr_avg_accball AS (
                    SELECT id, v FROM group_4_avg_accball
                ),
                filter_1_id AS (
                    SELECT product_0_id.id
                    FROM product_0_id
                    WHERE (EXISTS (SELECT * FROM product_0_customer_c_nationkey WHERE product_0_customer_c_nationkey.id = product_0_id.id AND product_0_customer_c_nationkey.v = 15.0)) AND (EXISTS (SELECT * FROM product_0_customer_c_acctbal WHERE product_0_customer_c_acctbal.id = product_0_id.id AND product_0_customer_c_acctbal.v > (SELECT v FROM return_5_attr_avg_accball)))
                ),
                filter_1_customer_c_custkey AS (
                    SELECT product_0_customer_c_custkey.*
                    FROM product_0_customer_c_custkey JOIN filter_1_id ON filter_1_id.id = product_0_customer_c_custkey.id
                ),
                filter_1_customer_c_mktsegment AS (
                    SELECT product_0_customer_c_mktsegment.*
                    FROM product_0_customer_c_mktsegment JOIN filter_1_id ON filter_1_id.id = product_0_customer_c_mktsegment.id
                ),
                group_6_id AS (
                SELECT filter_1_id.id
                FROM filter_1_id
                    WHERE NOT EXISTS (
                        SELECT * FROM filter_1_id R1
                        WHERE R1.id < filter_1_id.id AND EXISTS (SELECT * FROM filter_1_customer_c_mktsegment a1, filter_1_customer_c_mktsegment a2 WHERE a1.id = filter_1_id.id AND a2.id = R1.id AND a1.v = a2.v)
                    )
                ),
                group_6_customer_c_mktsegment AS (
                    SELECT filter_1_customer_c_mktsegment.*
                    FROM filter_1_customer_c_mktsegment JOIN group_6_id ON group_6_id.id = filter_1_customer_c_mktsegment.id
                ),
                group_6_seg AS (
                    SELECT group_6_id.id, COUNT(filter_1_customer_c_custkey.v) AS v
                    FROM group_6_id, filter_1_id input_id, filter_1_customer_c_custkey
                    WHERE filter_1_customer_c_custkey.id = input_id.id AND EXISTS (SELECT * FROM filter_1_customer_c_mktsegment g1, filter_1_customer_c_mktsegment g2 WHERE g1.id = input_id.id AND g2.id = group_6_id.id AND g1.v = g2.v)
                    GROUP BY group_6_id.id
                    UNION
                    SELECT group_6_id.id, 0 AS v
                    FROM group_6_id
                    WHERE NOT EXISTS (
                        SELECT * FROM filter_1_id input_id, filter_1_customer_c_custkey
                        WHERE filter_1_customer_c_custkey.id = input_id.id AND EXISTS (SELECT * FROM filter_1_customer_c_mktsegment g1, filter_1_customer_c_mktsegment g2 WHERE g1.id = input_id.id AND g2.id = group_6_id.id AND g1.v = g2.v)
                    )
                ),
                aggfilter_7_id AS (
                    SELECT group_6_id.id
                    FROM group_6_id
                    WHERE EXISTS (SELECT * FROM group_6_seg WHERE group_6_seg.id = group_6_id.id AND group_6_seg.v > 500.0)
                ),
                aggfilter_7_customer_c_mktsegment AS (
                    SELECT group_6_customer_c_mktsegment.*
                    FROM group_6_customer_c_mktsegment JOIN aggfilter_7_id ON aggfilter_7_id.id = group_6_customer_c_mktsegment.id
                ),
                aggfilter_7_seg AS (
                    SELECT group_6_seg.*
                    FROM group_6_seg JOIN aggfilter_7_id ON aggfilter_7_id.id = group_6_seg.id
                ),
                return_8_id AS (
                    SELECT id FROM aggfilter_7_id
                ),
                return_8_attr_mkt AS (
                    SELECT id, v FROM aggfilter_7_customer_c_mktsegment
                ),
                return_8_attr_seg AS (
                    SELECT id, v FROM aggfilter_7_seg
                )
                SELECT return_8_attr_mkt.v AS mkt, return_8_attr_seg.v AS seg
                    FROM return_8_id
                    JOIN return_8_attr_mkt ON return_8_id.id = return_8_attr_mkt.id
                    JOIN return_8_attr_seg ON return_8_id.id = return_8_attr_seg.id;
                """
        );
    }

    private void assertQueryTranslation(String inputQuery, String expectedOutput) {
        String actualOutput = translator.translate(inputQuery);
        assertEquals(normalizeWhitespace(expectedOutput), normalizeWhitespace(actualOutput));
    }

    private String normalizeWhitespace(String str) {
        return str
            .trim()
            .replaceAll("\\s+", " ");
    }
}
