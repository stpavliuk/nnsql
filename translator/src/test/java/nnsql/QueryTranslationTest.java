package nnsql;

import nnsql.query.QueryTranslator;
import nnsql.query.SchemaRegistry;
import nnsql.query.renderer.sql.SQLIRRenderer;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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
        schemaRegistry.registerTable(
            "lineitem",
            List.of(
                "l_partkey",
                "l_quantity",
                "l_extendedprice",
                "l_discount",
                "l_shipinstruct",
                "l_shipmode"
            )
        );
        schemaRegistry.registerTable(
            "part",
            List.of(
                "p_partkey",
                "p_brand",
                "p_container",
                "p_size"
            )
        );

        translator = new QueryTranslator(schemaRegistry, new SQLIRRenderer());
    }

    @Test
    void testMultipleQueryTranslations() {
        var equalityFilterSql = normalizeWhitespace(translator.translate("SELECT R.A FROM R WHERE R.B = 5"));
        assertTrue(equalityFilterSql.contains("product_0_R_A AS"));
        assertTrue(equalityFilterSql.contains("product_0_R_B AS"));
        assertContainsSql(equalityFilterSql,
            "filter_1_id AS ( SELECT product_0_R_B.id, product_0_R_B.v AS filter_attr_R_B FROM product_0_R_B WHERE (product_0_R_B.v = 5.0) )"
        );
        assertTrue(equalityFilterSql.contains("return_2_attr_R_A"));

        assertQueryTranslation(
            // language=sql
            "SELECT DISTINCT R.A FROM R, S WHERE R.B = S.B",
            // language=sql
            """
                WITH all_ids_product_0 AS (
                    SELECT hash(R__ID.id, S__ID.id, 0) AS id,
                           R__ID.id AS id1,
                           S__ID.id AS id2
                    FROM R__ID AS R__ID
                    JOIN R_B AS _jp0 ON R__ID.id = _jp0.id
                    JOIN S_B AS _jp1 ON _jp0.v = _jp1.v
                    JOIN S__ID AS S__ID ON _jp1.id = S__ID.id
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                    SELECT all_ids_product_0.id, R_A.v
                    FROM all_ids_product_0
                    JOIN R_A ON all_ids_product_0.id1 = R_A.id
                ),
                return_1_id AS (
                    SELECT id FROM product_0_id
                ),
                return_1_attr_R_A AS (
                    SELECT id, v FROM product_0_R_A
                ),
                duplelim_2_id AS (
                    SELECT return_1_id.id
                    FROM return_1_id
                    WHERE NOT EXISTS (SELECT * FROM return_1_id R1 WHERE R1.id < return_1_id.id AND (EXISTS (SELECT * FROM return_1_attr_R_A TEMP1, return_1_attr_R_A TEMP2 WHERE TEMP1.id = return_1_id.id AND TEMP2.id = R1.id AND TEMP1.v = TEMP2.v) OR NOT EXISTS (SELECT * FROM return_1_attr_R_A WHERE return_1_attr_R_A.id = return_1_id.id OR return_1_attr_R_A.id = R1.id)))
                ),
                duplelim_2_attr_R_A AS (
                    SELECT return_1_attr_R_A.*
                    FROM return_1_attr_R_A JOIN duplelim_2_id ON duplelim_2_id.id = return_1_attr_R_A.id
                )
                SELECT duplelim_2_attr_R_A.v AS R_A
                FROM duplelim_2_id
                LEFT JOIN duplelim_2_attr_R_A ON duplelim_2_id.id = duplelim_2_attr_R_A.id;\
                """
        );

        assertQueryTranslation(
            // language=sql
            "SELECT R.A, SUM(S.C) AS total FROM R, S WHERE R.B = S.B GROUP BY R.A",
            // language=sql
            """
                    WITH all_ids_product_0 AS (
                    SELECT hash(R__ID.id, S__ID.id, 0) AS id,
                           R__ID.id AS id1,
                           S__ID.id AS id2
                    FROM R__ID AS R__ID
                    JOIN R_B AS _jp0 ON R__ID.id = _jp0.id
                    JOIN S_B AS _jp1 ON _jp0.v = _jp1.v
                    JOIN S__ID AS S__ID ON _jp1.id = S__ID.id
                ),
                product_0_id AS (
                    SELECT id FROM all_ids_product_0
                ),
                product_0_R_A AS (
                    SELECT all_ids_product_0.id, R_A.v
                    FROM all_ids_product_0
                    JOIN R_A ON all_ids_product_0.id1 = R_A.id
                ),
                product_0_S_C AS (
                    SELECT all_ids_product_0.id, S_C.v
                    FROM all_ids_product_0
                    JOIN S_C ON all_ids_product_0.id2 = S_C.id
                ),
                grouped_group_1 AS (
                    SELECT MIN(product_0_id.id) AS id,
                           CASE WHEN product_0_R_A.id IS NULL THEN 0 ELSE 1 END AS group_key_0_present,
                           product_0_R_A.v AS group_key_0_value,
                           SUM(product_0_S_C.v) AS total
                    FROM product_0_id
                    LEFT JOIN product_0_R_A ON product_0_R_A.id = product_0_id.id
                    LEFT JOIN product_0_S_C ON product_0_S_C.id = product_0_id.id
                    GROUP BY CASE WHEN product_0_R_A.id IS NULL THEN 0 ELSE 1 END, product_0_R_A.v
                ),
                group_1_id AS (
                    SELECT grouped_group_1.id
                    FROM grouped_group_1
                ),
                group_1_R_A AS (
                    SELECT grouped_group_1.id, grouped_group_1.group_key_0_value AS v
                    FROM grouped_group_1
                    WHERE grouped_group_1.group_key_0_present = 1
                ),
                group_1_total AS (
                    SELECT grouped_group_1.id, grouped_group_1.total AS v
                    FROM grouped_group_1
                    WHERE grouped_group_1.total IS NOT NULL
                ),
                return_2_id AS (
                SELECT id FROM group_1_id
                ),
                return_2_attr_R_A AS (
                    SELECT id, v FROM group_1_R_A
                ),
                return_2_attr_total AS (
                    SELECT id, v FROM group_1_total
                )
                SELECT return_2_attr_R_A.v AS R_A, return_2_attr_total.v AS total
                    FROM return_2_id
                    LEFT JOIN return_2_attr_R_A ON return_2_id.id = return_2_attr_R_A.id
                    LEFT JOIN return_2_attr_total ON return_2_id.id = return_2_attr_total.id;\
                """
        );

        var isNullSql = normalizeWhitespace(translator.translate("SELECT T.D FROM T WHERE T.E IS NULL"));
        assertContainsSql(isNullSql, "product_0_id AS ( SELECT T__ID.id FROM T__ID AS T__ID )");
        assertContainsSql(isNullSql, "product_0_T_D AS ( SELECT T_D.id, T_D.v FROM T_D )");
        assertContainsSql(isNullSql, "product_0_T_E AS ( SELECT T_E.id, T_E.v FROM T_E )");
        assertTrue(isNullSql.contains("NOT EXISTS (SELECT * FROM product_0_T_E WHERE product_0_T_E.id = product_0_id.id)"));
        assertTrue(isNullSql.contains("return_2_attr_T_D"));

        var columnComparisonSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A > R.B"
        ));
        assertTrue(columnComparisonSql.contains("product_0_R_A AS"));
        assertTrue(columnComparisonSql.contains("product_0_R_B AS"));
        assertContainsSql(columnComparisonSql,
            "filter_1_id AS ( SELECT product_0_R_A.id, product_0_R_A.v AS filter_attr_R_A, product_0_R_B.v AS filter_attr_R_B FROM product_0_R_A JOIN product_0_R_B ON product_0_R_B.id = product_0_R_A.id WHERE (product_0_R_A.v > product_0_R_B.v) )"
        );
        assertTrue(columnComparisonSql.contains("return_2_attr_R_A"));
    }

    @Test
    void testBetweenPredicates() {
        var betweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B BETWEEN 10 AND 20"
        ));
        assertTrue(betweenSql.contains("product_0_R_B.v >= 10.0"));
        assertTrue(betweenSql.contains("product_0_R_B.v <= 20.0"));
        assertFalse(betweenSql.contains("EXISTS (SELECT * FROM product_0_R_B"));

        var notBetweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B NOT BETWEEN 10 AND 20"
        ));
        assertTrue(notBetweenSql.contains("product_0_R_B.v < 10.0"));
        assertTrue(notBetweenSql.contains("product_0_R_B.v > 20.0"));
        assertFalse(notBetweenSql.contains("EXISTS (SELECT * FROM product_0_R_B"));

        var columnBetweenSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A BETWEEN R.B AND R.B"
        ));
        assertTrue(columnBetweenSql.contains("product_0_R_A.v >= product_0_R_B.v"));
        assertTrue(columnBetweenSql.contains("product_0_R_A.v <= product_0_R_B.v"));
        assertFalse(columnBetweenSql.contains("WHERE EXISTS (SELECT * FROM product_0_R_A, product_0_R_B"));
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
        assertFalse(inSql.contains(
            "EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id"
        ));

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
    void testLikePredicates() {
        var likeSql = normalizeWhitespace(translator.translate(
            "SELECT customer.c_custkey FROM customer WHERE customer.c_mktsegment LIKE 'AUTO%'"
        ));
        assertTrue(likeSql.contains("product_0_customer_c_mktsegment.v LIKE 'AUTO%'"));

        var notLikeSql = normalizeWhitespace(translator.translate(
            "SELECT customer.c_custkey FROM customer WHERE customer.c_mktsegment NOT LIKE 'AUTO%'"
        ));
        assertTrue(notLikeSql.contains("product_0_customer_c_mktsegment.v NOT LIKE 'AUTO%'"));

        var caseWhenLikeSql = normalizeWhitespace(translator.translate(
            "SELECT CASE WHEN customer.c_mktsegment LIKE 'AUTO%' THEN 1 ELSE 0 END AS flag FROM customer"
        ));
        assertTrue(caseWhenLikeSql.contains(
            "CASE WHEN product_0_customer_c_mktsegment.v LIKE 'AUTO%' THEN 1.0 ELSE 0.0 END"
        ));
    }

    @Test
    void testInPredicateWithTrailingAndCondition() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A = 5 AND R.B IN (1, 2, 3) AND R.B > 0"
        ));

        assertTrue(sql.contains("product_0_R_A.v = 5.0"));
        assertTrue(sql.contains("product_0_R_B.v = 1.0"));
        assertTrue(sql.contains("product_0_R_B.v = 2.0"));
        assertTrue(sql.contains("product_0_R_B.v = 3.0"));
        assertTrue(sql.contains("product_0_R_B.v > 0.0"));
        assertTrue(sql.contains(
            "((product_0_R_B.v = 1.0) OR (product_0_R_B.v = 2.0) OR (product_0_R_B.v = 3.0))"
        ));
    }

    @Test
    void testNestedAndInlinesSafeSharedColumnPredicates() {
        var sql = normalizeWhitespace(translator.translate(
            """
                SELECT sum(l_extendedprice * (1 - l_discount)) AS revenue
                FROM lineitem, part
                WHERE p_partkey = l_partkey
                  AND p_brand = 'Brand#12'
                  AND p_container in ('SM CASE', 'SM BOX', 'SM PACK', 'SM PKG')
                  AND l_quantity >= 1
                  AND l_quantity <= 11
                  AND p_size BETWEEN 1 AND 5
                  AND l_shipmode in ('AIR', 'AIR REG')
                  AND l_shipinstruct = 'DELIVER IN PERSON'
                """
        ));

        assertTrue(sql.contains(
            "product_0_lineitem_l_quantity.v >= 1.0"
        ));
        assertTrue(sql.contains(
            "product_0_lineitem_l_quantity.v <= 11.0"
        ));
        assertFalse(sql.contains(
            "EXISTS (SELECT * FROM product_0_lineitem_l_quantity WHERE product_0_lineitem_l_quantity.id = product_0_id.id AND product_0_lineitem_l_quantity.v >= 1.0)"
        ));
        assertFalse(sql.contains(
            "EXISTS (SELECT * FROM product_0_part_p_container WHERE product_0_part_p_container.id = product_0_id.id"
        ));
        assertFalse(sql.contains(
            "EXISTS (SELECT * FROM product_0_lineitem_l_shipmode WHERE product_0_lineitem_l_shipmode.id = product_0_id.id"
        ));
    }

    @Test
    void testOrBranchesWithSharedRequiredColumnsInlineSafely() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE (R.A = 1 AND R.B = 2) OR (R.A = 3 AND R.B = 4)"
        ));

        assertTrue(sql.contains("product_0_R_A.v = 1.0"));
        assertTrue(sql.contains("product_0_R_B.v = 2.0"));
        assertTrue(sql.contains("product_0_R_A.v = 3.0"));
        assertTrue(sql.contains("product_0_R_B.v = 4.0"));
        assertFalse(sql.contains(
            "EXISTS (SELECT * FROM product_0_R_A WHERE product_0_R_A.id = product_0_id.id"
        ));
        assertFalse(sql.contains(
            "EXISTS (SELECT * FROM product_0_R_B WHERE product_0_R_B.id = product_0_id.id"
        ));
    }

    @Test
    void testExistsPredicatesWithUncorrelatedSubqueries() {
        var existsSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE EXISTS (SELECT * FROM S WHERE S.B > 10)"
        ));
        assertTrue(existsSql.matches(".*WHERE EXISTS \\(SELECT \\* FROM return_\\d+_id\\).*"));
        assertTrue(existsSql.contains("S_B.v > 10.0"));

        var notExistsSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE NOT EXISTS (SELECT * FROM S WHERE S.B > 10)"
        ));
        assertTrue(notExistsSql.matches(".*WHERE NOT EXISTS \\(SELECT \\* FROM return_\\d+_id\\).*"));
        assertTrue(notExistsSql.contains("S_B.v > 10.0"));
    }

    @Test
    void testInSubqueryWithUncorrelatedSubquery() {
        var inSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B IN (SELECT S.B FROM S WHERE S.C > 5)"
        ));
        assertTrue(inSql.matches(".*product_0_R_B\\.v IN \\(SELECT v FROM return_\\d+_attr_S_B\\).*"));
        assertTrue(inSql.contains("S_C.v > 5.0"));

        var inWithAndSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B IN (SELECT S.B FROM S) AND R.A > 10"
        ));
        assertTrue(inWithAndSql.matches(".*product_0_R_B\\.v IN \\(SELECT v FROM return_\\d+_attr_S_B\\).*"));
        assertTrue(inWithAndSql.contains("product_0_R_A.v > 10.0"));
    }

    @Test
    void testCorrelatedScalarSubqueryWithAggregate() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R, S WHERE R.B = S.B AND S.C = (SELECT MIN(T.D) FROM T WHERE T.E = R.A)"
        ));

        assertTrue(sql.contains("SELECT MIN("));
        assertTrue(sql.contains("T_E AS corr_agg_attr_"));
        assertTrue(sql.contains("WHERE corr_agg_attr_"));
        assertTrue(sql.contains("product_0_R_A.v"));
    }

    @Test
    void testJoinPushdownInsideCorrelatedScalarSubquery() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A = (SELECT MIN(S.C) FROM S, T WHERE S.B = T.D AND T.E = R.B)"
        ));

        assertTrue(sql.contains("corr_subquery_value"));
        assertTrue(sql.contains("corr_subquery_attr_1"));
    }

    @Test
    void testCorrelatedExists() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE EXISTS (SELECT * FROM S WHERE S.B = R.B AND S.C > 10)"
        ));

        assertTrue(sql.contains("filter_"), "should have inner filter for local predicate S.C > 10");
        assertTrue(sql.contains("R_B.v = "), "should join on correlation attribute R_B");
        assertTrue(sql.contains("S_B.v"), "should reference inner correlation attribute S_B");
        assertTrue(sql.contains("S_C.v > 10.0"), "should have local predicate in inner subquery");
    }

    @Test
    void testCorrelatedNotExists() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE NOT EXISTS (SELECT * FROM S WHERE S.B = R.B)"
        ));

        assertTrue(sql.contains("NOT EXISTS"), "should have NOT EXISTS");
        assertTrue(sql.contains("R_B.v = "), "should join on correlation attribute R_B");
        assertTrue(sql.contains("S_B.v"), "should reference inner correlation attribute S_B");
    }

    @Test
    void testCorrelatedExistsWithInequality() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE EXISTS (SELECT * FROM S WHERE S.B = R.B AND S.C <> R.A)"
        ));

        assertTrue(sql.contains("R_B.v = "), "should join on equality correlation");
        assertTrue(sql.contains("!="), "should have inequality correlation");
    }

    @Test
    void testTopLevelJoinLocalPredicatesArePushedIntoBaseRelations() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R, S WHERE R.B = S.B AND R.A > 10 AND S.C < 5"
        ));

        assertTrue(sql.contains("return_3_id AS R__ID"));
        assertTrue(sql.contains("return_6_id AS S__ID"));
        assertFalse(sql.contains("product_0_R_A.v > 10.0"));
        assertFalse(sql.contains("product_0_S_C.v < 5.0"));
    }

    @Test
    void testTopLevelJoinInSubqueryPredicatesArePushedIntoBaseRelations() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R, S WHERE R.B = S.B AND S.C IN (SELECT T.D FROM T WHERE T.E > 0)"
        ));

        assertTrue(sql.contains("return_6_id AS S__ID"));
        assertTrue(sql.contains("product_1_S_C.v IN (SELECT v FROM return_5_attr_T_D)"));
        assertTrue(sql.contains("T_E.v > 0.0"));
    }

    @Test
    void testRepeatedJoinAttributesAreJoinedOnceInProductCte() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R, S, T WHERE R.B = S.B AND R.B = T.D"
        ));

        assertEquals(1, Pattern.compile("JOIN R_B AS ").matcher(sql).results().count());
    }

    @Test
    void testOrBranchesInferLocalPushdownPredicates() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R, S WHERE (R.A > 10 AND S.C = 1) OR (R.A > 20 AND S.C = 2)"
        ));

        assertTrue(sql.contains("return_3_id AS R__ID"));
        assertTrue(sql.contains("return_6_id AS S__ID"));
        assertTrue(sql.contains("product_0_R_A.v > 10.0") || sql.contains("product_0_R_A.v > 20.0"));
    }

    @Test
    void testNestedSubqueriesDoNotWrapRepeatedBaseAliases() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.B = (SELECT MIN(R.B) FROM R WHERE R.A > 0)"
        ));

        assertFalse(sql.contains("R__ID AS ( SELECT id FROM return_"));
        assertTrue(sql.contains("group_"));
    }

    @Test
    void testNestedSubqueriesPushSafeLocalPredicatesWhenAliasesDoNotCollide() {
        var sql = normalizeWhitespace(translator.translate(
            """
                SELECT q.p_partkey
                FROM (
                    SELECT p_partkey
                    FROM part, lineitem
                    WHERE p_partkey = l_partkey
                      AND p_brand LIKE 'Brand%'
                ) q
                """
        ));

        assertTrue(sql.contains("filter_3_part_p_partkey"));
        assertTrue(sql.contains("LIKE 'Brand%'"));
    }

    @Test
    void translatesExplicitInnerJoinSyntax() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R INNER JOIN S ON R.B = S.B"
        ));

        assertTrue(sql.contains("R_B AS _jp0"));
        assertTrue(sql.contains("S_B AS _jp1"));
        assertTrue(sql.contains("_jp0.v = _jp1.v"));
        assertTrue(sql.contains("return_1_attr_R_A"));
    }

    @Test
    void translatesExplicitInnerJoinAliases() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT r.A FROM R r JOIN S s ON r.B = s.B"
        ));

        assertTrue(sql.contains("R_B AS _jp0"));
        assertTrue(sql.contains("S_B AS _jp1"));
        assertTrue(sql.contains("_jp0.v = _jp1.v"));
        assertTrue(sql.contains("return_1_attr_r_A"));
    }

    @Test
    void rejectsOuterJoinSyntaxUntilRowPreservingSemanticsExist() {
        var error = assertThrows(UnsupportedOperationException.class, () -> translator.translate(
            "SELECT customer.c_custkey FROM customer LEFT JOIN S ON customer.c_custkey = S.B"
        ));

        assertEquals(
            "Outer JOIN syntax is not supported yet; it needs row-preserving 6NF semantics",
            error.getMessage()
        );
    }

    @Test
    void testTPCHSchema() {
        var simpleTpchSql = normalizeWhitespace(translator.translate(
            """
                SELECT * FROM customer
                WHERE c_acctbal > 5000
                """
        ));
        assertTrue(simpleTpchSql.contains("product_0_customer_c_acctbal"));
        assertContainsSql(simpleTpchSql,
            "filter_1_id AS ( SELECT product_0_customer_c_acctbal.id, product_0_customer_c_acctbal.v AS filter_attr_customer_c_acctbal FROM product_0_customer_c_acctbal WHERE (product_0_customer_c_acctbal.v > 5000.0) )"
        );
        assertTrue(simpleTpchSql.endsWith("SELECT * FROM return_2_id;"));

        var groupedTpchSql = normalizeWhitespace(translator.translate(
            """
                SELECT c_mktsegment AS mkt, COUNT(c_custkey) AS seg FROM customer
                WHERE c_nationkey = 15 AND c_acctbal > (SELECT AVG(c_acctbal) AS avg_accball FROM customer
                    WHERE c_acctbal > 0.00 AND c_nationkey = 15)
                GROUP BY c_mktsegment
                HAVING seg > 500
                """
        ));
        assertContainsSql(groupedTpchSql,
            "filter_3_id AS ( SELECT product_2_customer_c_acctbal.id, product_2_customer_c_acctbal.v AS filter_attr_customer_c_acctbal, product_2_customer_c_nationkey.v AS filter_attr_customer_c_nationkey FROM product_2_customer_c_acctbal JOIN product_2_customer_c_nationkey ON product_2_customer_c_nationkey.id = product_2_customer_c_acctbal.id WHERE ((product_2_customer_c_acctbal.v > 0.0) AND (product_2_customer_c_nationkey.v = 15.0)) )"
        );
        assertTrue(groupedTpchSql.contains("return_5_attr_avg_accball"));
        assertTrue(groupedTpchSql.contains("product_0_customer_c_acctbal.v > (SELECT v FROM return_5_attr_avg_accball)"));
        assertTrue(groupedTpchSql.contains("group_6_seg"));
        assertTrue(groupedTpchSql.contains("aggfilter_7_id"));
    }

    @Test
    void testCTE() {
        var sql = normalizeWhitespace(translator.translate(
            "WITH ctr AS (SELECT A, B FROM R WHERE B > 5) SELECT A FROM ctr WHERE B = 10"
        ));
        assertTrue(sql.contains("product_0_ctr_B"));
        assertContainsSql(sql,
            "filter_2_id AS ( SELECT product_1_R_B.id, product_1_R_B.v AS filter_attr_R_B FROM product_1_R_B WHERE (product_1_R_B.v > 5.0) )"
        );
        assertContainsSql(sql,
            "filter_4_id AS ( SELECT product_0_ctr_B.id, product_0_ctr_B.v AS filter_attr_ctr_B FROM product_0_ctr_B WHERE (product_0_ctr_B.v = 10.0) )"
        );
        assertTrue(sql.contains("return_5_attr_A"));
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
    void testScalarFunctionExpression() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT strftime('%Y', customer.c_comment) AS comment_year FROM customer"
        )).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("strftime('%y', product_0_customer_c_comment.v) as v"));
        assertTrue(sql.contains("return_1_attr_comment_year"));

        var substrSql = normalizeWhitespace(translator.translate(
            "SELECT SUBSTR(customer.c_phone, 1, 2) AS cntrycode FROM customer"
        ));
        assertTrue(substrSql.contains("SUBSTR(product_0_customer_c_phone.v, 1, 2) AS v"));
        assertFalse(substrSql.contains("SUBSTR(product_0_customer_c_phone.v, 1.0, 2.0)"));
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
        assertFalse(addSql.contains("LEFT JOIN product_0_"),
            "Non-CASE BinaryOp should not use LEFT JOIN for source attribute access");

        var castSql = normalizeWhitespace(translator.translate(
            "SELECT CAST(R.A AS INTEGER) AS a_int FROM R"
        ));
        assertFalse(castSql.contains("LEFT JOIN product_0_"),
            "Non-CASE Cast should not use LEFT JOIN for source attribute access");

        var computedWhereSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A + R.B > 5"
        ));
        assertTrue(computedWhereSql.contains("WHERE EXISTS (SELECT * FROM"));
        assertTrue(computedWhereSql.contains("product_0_R_A.v + product_0_R_B.v > 5.0"));
        assertFalse(computedWhereSql.contains("LEFT JOIN product_0_"),
            "Non-CASE computed predicate should not use LEFT JOIN for source attribute access");
    }

    @Test
    void testCaseWhenAggregateNullByAbsence() {
        var sumSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(CASE WHEN R.A > 10 THEN R.B END) AS sum_b FROM R"
        ));
        assertTrue(sumSql.contains("group_1_sum_b"));
        assertTrue(sumSql.contains("SUM(CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END)"));
        assertFalse(sumSql.contains("CASE WHEN product_0_R_A.v > 10.0 THEN product_0_R_B.v END IS NOT NULL"));

        var groupedSumSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, SUM(CASE WHEN R.B > 0 THEN R.B END) AS sum_b FROM R GROUP BY R.A"
        ));
        assertTrue(groupedSumSql.contains("group_1_sum_b"));
        assertFalse(groupedSumSql.contains("CASE WHEN product_0_R_B.v > 0.0 THEN product_0_R_B.v END IS NOT NULL"));

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

        var sumNestedSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(R.A * (1 - R.B)) AS revenue FROM R"
        ));
        assertTrue(sumNestedSql.contains("SUM(product_0_R_A.v * (1.0 - product_0_R_B.v))"));

        var sumChainedSql = normalizeWhitespace(translator.translate(
            "SELECT SUM(R.A * (1 - R.B) * (1 + R.B)) AS charge FROM R"
        ));
        assertTrue(sumChainedSql.contains(
            "SUM(product_0_R_A.v * (1.0 - product_0_R_B.v) * (1.0 + product_0_R_B.v))"
        ));
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
    void testLiteralComparisonsAreRenderedNotEvaluated() {
        var numericSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE 1 < 2"
        ));
        assertTrue(numericSql.contains("SELECT product_0_id.id FROM product_0_id WHERE (1.0 < 2.0)"));
        assertFalse(numericSql.contains("WHERE TRUE"));

        var stringSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE 'a' = 'a'"
        ));
        assertTrue(stringSql.contains("SELECT product_0_id.id FROM product_0_id WHERE ('a' = 'a')"));

        var negatedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE NOT (1 < 2)"
        ));
        assertTrue(negatedSql.contains("SELECT product_0_id.id FROM product_0_id WHERE (NOT (1.0 < 2.0))"));
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
    void testGroupByColumnAliasResolution() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A AS alias_a, SUM(R.B) AS total FROM R GROUP BY alias_a"
        ));
        assertTrue(sql.contains("group_1_R_A"));
        assertTrue(sql.contains("return_2_attr_alias_a"));
        assertTrue(sql.contains("return_2_attr_total"));
    }

    @Test
    void testGroupByComputedAliasResolutionFailure() {
        var exception = assertThrows(UnsupportedOperationException.class, () ->
            translator.translate("SELECT R.A + R.B AS total FROM R GROUP BY total")
        );
        assertTrue(exception.getMessage().contains("GROUP BY alias 'total'"));
    }

    @Test
    void testGroupByExpressionFailure() {
        var exception = assertThrows(UnsupportedOperationException.class, () ->
            translator.translate("SELECT R.A + R.B AS total, COUNT(*) AS cnt FROM R GROUP BY R.A + R.B")
        );
        assertTrue(exception.getMessage().contains("GROUP BY expression"));
        assertTrue(exception.getMessage().contains("only simple column references"));
    }

    @Test
    void testCountWithArithmeticExpression() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT COUNT(R.A + R.B) AS cnt FROM R"
        ));
        assertTrue(sql.contains("COUNT(product_0_R_A.v + product_0_R_B.v)"));
    }

    @Test
    void testCountStar() {
        var groupedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, COUNT(*) AS cnt FROM R GROUP BY R.A"
        ));
        assertTrue(groupedSql.contains("COUNT(1)"));
        assertTrue(groupedSql.contains("group_1_cnt"));

        var ungroupedSql = normalizeWhitespace(translator.translate(
            "SELECT COUNT(*) AS cnt FROM R"
        ));
        assertTrue(ungroupedSql.contains("COUNT(1)"));
        assertTrue(ungroupedSql.contains("COALESCE(MIN(product_0_id.id), 0) AS id"));
        assertTrue(ungroupedSql.contains("group_1_cnt"));

        var mixedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, COUNT(*) AS cnt, SUM(R.B) AS total FROM R GROUP BY R.A"
        ));
        assertTrue(mixedSql.contains("COUNT(1)"));
        assertTrue(mixedSql.contains("SUM(product_0_R_B.v)"));
        assertTrue(mixedSql.contains("group_1_cnt"));
        assertTrue(mixedSql.contains("group_1_total"));
    }

    @Test
    void testCountDistinct() {
        var groupedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, COUNT(DISTINCT R.B) AS cnt FROM R GROUP BY R.A"
        ));
        assertTrue(groupedSql.contains("COUNT(DISTINCT product_0_R_B.v)"));
        assertTrue(groupedSql.contains("group_1_cnt"));
    }

    @Test
    void testGroupByUsesBothNullEquality() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A, COUNT(*) AS cnt FROM R GROUP BY R.A"
        ));

        assertTrue(sql.contains(
            "CASE WHEN product_0_R_A.id IS NULL THEN 0 ELSE 1 END AS group_key_0_present"
        ));
        assertTrue(sql.contains(
            "GROUP BY CASE WHEN product_0_R_A.id IS NULL THEN 0 ELSE 1 END, product_0_R_A.v"
        ));
    }

    @Test
    void testArithmeticInHavingWithAggregateAlias() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A, SUM(R.B) AS sum_b FROM R GROUP BY R.A HAVING sum_b * 2 > 10"
        ));
        assertTrue(sql.contains("group_1_sum_b.v * 2.0 > 10.0"));
    }

    @Test
    void testComputedAggregateExpressionInSelect() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT SUM(R.B) * 2 AS total FROM R"
        ));

        assertTrue(sql.contains("SUM(product_0_R_B.v) AS agg_expr_1"));
        assertTrue(sql.contains("group_1_agg_expr_1.v * 2.0 AS v"));
        assertTrue(sql.contains("return_2_attr_total"));
    }

    @Test
    void testHavingWithRawAggregateExpression() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R GROUP BY R.A HAVING SUM(R.B * 2) > 10"
        ));

        assertTrue(sql.contains("SUM(product_0_R_B.v * 2.0) AS agg_expr_1"));
        assertTrue(sql.contains("group_1_agg_expr_1.v > 10.0"));
    }

    @Test
    void testDirectGroupedInSubqueryUsesInnerJoinForNullRejectingAggregate() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A IN (SELECT S.B FROM S GROUP BY S.B HAVING SUM(S.C) > 10)"
        ));

        assertTrue(sql.contains(
            "SELECT S_B.v FROM S_B JOIN S_C ON S_C.id = S_B.id GROUP BY S_B.v HAVING SUM(S_C.v) > 10.0"
        ));
    }

    @Test
    void testDirectGroupedInSubqueryKeepsLeftJoinForCountAggregate() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT R.A FROM R WHERE R.A IN (SELECT S.B FROM S GROUP BY S.B HAVING COUNT(S.C) = 0)"
        ));

        assertTrue(sql.contains(
            "SELECT S_B.v FROM S_B LEFT JOIN S_C ON S_C.id = S_B.id GROUP BY S_B.v HAVING COUNT(S_C.v) = 0.0"
        ));
    }

    @Test
    void testProductReusesProjectedSubqueryJoinAttribute() {
        var sql = normalizeWhitespace(translator.translate(
            "SELECT sq.S_B FROM R, (SELECT S.B FROM S WHERE S.C > 0) sq WHERE R.B = sq.S_B GROUP BY sq.S_B"
        ));

        assertTrue(sql.contains("sq__ID.id AS id2, _jp0.v AS join_attr_2_S_B"));
        assertTrue(sql.contains(
            "product_0_sq_S_B AS (SELECT all_ids_product_0.id, all_ids_product_0.join_attr_2_S_B AS v FROM all_ids_product_0)"
        ));
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
    void testPostgresCompatibleRendererUsesUuidCompositeIds() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));
        schemaRegistry.registerTable("S", List.of("B", "C"));

        var postgresTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            "SELECT DISTINCT R.A FROM R, S WHERE R.B = S.B"
        ));

        assertTrue(sql.contains(
            "CAST(md5(concat_ws('|', CAST(R__ID.id AS TEXT), CAST(S__ID.id AS TEXT), '0')) AS UUID) AS id"
        ));
        assertFalse(sql.contains("hash("));
    }

    @Test
    void testPostgresCompatibleRendererRewritesStrftimeYear() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("customer", List.of("c_comment"));

        var postgresTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            "SELECT strftime('%Y', customer.c_comment) AS comment_year FROM customer"
        )).toLowerCase(Locale.ROOT);

        assertTrue(sql.contains("to_char(product_0_customer_c_comment.v, 'yyyy') as v"));
        assertFalse(sql.contains("strftime("));
    }

    @Test
    void testPostgresCompatibleRendererCastsUuidRepresentativeIds() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));

        var postgresTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            "SELECT R.A, COUNT(*) AS count_order FROM R GROUP BY R.A"
        ));

        assertTrue(sql.contains("CAST(MIN(CAST(product_0_id.id AS TEXT)) AS UUID) AS id"));
        assertFalse(sql.contains("MIN(product_0_id.id) AS id"));
    }

    @Test
    void testPostgresCompatibleRendererUsesUuidGlobalAggregateId() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));

        var postgresTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            "SELECT COUNT(*) AS cnt FROM R"
        ));

        assertTrue(sql.contains(
            "COALESCE(CAST(MIN(CAST(product_0_id.id AS TEXT)) AS UUID), "
                + "CAST('00000000-0000-0000-0000-000000000000' AS UUID)) AS id"
        ));
    }

    @Test
    void testPostgresCompatibleRendererMaterializesSharedCtes() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable("R", List.of("A", "B"));

        var postgresTranslator = new QueryTranslator(schemaRegistry, SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            "SELECT R.A FROM R WHERE R.B > 10"
        ));

        assertFalse(sql.contains("product_0_R_A AS MATERIALIZED"));
        assertTrue(sql.contains("filter_1_id AS MATERIALIZED"));
    }

    @Test
    void testPostgresCompatibleRendererUsesDirectFilteredSingleTableGroup() {
        var postgresTranslator = new QueryTranslator(schemaRegistryWithLineitemQ1Columns(), SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            // language=sql
            """
                SELECT l_returnflag,
                       l_linestatus,
                       SUM(l_quantity) AS sum_qty,
                       SUM(l_extendedprice * (1 - l_discount)) AS sum_disc_price,
                       COUNT(*) AS count_order
                FROM lineitem
                WHERE l_shipdate <= '1998-09-02'
                GROUP BY l_returnflag, l_linestatus
                ORDER BY l_returnflag, l_linestatus
                """
        ));

        assertTrue(sql.contains("FROM lineitem_l_shipdate AS direct_group_attr_0"));
        assertTrue(sql.contains("LEFT JOIN lineitem_l_returnflag AS direct_group_attr_1"));
        assertTrue(sql.contains("LEFT JOIN lineitem_l_linestatus AS direct_group_attr_2"));
        assertTrue(sql.contains("GROUP BY direct_group_attr_1.v, direct_group_attr_2.v"));
        assertTrue(sql.contains("SUM(direct_group_attr_4.v * (1.0 - direct_group_attr_5.v)) AS sum_disc_price"));
        assertFalse(sql.contains("product_0_"));
        assertFalse(sql.contains("filter_1_"));
    }

    @Test
    void testPostgresCompatibleRendererInlinesQ19StyleRevenueQuery() {
        var postgresTranslator = new QueryTranslator(schemaRegistryWithTpchTables(), SQLIRRenderer.postgresCompatible());
        var sql = normalizeWhitespace(postgresTranslator.translate(
            // language=sql
            """
                SELECT SUM(l_extendedprice * (1 - l_discount)) AS revenue
                FROM lineitem, part
                WHERE p_partkey = l_partkey
                  AND (
                    (
                      p_brand = 'Brand#12'
                      AND p_container = 'SM CASE'
                      AND l_quantity >= 1
                      AND l_quantity <= 11
                      AND p_size >= 1
                      AND p_size <= 5
                      AND l_shipmode = 'AIR'
                      AND l_shipinstruct = 'DELIVER IN PERSON'
                    )
                    OR
                    (
                      p_brand = 'Brand#23'
                      AND p_container = 'MED BAG'
                      AND l_quantity >= 10
                      AND l_quantity <= 20
                      AND p_size >= 1
                      AND p_size <= 10
                      AND l_shipmode = 'AIR'
                      AND l_shipinstruct = 'DELIVER IN PERSON'
                    )
                  )
                """
        ));

        assertTrue(sql.contains("all_ids_product_0 AS"));
        assertFalse(sql.contains("AS MATERIALIZED"));
    }

    @Test
    void testUnsupportedOrderByExpressions() {
        assertThrows(UnsupportedOperationException.class, () ->
            translator.translate("SELECT R.A FROM R ORDER BY R.A + R.B"));
    }

    @Test
    void testChainedCTEs() {
        var sql = normalizeWhitespace(translator.translate(
            """
                WITH c1 AS (SELECT A, B FROM R WHERE B > 5),
                     c2 AS (SELECT A FROM c1 WHERE B = 10)
                SELECT A FROM c2
                """
        ));
        assertTrue(sql.contains("product_2_R_B"));
        assertTrue(sql.contains("product_1_c1_B"));
        assertContainsSql(sql,
            "filter_3_id AS ( SELECT product_2_R_B.id, product_2_R_B.v AS filter_attr_R_B FROM product_2_R_B WHERE (product_2_R_B.v > 5.0) )"
        );
        assertContainsSql(sql,
            "filter_5_id AS ( SELECT product_1_c1_B.id, product_1_c1_B.v AS filter_attr_c1_B FROM product_1_c1_B WHERE (product_1_c1_B.v = 10.0) )"
        );
        assertTrue(sql.contains("return_7_attr_A"));
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
        assertTrue(sql.matches(".*_jp\\d+\\.v = _jp\\d+\\.v.*"), "Join predicate should be pushed into product");

        // CTE body should be rendered only once — t1 and t2 should reference the same base CTEs
        assertEquals(1, countOccurrences(sql, "product_1_id AS"),
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

    @Test
    void testAliaslessComputedExpressions() {
        var singleExprSql = normalizeWhitespace(translator.translate(
            "SELECT (R.A + R.B) FROM R"
        ));
        assertTrue(singleExprSql.contains("return_1_attr_expr_1"));
        assertTrue(singleExprSql.contains("product_0_R_A.v + product_0_R_B.v"));

        var secondPositionSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, (R.A * R.B) FROM R"
        ));
        assertTrue(secondPositionSql.contains("return_1_attr_expr_2"));
        assertTrue(secondPositionSql.contains("product_0_R_A.v * product_0_R_B.v"));

        var groupedSql = normalizeWhitespace(translator.translate(
            "SELECT R.A, (R.A + R.B) FROM R GROUP BY R.A, R.B"
        ));
        assertTrue(groupedSql.contains("return_2_attr_expr_2"));
        assertTrue(groupedSql.contains("group_1_R_A.v + group_1_R_B.v"));

        var explicitAliasSql = normalizeWhitespace(translator.translate(
            "SELECT (R.A + R.B) AS total FROM R"
        ));
        assertTrue(explicitAliasSql.contains("return_1_attr_total"));
        assertFalse(explicitAliasSql.contains("expr_1"));
    }

    private void assertQueryTranslation(String inputQuery, String expectedOutput) {
        String actualOutput = translator.translate(inputQuery);
        assertEquals(normalizeWhitespace(expectedOutput), normalizeWhitespace(actualOutput));
    }

    private void assertContainsSql(String sql, String expectedFragment) {
        assertTrue(sql.contains(normalizeWhitespace(expectedFragment)));
    }

    private SchemaRegistry schemaRegistryWithTpchTables() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "lineitem",
            List.of(
                "l_partkey",
                "l_quantity",
                "l_extendedprice",
                "l_discount",
                "l_shipinstruct",
                "l_shipmode"
            )
        );
        schemaRegistry.registerTable(
            "part",
            List.of(
                "p_partkey",
                "p_brand",
                "p_container",
                "p_size"
            )
        );
        return schemaRegistry;
    }

    private SchemaRegistry schemaRegistryWithLineitemQ1Columns() {
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.registerTable(
            "lineitem",
            List.of(
                "l_quantity",
                "l_extendedprice",
                "l_discount",
                "l_tax",
                "l_returnflag",
                "l_linestatus",
                "l_shipdate"
            )
        );
        return schemaRegistry;
    }

    private String normalizeWhitespace(String str) {
        return str
            .trim()
            .replaceAll("\\s+", " ")
            .replaceAll("\\(\\s+", "(")
            .replaceAll("\\s+\\)", ")");
    }
}
