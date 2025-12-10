// Generated from parser/ddl/ddl.g4 by ANTLR 4.5
package parser.ddl;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ddlParser}.
 */
public interface ddlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ddlParser#ddlStatement}.
	 * @param ctx the parse tree
	 */
	void enterDdlStatement(ddlParser.DdlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#ddlStatement}.
	 * @param ctx the parse tree
	 */
	void exitDdlStatement(ddlParser.DdlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#createTableStmt}.
	 * @param ctx the parse tree
	 */
	void enterCreateTableStmt(ddlParser.CreateTableStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#createTableStmt}.
	 * @param ctx the parse tree
	 */
	void exitCreateTableStmt(ddlParser.CreateTableStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#tableElementList}.
	 * @param ctx the parse tree
	 */
	void enterTableElementList(ddlParser.TableElementListContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#tableElementList}.
	 * @param ctx the parse tree
	 */
	void exitTableElementList(ddlParser.TableElementListContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void enterColumnDef(ddlParser.ColumnDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#columnDef}.
	 * @param ctx the parse tree
	 */
	void exitColumnDef(ddlParser.ColumnDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterColumnConstraint(ddlParser.ColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitColumnConstraint(ddlParser.ColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterIntType(ddlParser.IntTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitIntType(ddlParser.IntTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterIntegerType(ddlParser.IntegerTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitIntegerType(ddlParser.IntegerTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterSmallintType(ddlParser.SmallintTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitSmallintType(ddlParser.SmallintTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterBigintType(ddlParser.BigintTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitBigintType(ddlParser.BigintTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterTinyintType(ddlParser.TinyintTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyintType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitTinyintType(ddlParser.TinyintTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterFloatType(ddlParser.FloatTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitFloatType(ddlParser.FloatTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDoubleType(ddlParser.DoubleTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDoubleType(ddlParser.DoubleTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decimalType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDecimalType(ddlParser.DecimalTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decimalType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDecimalType(ddlParser.DecimalTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterNumericType(ddlParser.NumericTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitNumericType(ddlParser.NumericTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code charType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterCharType(ddlParser.CharTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code charType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitCharType(ddlParser.CharTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code varcharType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterVarcharType(ddlParser.VarcharTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code varcharType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitVarcharType(ddlParser.VarcharTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code textType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterTextType(ddlParser.TextTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code textType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitTextType(ddlParser.TextTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dateType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDateType(ddlParser.DateTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dateType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDateType(ddlParser.DateTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timeType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterTimeType(ddlParser.TimeTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timeType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitTimeType(ddlParser.TimeTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code datetimeType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDatetimeType(ddlParser.DatetimeTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code datetimeType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDatetimeType(ddlParser.DatetimeTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code timestampType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterTimestampType(ddlParser.TimestampTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code timestampType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitTimestampType(ddlParser.TimestampTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterBooleanType(ddlParser.BooleanTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitBooleanType(ddlParser.BooleanTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code boolType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterBoolType(ddlParser.BoolTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code boolType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitBoolType(ddlParser.BoolTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code blobType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterBlobType(ddlParser.BlobTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code blobType}
	 * labeled alternative in {@link ddlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitBlobType(ddlParser.BlobTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#precision}.
	 * @param ctx the parse tree
	 */
	void enterPrecision(ddlParser.PrecisionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#precision}.
	 * @param ctx the parse tree
	 */
	void exitPrecision(ddlParser.PrecisionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#scale}.
	 * @param ctx the parse tree
	 */
	void enterScale(ddlParser.ScaleContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#scale}.
	 * @param ctx the parse tree
	 */
	void exitScale(ddlParser.ScaleContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#length}.
	 * @param ctx the parse tree
	 */
	void enterLength(ddlParser.LengthContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#length}.
	 * @param ctx the parse tree
	 */
	void exitLength(ddlParser.LengthContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void enterTableName(ddlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void exitTableName(ddlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#columnName}.
	 * @param ctx the parse tree
	 */
	void enterColumnName(ddlParser.ColumnNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#columnName}.
	 * @param ctx the parse tree
	 */
	void exitColumnName(ddlParser.ColumnNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ddlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ddlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ddlParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(ddlParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link ddlParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(ddlParser.LiteralContext ctx);
}