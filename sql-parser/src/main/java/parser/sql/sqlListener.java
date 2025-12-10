// Generated from parser/sql/sql.g4 by ANTLR 4.5
package parser.sql;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link sqlParser}.
 */
public interface sqlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link sqlParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(sqlParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(sqlParser.QueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#setOp}.
	 * @param ctx the parse tree
	 */
	void enterSetOp(sqlParser.SetOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#setOp}.
	 * @param ctx the parse tree
	 */
	void exitSetOp(sqlParser.SetOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#selectStmt}.
	 * @param ctx the parse tree
	 */
	void enterSelectStmt(sqlParser.SelectStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#selectStmt}.
	 * @param ctx the parse tree
	 */
	void exitSelectStmt(sqlParser.SelectStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(sqlParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(sqlParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(sqlParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(sqlParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(sqlParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(sqlParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void enterGroupByClause(sqlParser.GroupByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#groupByClause}.
	 * @param ctx the parse tree
	 */
	void exitGroupByClause(sqlParser.GroupByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void enterHavingClause(sqlParser.HavingClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#havingClause}.
	 * @param ctx the parse tree
	 */
	void exitHavingClause(sqlParser.HavingClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#selectList}.
	 * @param ctx the parse tree
	 */
	void enterSelectList(sqlParser.SelectListContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#selectList}.
	 * @param ctx the parse tree
	 */
	void exitSelectList(sqlParser.SelectListContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void enterSelectItem(sqlParser.SelectItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#selectItem}.
	 * @param ctx the parse tree
	 */
	void exitSelectItem(sqlParser.SelectItemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FromTableItem}
	 * labeled alternative in {@link sqlParser#fromItem}.
	 * @param ctx the parse tree
	 */
	void enterFromTableItem(sqlParser.FromTableItemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FromTableItem}
	 * labeled alternative in {@link sqlParser#fromItem}.
	 * @param ctx the parse tree
	 */
	void exitFromTableItem(sqlParser.FromTableItemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FromQueryItem}
	 * labeled alternative in {@link sqlParser#fromItem}.
	 * @param ctx the parse tree
	 */
	void enterFromQueryItem(sqlParser.FromQueryItemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FromQueryItem}
	 * labeled alternative in {@link sqlParser#fromItem}.
	 * @param ctx the parse tree
	 */
	void exitFromQueryItem(sqlParser.FromQueryItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#groupByList}.
	 * @param ctx the parse tree
	 */
	void enterGroupByList(sqlParser.GroupByListContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#groupByList}.
	 * @param ctx the parse tree
	 */
	void exitGroupByList(sqlParser.GroupByListContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#boolExpr}.
	 * @param ctx the parse tree
	 */
	void enterBoolExpr(sqlParser.BoolExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#boolExpr}.
	 * @param ctx the parse tree
	 */
	void exitBoolExpr(sqlParser.BoolExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(sqlParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(sqlParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(sqlParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(sqlParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(sqlParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(sqlParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(sqlParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(sqlParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#compOp}.
	 * @param ctx the parse tree
	 */
	void enterCompOp(sqlParser.CompOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#compOp}.
	 * @param ctx the parse tree
	 */
	void exitCompOp(sqlParser.CompOpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterColumnExpr(sqlParser.ColumnExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitColumnExpr(sqlParser.ColumnExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aggCallExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAggCallExpr(sqlParser.AggCallExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aggCallExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAggCallExpr(sqlParser.AggCallExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addSubExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAddSubExpr(sqlParser.AddSubExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addSubExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAddSubExpr(sqlParser.AddSubExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code literalExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpr(sqlParser.LiteralExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code literalExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpr(sqlParser.LiteralExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mulExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMulExpr(sqlParser.MulExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mulExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMulExpr(sqlParser.MulExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code scalarSubqueryExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterScalarSubqueryExpr(sqlParser.ScalarSubqueryExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code scalarSubqueryExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitScalarSubqueryExpr(sqlParser.ScalarSubqueryExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterParenExpr(sqlParser.ParenExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link sqlParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitParenExpr(sqlParser.ParenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#aggFunc}.
	 * @param ctx the parse tree
	 */
	void enterAggFunc(sqlParser.AggFuncContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#aggFunc}.
	 * @param ctx the parse tree
	 */
	void exitAggFunc(sqlParser.AggFuncContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#columnRef}.
	 * @param ctx the parse tree
	 */
	void enterColumnRef(sqlParser.ColumnRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#columnRef}.
	 * @param ctx the parse tree
	 */
	void exitColumnRef(sqlParser.ColumnRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void enterTableName(sqlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void exitTableName(sqlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#alias}.
	 * @param ctx the parse tree
	 */
	void enterAlias(sqlParser.AliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#alias}.
	 * @param ctx the parse tree
	 */
	void exitAlias(sqlParser.AliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(sqlParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(sqlParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link sqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(sqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link sqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(sqlParser.IdentifierContext ctx);
}