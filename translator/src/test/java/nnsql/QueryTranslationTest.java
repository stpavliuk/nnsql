package nnsql;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryTranslationTest {
    private QueryTranslator translator;

    @SuppressWarnings("unused")
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
                    WHERE NOT EXISTS (SELECT * FROM return_2_id R1 WHERE R1.id < return_2_id.id AND (EXISTS (SELECT * FROM return_2_attr_R_A TEMP1, return_2_attr_R_A TEMP2 WHERE TEMP1.id = return_2_id.id AND TEMP2.id = R1.id AND TEMP1.v = TEMP2.v) OR NOT EXISTS (SELECT * FROM return_2_attr_R_A WHERE return_2_attr_R_A.id = return_2_id.id OR return_2_attr_R_A.id = R1.id)))
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
                    WHERE NOT EXISTS (SELECT * FROM filter_1_id R1 WHERE R1.id < filter_1_id.id AND EXISTS (SELECT * FROM filter_1_R_A a1, filter_1_R_A a2 WHERE a1.id = filter_1_id.id AND a2.id = R1.id AND a1.v = a2.v))
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
    void testBetweenPredicates() {
        var betweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B BETWEEN 10 AND 20"
        ));
        assertTrue(
            betweenSql.contains(
                "(EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id AND product_0_R_B.v >= 10.0)) AND (EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id AND product_0_R_B.v <= 20.0))"
            )
        );

        var notBetweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B NOT BETWEEN 10 AND 20"
        ));
        assertTrue(
            notBetweenSql.contains(
                "(EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id AND product_0_R_B.v < 10.0)) OR (EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id AND product_0_R_B.v > 20.0))"
            )
        );

        var columnBetweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A BETWEEN R.B AND R.B"
        ));
        assertTrue(
            columnBetweenSql.contains(
                "(EXISTS (SELECT * FROM product_0_R_A, product_0_R_B WHERE product_0_R_A.id = product_0_id.id AND product_0_R_B.id = product_0_id.id AND product_0_R_A.v >= product_0_R_B.v)) AND (EXISTS (SELECT * FROM product_0_R_A, product_0_R_B WHERE product_0_R_A.id = product_0_id.id AND product_0_R_B.id = product_0_id.id AND product_0_R_A.v <= product_0_R_B.v))"
            )
        );
    }

    @Test
    void testInListPredicates() {
        var inSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B IN (1, 2, 3)"
        ));
        assertTrue(inSql.contains("product_0_R_B.v = 1.0"));
        assertTrue(inSql.contains("product_0_R_B.v = 2.0"));
        assertTrue(inSql.contains("product_0_R_B.v = 3.0"));
        assertTrue(inSql.contains(" OR "));

        var notInSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B NOT IN (1, 2, 3)"
        ));
        assertTrue(notInSql.contains("product_0_R_B.v != 1.0"));
        assertTrue(notInSql.contains("product_0_R_B.v != 2.0"));
        assertTrue(notInSql.contains("product_0_R_B.v != 3.0"));
        assertTrue(notInSql.contains(" AND "));

        var stringInSql = normalizeWhitespace(translator.translate(
            "SELECT customer.c_custkey FROM customer WHERE customer.c_mktsegment IN ('AUTOMOBILE', 'BUILDING')"
        ));
        assertTrue(stringInSql.contains("product_0_customer_c_mktsegment.v = 'AUTOMOBILE'"));
        assertTrue(stringInSql.contains("product_0_customer_c_mktsegment.v = 'BUILDING'"));
        assertTrue(stringInSql.contains(" OR "));

        var singleValueInSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B IN (42)"
        ));
        assertTrue(singleValueInSql.contains("product_0_R_B.v = 42.0"));
        assertFalse(singleValueInSql.contains(" OR "));
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
                    WHERE NOT EXISTS (SELECT * FROM filter_1_id R1 WHERE R1.id < filter_1_id.id AND EXISTS (SELECT * FROM filter_1_customer_c_mktsegment a1, filter_1_customer_c_mktsegment a2 WHERE a1.id = filter_1_id.id AND a2.id = R1.id AND a1.v = a2.v))
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
                    WHERE NOT EXISTS (SELECT * FROM filter_1_id input_id, filter_1_customer_c_custkey WHERE filter_1_customer_c_custkey.id = input_id.id AND EXISTS (SELECT * FROM filter_1_customer_c_mktsegment g1, filter_1_customer_c_mktsegment g2 WHERE g1.id = input_id.id AND g2.id = group_6_id.id AND g1.v = g2.v))
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

    @Test
    void testCTE() {
        assertQueryTranslation(
            // language=sql
            "WITH ctr AS (SELECT A, B FROM R WHERE B > 5) SELECT A FROM ctr WHERE B = 10",
            // language=sql
            """
                WITH all_ids_product_1 AS (
                    SELECT R__ID.id || '_0' AS id, R__ID.id AS id1 FROM R__ID AS R__ID
                ),
                product_1_id AS (
                    SELECT id FROM all_ids_product_1
                ),
                product_1_R_A AS (
                    SELECT all_ids_product_1.id, R_A.v FROM all_ids_product_1, R_A WHERE all_ids_product_1.id1 = R_A.id
                ),
                product_1_R_B AS (
                    SELECT all_ids_product_1.id, R_B.v FROM all_ids_product_1, R_B WHERE all_ids_product_1.id1 = R_B.id
                ),
                filter_2_id AS (
                    SELECT product_1_id.id FROM product_1_id WHERE EXISTS (SELECT * FROM product_1_R_B WHERE product_1_R_B.id = product_1_id.id AND product_1_R_B.v > 5.0)
                ),
                filter_2_R_A AS (
                    SELECT product_1_R_A.* FROM product_1_R_A JOIN filter_2_id ON filter_2_id.id = product_1_R_A.id
                ),
                filter_2_R_B AS (
                    SELECT product_1_R_B.* FROM product_1_R_B JOIN filter_2_id ON filter_2_id.id = product_1_R_B.id
                ),
                return_3_id AS (
                    SELECT id FROM filter_2_id
                ),
                return_3_attr_A AS (
                    SELECT id, v FROM filter_2_R_A
                ),
                return_3_attr_B AS (
                    SELECT id, v FROM filter_2_R_B
                ),
                ctr__ID AS (
                    SELECT id FROM return_3_id
                ),
                ctr_A AS (
                    SELECT id, v FROM return_3_attr_A
                ),
                ctr_B AS (
                    SELECT id, v FROM return_3_attr_B
                ),
                all_ids_product_0 AS (
                    SELECT ctr__ID.id || '_0' AS id, ctr__ID.id AS id1 FROM ctr__ID AS ctr__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_ctr_A AS (
                    SELECT all_ids_product_0.id, ctr_A.v FROM all_ids_product_0, ctr_A WHERE all_ids_product_0.id1 = ctr_A.id
                ),
                product_0_ctr_B AS (
                    SELECT all_ids_product_0.id, ctr_B.v FROM all_ids_product_0, ctr_B WHERE all_ids_product_0.id1 = ctr_B.id
                ),
                filter_4_id AS (
                    SELECT product_0_id.id FROM product_0_id WHERE EXISTS (SELECT * FROM product_0_ctr_B WHERE product_0_ctr_B.id = product_0_id.id AND product_0_ctr_B.v = 10.0)
                ),
                filter_4_ctr_A AS (
                    SELECT product_0_ctr_A.* FROM product_0_ctr_A JOIN filter_4_id ON filter_4_id.id = product_0_ctr_A.id
                ),
                return_5_id AS (
                    SELECT id FROM filter_4_id
                ),
                return_5_attr_A AS (
                    SELECT id, v FROM filter_4_ctr_A
                )
                SELECT return_5_attr_A.v AS A
                FROM return_5_id
                JOIN return_5_attr_A ON return_5_id.id = return_5_attr_A.id;\
                """
        );
    }

    @Test
    void testArithmeticExpressions() {
        var addSql = normalizeWhitespace(translator.translate(
            "SELECT R.A + R.B AS total FROM R"
        ));
        assertTrue(addSql.contains("return_1_attr_total"));
        assertTrue(addSql.contains("product_0_R_A.v + product_0_R_B.v"));

        var mulWhereSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B * 2 > 100"
        ));
        assertTrue(mulWhereSql.contains("product_0_R_B.v * 2.0 > 100.0"));

        var compoundSql = normalizeWhitespace(translator.translate(
            "SELECT (R.A + R.B) * 2 AS computed FROM R"
        ));
        assertTrue(compoundSql.contains("return_1_attr_computed"));
        assertTrue(compoundSql.contains("product_0_R_A.v + product_0_R_B.v"));
        assertTrue(compoundSql.contains("* 2.0"));

        var divWhereSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A / 10 < R.B"
        ));
        assertTrue(divWhereSql.contains("product_0_R_A.v / 10.0 < product_0_R_B.v"));

        var subSql = normalizeWhitespace(translator.translate(
            "SELECT R.A - R.B AS diff FROM R"
        ));

        assertTrue(subSql.contains("product_0_R_A.v - product_0_R_B.v"));
    }

    @Test
    void testCastExpressions() {
        var selectCastSql = normalizeWhitespace(translator.translate(
            "SELECT CAST(R.A AS INTEGER) AS a_int FROM R"
        ));
        assertTrue(selectCastSql.contains("return_1_attr_a_int"));
        assertTrue(selectCastSql.contains("CAST(product_0_R_A.v AS INTEGER) AS v"));

        var whereCastSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE CAST(R.B AS DECIMAL(15,2)) > 100.50"
        ));
        assertTrue(whereCastSql.contains("CAST(product_0_R_B.v AS DECIMAL"));
        assertTrue(whereCastSql.contains("> 100.5"));

        var combinedCastSql = normalizeWhitespace(translator.translate(
            "SELECT CAST(R.A + R.B AS DECIMAL(10,2)) AS total FROM R"
        ));
        assertTrue(combinedCastSql.contains("return_1_attr_total"));
        assertTrue(combinedCastSql.contains("CAST(product_0_R_A.v + product_0_R_B.v AS DECIMAL"));
    }

    @Test
    void testCaseWhenExpressions() {
        var searchedCaseSql = normalizeWhitespace(translator.translate(
            "SELECT CASE WHEN R.A > 10 THEN 'high' ELSE 'low' END AS category FROM R"
        ));
        assertTrue(searchedCaseSql.contains("return_1_attr_category"));
        assertTrue(searchedCaseSql.contains("CASE WHEN product_0_R_A.v > 10.0 THEN 'high' ELSE 'low' END"));
        assertTrue(searchedCaseSql.contains("LEFT JOIN product_0_R_A"));
        assertTrue(searchedCaseSql.contains("IS NOT NULL"));

        var simpleCaseSql = normalizeWhitespace(translator.translate(
            "SELECT CASE R.B WHEN 1 THEN 'one' WHEN 2 THEN 'two' END AS label FROM R"
        ));
        assertTrue(simpleCaseSql.contains("return_1_attr_label"));
        assertTrue(simpleCaseSql.contains("product_0_R_B.v = 1.0"));
        assertTrue(simpleCaseSql.contains("product_0_R_B.v = 2.0"));
        assertTrue(simpleCaseSql.contains("LEFT JOIN product_0_R_B"));

        var caseInAggregateSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(CASE WHEN R.A > 0 THEN R.B ELSE 0 END) AS total FROM R"
        ));
        assertTrue(caseInAggregateSql.contains("SUM(CASE WHEN"));
        assertTrue(caseInAggregateSql.contains("product_0_R_A.v > 0.0"));
        assertTrue(caseInAggregateSql.contains("THEN product_0_R_B.v ELSE 0.0 END"));
        assertTrue(caseInAggregateSql.contains("LEFT JOIN product_0_R_A"));
        assertTrue(caseInAggregateSql.contains("LEFT JOIN product_0_R_B"));

        var nestedCaseSql = normalizeWhitespace(translator.translate(
            "SELECT CASE WHEN R.A > 10 THEN CASE WHEN R.B > 5 THEN 'both' ELSE 'a' END ELSE 'none' END AS nested FROM R"
        ));
        assertTrue(nestedCaseSql.contains("return_1_attr_nested"));
        assertTrue(nestedCaseSql.contains(
            "CASE WHEN product_0_R_A.v > 10.0 THEN CASE WHEN product_0_R_B.v > 5.0 THEN 'both' ELSE 'a' END ELSE 'none' END"
        ));
        assertTrue(nestedCaseSql.contains("LEFT JOIN product_0_R_A"));
        assertTrue(nestedCaseSql.contains("LEFT JOIN product_0_R_B"));

        var caseInWhereSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE CASE WHEN R.B > 0 THEN R.A ELSE 0 END > 5"
        ));
        assertTrue(caseInWhereSql.contains("WHERE EXISTS (SELECT * FROM"));
        assertTrue(caseInWhereSql.contains(
            "CASE WHEN product_0_R_B.v > 0.0 THEN product_0_R_A.v ELSE 0.0 END > 5.0"
        ));
        assertTrue(caseInWhereSql.contains("cw_id"));
        assertTrue(caseInWhereSql.contains("LEFT JOIN product_0_R_A"));
        assertTrue(caseInWhereSql.contains("LEFT JOIN product_0_R_B"));
    }

    @Test
    void testCaseWhenNullHandling() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT CASE WHEN R.A > 10 THEN R.B ELSE 0 END AS result FROM R"
        ));
        assertTrue(sql.contains("return_1_attr_result"));
        assertTrue(sql.contains("LEFT JOIN product_0_R_A"));
        assertTrue(sql.contains("LEFT JOIN product_0_R_B"));
        assertTrue(sql.contains("IS NOT NULL"));

        var addSql = normalizeWhitespace(translator.translate(
            "SELECT R.A + R.B AS total FROM R"
        ));
        assertFalse(addSql.contains("LEFT JOIN"),
            "Non-CASE BinaryOp should use INNER JOIN, not LEFT JOIN");

        var castSql = normalizeWhitespace(translator.translate(
            "SELECT CAST(R.A AS INTEGER) AS a_int FROM R"
        ));
        assertFalse(castSql.contains("LEFT JOIN"),
            "Non-CASE Cast should use INNER JOIN, not LEFT JOIN");

        var computedWhereSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A + R.B > 5"
        ));
        assertTrue(computedWhereSql.contains("WHERE EXISTS (SELECT * FROM"));
        assertTrue(computedWhereSql.contains("product_0_R_A.v + product_0_R_B.v > 5.0"));
        assertFalse(computedWhereSql.contains("LEFT JOIN"),
            "Non-CASE computed predicate should use INNER/simple joins, not LEFT JOIN");
    }

    @Test
    void testCaseWhenAggregateNullByAbsence() {
        var sumSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(CASE WHEN R.A > 10 THEN R.B END) AS sum_b FROM R"
        ));
        assertTrue(sumSql.contains("group_1_sum_b"));
        assertTrue(sumSql.contains("SUM(CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END)"));
        assertTrue(sumSql.contains("CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END IS NOT NULL"));

        var groupedSumSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, SUM(CASE WHEN R.B > 0 THEN R.B END) AS sum_b FROM R GROUP BY R.A"
        ));
        assertTrue(groupedSumSql.contains("group_1_sum_b"));
        assertTrue(groupedSumSql.contains("CASE WHEN product_0_R_B.v > 0.0 THEN product_0_R_B.v END IS NOT NULL"));

        var countSql = normalizeWhitespace(translator.translate(
            "SELECT COUNT(CASE WHEN R.A > 10 THEN R.B END) AS cnt FROM R"
        ));
        assertTrue(countSql.contains("COUNT(CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END)"));
        assertFalse(countSql.contains("CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END IS NOT NULL"));
    }

    @Test
    void testArithmeticInAggregates() {
        var sumMulSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(R.A * R.B) AS revenue FROM R"
        ));
        assertTrue(sumMulSql.contains("SUM(product_0_R_A.v * product_0_R_B.v)"));
        assertTrue(sumMulSql.contains("group_1_revenue"));
    }

    @Test
    void testArithmeticConstantPredicate() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE 1 + 2 > 2"
        ));
        assertTrue(sql.contains("SELECT product_0_id.id FROM product_0_id WHERE 1.0 + 2.0 > 2.0"));
        assertFalse(sql.contains("EXISTS (SELECT * FROM product_0_id"));
    }

    @Test
    void testArithmeticExpressionInGroupByReturn() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A + R.B AS total, SUM(R.B) AS sum_b FROM R GROUP BY R.A, R.B"
        ));
        assertTrue(sql.contains("return_2_attr_total"));
        assertTrue(sql.contains("group_1_R_A.v + group_1_R_B.v"));
        assertTrue(sql.contains("return_2_attr_sum_b"));
    }

    @Test
    void testCountWithArithmeticExpression() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT COUNT(R.A + R.B) AS cnt FROM R"
        ));
        assertTrue(sql.contains("COUNT(product_0_R_A.v + product_0_R_B.v)"));
    }

    @Test
    void testArithmeticInHavingWithAggregateAlias() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A, SUM(R.B) AS sum_b FROM R GROUP BY R.A HAVING sum_b * 2 > 10"
        ));
        assertTrue(sql.contains("group_1_sum_b.v * 2.0 > 10.0"));
    }

    @Test
    void testOrderByAndLimit() {
        var orderedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R ORDER BY R.A LIMIT 10"
        ));
        assertTrue(orderedSql.contains("ORDER BY return_1_attr_R_A.v ASC"));
        assertTrue(orderedSql.contains("LIMIT 10"));

        var multiOrderedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, R.B FROM R ORDER BY R.A ASC, R.B DESC LIMIT 5"
        ));
        assertTrue(multiOrderedSql.contains("ORDER BY return_1_attr_R_A.v ASC, return_1_attr_R_B.v DESC"));
        assertTrue(multiOrderedSql.contains("LIMIT 5"));

        var aliasOrderedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, SUM(R.B) AS total FROM R GROUP BY R.A ORDER BY total DESC LIMIT 3"
        ));
        assertTrue(aliasOrderedSql.contains("ORDER BY return_2_attr_total.v DESC"));
        assertTrue(aliasOrderedSql.contains("LIMIT 3"));

        var ordinalOrderedSql = normalizeWhitespace(translator.translate(
            "SELECT DISTINCT R.A FROM R ORDER BY 1 LIMIT 10"
        ));
        assertTrue(ordinalOrderedSql.contains("ORDER BY duplelim_2_attr_R_A.v ASC"));
        assertTrue(ordinalOrderedSql.contains("LIMIT 10"));

        var limitOnlySql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R LIMIT 5"
        ));
        assertFalse(limitOnlySql.contains(" ORDER BY "));
        assertTrue(limitOnlySql.contains("LIMIT 5"));
    }

    @Test
    void testUnsupportedOrderByExpressions() {
        assertThrows(UnsupportedOperationException.class, () ->
            translator.translate("SELECT R.A FROM R ORDER BY R.A + R.B"));
    }

    @Test
    void testChainedCTEs() {
        assertQueryTranslation(
            // language=sql
            """
                WITH c1 AS (SELECT A, B FROM R WHERE B > 5),
                     c2 AS (SELECT A FROM c1 WHERE B = 10)
                SELECT A FROM c2
                """,
            // language=sql
            """
                WITH all_ids_product_2 AS (
                    SELECT R__ID.id || '_0' AS id, R__ID.id AS id1 FROM R__ID AS R__ID
                ),
                product_2_id AS (
                    SELECT id FROM all_ids_product_2
                ),
                product_2_R_A AS (
                    SELECT all_ids_product_2.id, R_A.v FROM all_ids_product_2, R_A WHERE all_ids_product_2.id1 = R_A.id
                ),
                product_2_R_B AS (
                    SELECT all_ids_product_2.id, R_B.v FROM all_ids_product_2, R_B WHERE all_ids_product_2.id1 = R_B.id
                ),
                filter_3_id AS (
                    SELECT product_2_id.id FROM product_2_id WHERE EXISTS (SELECT * FROM product_2_R_B WHERE product_2_R_B.id = product_2_id.id AND product_2_R_B.v > 5.0)
                ),
                filter_3_R_A AS (
                    SELECT product_2_R_A.* FROM product_2_R_A JOIN filter_3_id ON filter_3_id.id = product_2_R_A.id
                ),
                filter_3_R_B AS (
                    SELECT product_2_R_B.* FROM product_2_R_B JOIN filter_3_id ON filter_3_id.id = product_2_R_B.id
                ),
                return_4_id AS (
                    SELECT id FROM filter_3_id
                ),
                return_4_attr_A AS (
                    SELECT id, v FROM filter_3_R_A
                ),
                return_4_attr_B AS (
                    SELECT id, v FROM filter_3_R_B
                ),
                c1__ID AS (
                    SELECT id FROM return_4_id
                ),
                c1_A AS (
                    SELECT id, v FROM return_4_attr_A
                ),
                c1_B AS (
                    SELECT id, v FROM return_4_attr_B
                ),
                all_ids_product_1 AS (
                    SELECT c1__ID.id || '_0' AS id, c1__ID.id AS id1 FROM c1__ID AS c1__ID
                ),
                product_1_id AS (
                    SELECT id FROM all_ids_product_1
                ),
                product_1_c1_A AS (
                    SELECT all_ids_product_1.id, c1_A.v FROM all_ids_product_1, c1_A WHERE all_ids_product_1.id1 = c1_A.id
                ),
                product_1_c1_B AS (
                    SELECT all_ids_product_1.id, c1_B.v FROM all_ids_product_1, c1_B WHERE all_ids_product_1.id1 = c1_B.id
                ),
                filter_5_id AS (
                    SELECT product_1_id.id FROM product_1_id WHERE EXISTS (SELECT * FROM product_1_c1_B WHERE product_1_c1_B.id = product_1_id.id AND product_1_c1_B.v = 10.0)
                ),
                filter_5_c1_A AS (
                    SELECT product_1_c1_A.* FROM product_1_c1_A JOIN filter_5_id ON filter_5_id.id = product_1_c1_A.id
                ),
                return_6_id AS (
                    SELECT id FROM filter_5_id
                ),
                return_6_attr_A AS (
                    SELECT id, v FROM filter_5_c1_A
                ),
                c2__ID AS (
                    SELECT id FROM return_6_id
                ),
                c2_A AS (
                    SELECT id, v FROM return_6_attr_A
                ),
                all_ids_product_0 AS (
                    SELECT c2__ID.id || '_0' AS id, c2__ID.id AS id1 FROM c2__ID AS c2__ID
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_c2_A AS (
                    SELECT all_ids_product_0.id, c2_A.v FROM all_ids_product_0, c2_A WHERE all_ids_product_0.id1 = c2_A.id
                ),
                return_7_id AS (
                    SELECT id FROM product_0_id
                ),
                return_7_attr_A AS (
                    SELECT id, v FROM product_0_c2_A
                )
                SELECT return_7_attr_A.v AS A FROM return_7_id JOIN return_7_attr_A ON return_7_id.id = return_7_attr_A.id;\
                """
        );
    }

    @Test
    void testMultiReferenceCTE() {
        var sql = normalizeWhitespace(translator.translate(
            // language=sql
            """
                WITH cte AS (SELECT A, B FROM R)
                SELECT t1.A FROM cte t1, cte t2 WHERE t1.A = t2.B
                """
        ));
        assertTrue(sql.contains("t1__ID"), "t1 alias should produce t1__ID");
        assertTrue(sql.contains("t2__ID"), "t2 alias should produce t2__ID");
        assertTrue(sql.contains("product_0_t1_A"), "t1.A should be accessible");
        assertTrue(sql.contains("product_0_t2_B"), "t2.B should be accessible");
        assertTrue(sql.contains("product_0_t1_A.v = product_0_t2_B.v"), "Join condition");

        // CTE body should be rendered only once — t1 and t2 should reference the same base CTEs
        assertEquals(1, countOccurrences(sql, "all_ids_product_1 AS"),
            "CTE body should be defined once, not duplicated per reference");
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substring, idx)) != -1) {
            count++;
            idx += substring.length();
        }
        return count;
    }

    @Test
    void testCTEWithAggregation() {
        var sql = normalizeWhitespace(translator.translate(
            // language=sql
            """
                WITH totals AS (SELECT A, SUM(B) AS sum_b FROM R GROUP BY A)
                SELECT A, sum_b FROM totals WHERE sum_b > 100
                """
        ));
        assertTrue(sql.contains("totals__ID"), "CTE should produce totals__ID");
        assertTrue(sql.contains("totals_A"), "CTE should expose A attribute");
        assertTrue(sql.contains("totals_sum_b"), "CTE should expose sum_b aggregate");
        assertTrue(sql.contains("group_"), "CTE body should contain grouping");
        assertTrue(sql.contains("SUM("), "CTE body should contain SUM aggregate");
        assertTrue(sql.contains("sum_b") && sql.contains("> 100.0"),
            "Outer query should filter on sum_b > 100");
    }

    @Test
    void testCTEWithDistinct() {
        var sql = normalizeWhitespace(translator.translate(
            // language=sql
            """
                WITH unique_a AS (SELECT DISTINCT A FROM R)
                SELECT A FROM unique_a
                """
        ));
        assertTrue(sql.contains("unique_a__ID"), "CTE should produce unique_a__ID");
        assertTrue(sql.contains("unique_a_A"), "CTE should expose A attribute");
        assertTrue(sql.contains("duplelim_"), "CTE body should contain DISTINCT elimination");
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
