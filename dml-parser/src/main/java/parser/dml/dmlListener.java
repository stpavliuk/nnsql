// Generated from parser/dml/dml.g4 by ANTLR 4.5
package parser.dml;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link dmlParser}.
 */
public interface dmlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link dmlParser#dmlStatement}.
	 * @param ctx the parse tree
	 */
	void enterDmlStatement(dmlParser.DmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#dmlStatement}.
	 * @param ctx the parse tree
	 */
	void exitDmlStatement(dmlParser.DmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#insertStmt}.
	 * @param ctx the parse tree
	 */
	void enterInsertStmt(dmlParser.InsertStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#insertStmt}.
	 * @param ctx the parse tree
	 */
	void exitInsertStmt(dmlParser.InsertStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#columnList}.
	 * @param ctx the parse tree
	 */
	void enterColumnList(dmlParser.ColumnListContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#columnList}.
	 * @param ctx the parse tree
	 */
	void exitColumnList(dmlParser.ColumnListContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#valuesList}.
	 * @param ctx the parse tree
	 */
	void enterValuesList(dmlParser.ValuesListContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#valuesList}.
	 * @param ctx the parse tree
	 */
	void exitValuesList(dmlParser.ValuesListContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#literalList}.
	 * @param ctx the parse tree
	 */
	void enterLiteralList(dmlParser.LiteralListContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#literalList}.
	 * @param ctx the parse tree
	 */
	void exitLiteralList(dmlParser.LiteralListContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void enterTableName(dmlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void exitTableName(dmlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#columnName}.
	 * @param ctx the parse tree
	 */
	void enterColumnName(dmlParser.ColumnNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#columnName}.
	 * @param ctx the parse tree
	 */
	void exitColumnName(dmlParser.ColumnNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(dmlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(dmlParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link dmlParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(dmlParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link dmlParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(dmlParser.LiteralContext ctx);
}