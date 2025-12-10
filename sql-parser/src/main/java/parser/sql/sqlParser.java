// Generated from parser/sql/sql.g4 by ANTLR 4.5
package parser.sql;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class sqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SELECT=1, DISTINCT=2, FROM=3, WHERE=4, GROUP=5, BY=6, HAVING=7, AS=8, 
		UNION=9, ALL=10, EXCEPT=11, IS=12, NOT=13, AND=14, OR=15, NULL_T=16, COUNT=17, 
		SUM=18, AVG=19, MIN=20, MAX=21, EQ=22, NEQ=23, LTE=24, GTE=25, LT=26, 
		GT=27, PLUS=28, MINUS=29, STAR=30, DOT=31, COMMA=32, LPAREN=33, RPAREN=34, 
		SEMICOLON=35, IDENT=36, QUOTED_IDENT=37, NUMBER=38, STRING=39, WS=40, 
		LINE_COMMENT=41, BLOCK_COMMENT=42;
	public static final int
		RULE_query = 0, RULE_setOp = 1, RULE_selectStmt = 2, RULE_selectClause = 3, 
		RULE_fromClause = 4, RULE_whereClause = 5, RULE_groupByClause = 6, RULE_havingClause = 7, 
		RULE_selectList = 8, RULE_selectItem = 9, RULE_fromItem = 10, RULE_groupByList = 11, 
		RULE_boolExpr = 12, RULE_orExpr = 13, RULE_andExpr = 14, RULE_notExpr = 15, 
		RULE_predicate = 16, RULE_compOp = 17, RULE_expr = 18, RULE_aggFunc = 19, 
		RULE_columnRef = 20, RULE_tableName = 21, RULE_alias = 22, RULE_literal = 23, 
		RULE_identifier = 24;
	public static final String[] ruleNames = {
		"query", "setOp", "selectStmt", "selectClause", "fromClause", "whereClause", 
		"groupByClause", "havingClause", "selectList", "selectItem", "fromItem", 
		"groupByList", "boolExpr", "orExpr", "andExpr", "notExpr", "predicate", 
		"compOp", "expr", "aggFunc", "columnRef", "tableName", "alias", "literal", 
		"identifier"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, "'='", "'!='", 
		"'<='", "'>='", "'<'", "'>'", "'+'", "'-'", "'*'", "'.'", "','", "'('", 
		"')'", "';'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "SELECT", "DISTINCT", "FROM", "WHERE", "GROUP", "BY", "HAVING", 
		"AS", "UNION", "ALL", "EXCEPT", "IS", "NOT", "AND", "OR", "NULL_T", "COUNT", 
		"SUM", "AVG", "MIN", "MAX", "EQ", "NEQ", "LTE", "GTE", "LT", "GT", "PLUS", 
		"MINUS", "STAR", "DOT", "COMMA", "LPAREN", "RPAREN", "SEMICOLON", "IDENT", 
		"QUOTED_IDENT", "NUMBER", "STRING", "WS", "LINE_COMMENT", "BLOCK_COMMENT"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "sql.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public sqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class QueryContext extends ParserRuleContext {
		public List<SelectStmtContext> selectStmt() {
			return getRuleContexts(SelectStmtContext.class);
		}
		public SelectStmtContext selectStmt(int i) {
			return getRuleContext(SelectStmtContext.class,i);
		}
		public TerminalNode EOF() { return getToken(sqlParser.EOF, 0); }
		public List<SetOpContext> setOp() {
			return getRuleContexts(SetOpContext.class);
		}
		public SetOpContext setOp(int i) {
			return getRuleContext(SetOpContext.class,i);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitQuery(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			selectStmt();
			setState(56);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==UNION || _la==EXCEPT) {
				{
				{
				setState(51);
				setOp();
				setState(52);
				selectStmt();
				}
				}
				setState(58);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(59);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SetOpContext extends ParserRuleContext {
		public TerminalNode UNION() { return getToken(sqlParser.UNION, 0); }
		public TerminalNode ALL() { return getToken(sqlParser.ALL, 0); }
		public TerminalNode EXCEPT() { return getToken(sqlParser.EXCEPT, 0); }
		public SetOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterSetOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitSetOp(this);
		}
	}

	public final SetOpContext setOp() throws RecognitionException {
		SetOpContext _localctx = new SetOpContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_setOp);
		int _la;
		try {
			setState(69);
			switch (_input.LA(1)) {
			case UNION:
				enterOuterAlt(_localctx, 1);
				{
				setState(61);
				match(UNION);
				setState(63);
				_la = _input.LA(1);
				if (_la==ALL) {
					{
					setState(62);
					match(ALL);
					}
				}

				}
				break;
			case EXCEPT:
				enterOuterAlt(_localctx, 2);
				{
				setState(65);
				match(EXCEPT);
				setState(67);
				_la = _input.LA(1);
				if (_la==ALL) {
					{
					setState(66);
					match(ALL);
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectStmtContext extends ParserRuleContext {
		public SelectClauseContext selectClause() {
			return getRuleContext(SelectClauseContext.class,0);
		}
		public FromClauseContext fromClause() {
			return getRuleContext(FromClauseContext.class,0);
		}
		public WhereClauseContext whereClause() {
			return getRuleContext(WhereClauseContext.class,0);
		}
		public GroupByClauseContext groupByClause() {
			return getRuleContext(GroupByClauseContext.class,0);
		}
		public HavingClauseContext havingClause() {
			return getRuleContext(HavingClauseContext.class,0);
		}
		public SelectStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterSelectStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitSelectStmt(this);
		}
	}

	public final SelectStmtContext selectStmt() throws RecognitionException {
		SelectStmtContext _localctx = new SelectStmtContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_selectStmt);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(71);
			selectClause();
			setState(73);
			_la = _input.LA(1);
			if (_la==FROM) {
				{
				setState(72);
				fromClause();
				}
			}

			setState(76);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(75);
				whereClause();
				}
			}

			setState(79);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(78);
				groupByClause();
				}
			}

			setState(82);
			_la = _input.LA(1);
			if (_la==HAVING) {
				{
				setState(81);
				havingClause();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectClauseContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(sqlParser.SELECT, 0); }
		public SelectListContext selectList() {
			return getRuleContext(SelectListContext.class,0);
		}
		public TerminalNode DISTINCT() { return getToken(sqlParser.DISTINCT, 0); }
		public SelectClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterSelectClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitSelectClause(this);
		}
	}

	public final SelectClauseContext selectClause() throws RecognitionException {
		SelectClauseContext _localctx = new SelectClauseContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_selectClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			match(SELECT);
			setState(86);
			_la = _input.LA(1);
			if (_la==DISTINCT) {
				{
				setState(85);
				match(DISTINCT);
				}
			}

			setState(88);
			selectList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromClauseContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(sqlParser.FROM, 0); }
		public List<FromItemContext> fromItem() {
			return getRuleContexts(FromItemContext.class);
		}
		public FromItemContext fromItem(int i) {
			return getRuleContext(FromItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(sqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(sqlParser.COMMA, i);
		}
		public FromClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterFromClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitFromClause(this);
		}
	}

	public final FromClauseContext fromClause() throws RecognitionException {
		FromClauseContext _localctx = new FromClauseContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_fromClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(FROM);
			setState(91);
			fromItem();
			setState(96);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(92);
				match(COMMA);
				setState(93);
				fromItem();
				}
				}
				setState(98);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhereClauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(sqlParser.WHERE, 0); }
		public BoolExprContext boolExpr() {
			return getRuleContext(BoolExprContext.class,0);
		}
		public WhereClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whereClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterWhereClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitWhereClause(this);
		}
	}

	public final WhereClauseContext whereClause() throws RecognitionException {
		WhereClauseContext _localctx = new WhereClauseContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_whereClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(99);
			match(WHERE);
			setState(100);
			boolExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupByClauseContext extends ParserRuleContext {
		public TerminalNode GROUP() { return getToken(sqlParser.GROUP, 0); }
		public TerminalNode BY() { return getToken(sqlParser.BY, 0); }
		public GroupByListContext groupByList() {
			return getRuleContext(GroupByListContext.class,0);
		}
		public GroupByClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupByClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterGroupByClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitGroupByClause(this);
		}
	}

	public final GroupByClauseContext groupByClause() throws RecognitionException {
		GroupByClauseContext _localctx = new GroupByClauseContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_groupByClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(GROUP);
			setState(103);
			match(BY);
			setState(104);
			groupByList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HavingClauseContext extends ParserRuleContext {
		public TerminalNode HAVING() { return getToken(sqlParser.HAVING, 0); }
		public BoolExprContext boolExpr() {
			return getRuleContext(BoolExprContext.class,0);
		}
		public HavingClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_havingClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterHavingClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitHavingClause(this);
		}
	}

	public final HavingClauseContext havingClause() throws RecognitionException {
		HavingClauseContext _localctx = new HavingClauseContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_havingClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			match(HAVING);
			setState(107);
			boolExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectListContext extends ParserRuleContext {
		public List<SelectItemContext> selectItem() {
			return getRuleContexts(SelectItemContext.class);
		}
		public SelectItemContext selectItem(int i) {
			return getRuleContext(SelectItemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(sqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(sqlParser.COMMA, i);
		}
		public SelectListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterSelectList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitSelectList(this);
		}
	}

	public final SelectListContext selectList() throws RecognitionException {
		SelectListContext _localctx = new SelectListContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_selectList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			selectItem();
			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(110);
				match(COMMA);
				setState(111);
				selectItem();
				}
				}
				setState(116);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SelectItemContext extends ParserRuleContext {
		public TerminalNode STAR() { return getToken(sqlParser.STAR, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public TerminalNode AS() { return getToken(sqlParser.AS, 0); }
		public SelectItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterSelectItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitSelectItem(this);
		}
	}

	public final SelectItemContext selectItem() throws RecognitionException {
		SelectItemContext _localctx = new SelectItemContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_selectItem);
		int _la;
		try {
			setState(125);
			switch (_input.LA(1)) {
			case STAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(117);
				match(STAR);
				}
				break;
			case NULL_T:
			case COUNT:
			case SUM:
			case AVG:
			case MIN:
			case MAX:
			case LPAREN:
			case IDENT:
			case QUOTED_IDENT:
			case NUMBER:
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(118);
				expr(0);
				setState(123);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << AS) | (1L << IDENT) | (1L << QUOTED_IDENT))) != 0)) {
					{
					setState(120);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(119);
						match(AS);
						}
					}

					setState(122);
					alias();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FromItemContext extends ParserRuleContext {
		public FromItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromItem; }
	 
		public FromItemContext() { }
		public void copyFrom(FromItemContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FromTableItemContext extends FromItemContext {
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public TerminalNode AS() { return getToken(sqlParser.AS, 0); }
		public FromTableItemContext(FromItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterFromTableItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitFromTableItem(this);
		}
	}
	public static class FromQueryItemContext extends FromItemContext {
		public TerminalNode LPAREN() { return getToken(sqlParser.LPAREN, 0); }
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(sqlParser.RPAREN, 0); }
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public TerminalNode AS() { return getToken(sqlParser.AS, 0); }
		public FromQueryItemContext(FromItemContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterFromQueryItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitFromQueryItem(this);
		}
	}

	public final FromItemContext fromItem() throws RecognitionException {
		FromItemContext _localctx = new FromItemContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_fromItem);
		int _la;
		try {
			setState(142);
			switch (_input.LA(1)) {
			case IDENT:
			case QUOTED_IDENT:
				_localctx = new FromTableItemContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(127);
				tableName();
				setState(132);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << AS) | (1L << IDENT) | (1L << QUOTED_IDENT))) != 0)) {
					{
					setState(129);
					_la = _input.LA(1);
					if (_la==AS) {
						{
						setState(128);
						match(AS);
						}
					}

					setState(131);
					alias();
					}
				}

				}
				break;
			case LPAREN:
				_localctx = new FromQueryItemContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(134);
				match(LPAREN);
				setState(135);
				query();
				setState(136);
				match(RPAREN);
				{
				setState(138);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(137);
					match(AS);
					}
				}

				setState(140);
				alias();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GroupByListContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(sqlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(sqlParser.COMMA, i);
		}
		public GroupByListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_groupByList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterGroupByList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitGroupByList(this);
		}
	}

	public final GroupByListContext groupByList() throws RecognitionException {
		GroupByListContext _localctx = new GroupByListContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_groupByList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			expr(0);
			setState(149);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(145);
				match(COMMA);
				setState(146);
				expr(0);
				}
				}
				setState(151);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BoolExprContext extends ParserRuleContext {
		public OrExprContext orExpr() {
			return getRuleContext(OrExprContext.class,0);
		}
		public BoolExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boolExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterBoolExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitBoolExpr(this);
		}
	}

	public final BoolExprContext boolExpr() throws RecognitionException {
		BoolExprContext _localctx = new BoolExprContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_boolExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			orExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrExprContext extends ParserRuleContext {
		public List<AndExprContext> andExpr() {
			return getRuleContexts(AndExprContext.class);
		}
		public AndExprContext andExpr(int i) {
			return getRuleContext(AndExprContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(sqlParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(sqlParser.OR, i);
		}
		public OrExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterOrExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitOrExpr(this);
		}
	}

	public final OrExprContext orExpr() throws RecognitionException {
		OrExprContext _localctx = new OrExprContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_orExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			andExpr();
			setState(159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(155);
				match(OR);
				setState(156);
				andExpr();
				}
				}
				setState(161);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AndExprContext extends ParserRuleContext {
		public List<NotExprContext> notExpr() {
			return getRuleContexts(NotExprContext.class);
		}
		public NotExprContext notExpr(int i) {
			return getRuleContext(NotExprContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(sqlParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(sqlParser.AND, i);
		}
		public AndExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_andExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterAndExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitAndExpr(this);
		}
	}

	public final AndExprContext andExpr() throws RecognitionException {
		AndExprContext _localctx = new AndExprContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_andExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			notExpr();
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(163);
				match(AND);
				setState(164);
				notExpr();
				}
				}
				setState(169);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NotExprContext extends ParserRuleContext {
		public TerminalNode NOT() { return getToken(sqlParser.NOT, 0); }
		public NotExprContext notExpr() {
			return getRuleContext(NotExprContext.class,0);
		}
		public PredicateContext predicate() {
			return getRuleContext(PredicateContext.class,0);
		}
		public NotExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_notExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterNotExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitNotExpr(this);
		}
	}

	public final NotExprContext notExpr() throws RecognitionException {
		NotExprContext _localctx = new NotExprContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_notExpr);
		try {
			setState(173);
			switch (_input.LA(1)) {
			case NOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(170);
				match(NOT);
				setState(171);
				notExpr();
				}
				break;
			case NULL_T:
			case COUNT:
			case SUM:
			case AVG:
			case MIN:
			case MAX:
			case LPAREN:
			case IDENT:
			case QUOTED_IDENT:
			case NUMBER:
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(172);
				predicate();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PredicateContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public CompOpContext compOp() {
			return getRuleContext(CompOpContext.class,0);
		}
		public TerminalNode IS() { return getToken(sqlParser.IS, 0); }
		public TerminalNode NULL_T() { return getToken(sqlParser.NULL_T, 0); }
		public TerminalNode NOT() { return getToken(sqlParser.NOT, 0); }
		public TerminalNode LPAREN() { return getToken(sqlParser.LPAREN, 0); }
		public BoolExprContext boolExpr() {
			return getRuleContext(BoolExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(sqlParser.RPAREN, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitPredicate(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_predicate);
		int _la;
		try {
			setState(190);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(175);
				expr(0);
				setState(176);
				compOp();
				setState(177);
				expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(179);
				expr(0);
				setState(180);
				match(IS);
				setState(182);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(181);
					match(NOT);
					}
				}

				setState(184);
				match(NULL_T);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(186);
				match(LPAREN);
				setState(187);
				boolExpr();
				setState(188);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CompOpContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(sqlParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(sqlParser.NEQ, 0); }
		public TerminalNode LT() { return getToken(sqlParser.LT, 0); }
		public TerminalNode GT() { return getToken(sqlParser.GT, 0); }
		public TerminalNode LTE() { return getToken(sqlParser.LTE, 0); }
		public TerminalNode GTE() { return getToken(sqlParser.GTE, 0); }
		public CompOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterCompOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitCompOp(this);
		}
	}

	public final CompOpContext compOp() throws RecognitionException {
		CompOpContext _localctx = new CompOpContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_compOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQ) | (1L << NEQ) | (1L << LTE) | (1L << GTE) | (1L << LT) | (1L << GT))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ColumnExprContext extends ExprContext {
		public ColumnRefContext columnRef() {
			return getRuleContext(ColumnRefContext.class,0);
		}
		public ColumnExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterColumnExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitColumnExpr(this);
		}
	}
	public static class AggCallExprContext extends ExprContext {
		public AggFuncContext aggFunc() {
			return getRuleContext(AggFuncContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(sqlParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(sqlParser.RPAREN, 0); }
		public AggCallExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterAggCallExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitAggCallExpr(this);
		}
	}
	public static class AddSubExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode PLUS() { return getToken(sqlParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(sqlParser.MINUS, 0); }
		public AddSubExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterAddSubExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitAddSubExpr(this);
		}
	}
	public static class LiteralExprContext extends ExprContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public LiteralExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterLiteralExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitLiteralExpr(this);
		}
	}
	public static class MulExprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode STAR() { return getToken(sqlParser.STAR, 0); }
		public MulExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterMulExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitMulExpr(this);
		}
	}
	public static class ScalarSubqueryExprContext extends ExprContext {
		public TerminalNode LPAREN() { return getToken(sqlParser.LPAREN, 0); }
		public SelectStmtContext selectStmt() {
			return getRuleContext(SelectStmtContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(sqlParser.RPAREN, 0); }
		public ScalarSubqueryExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterScalarSubqueryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitScalarSubqueryExpr(this);
		}
	}
	public static class ParenExprContext extends ExprContext {
		public TerminalNode LPAREN() { return getToken(sqlParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(sqlParser.RPAREN, 0); }
		public ParenExprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterParenExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitParenExpr(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(210);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				_localctx = new AggCallExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(195);
				aggFunc();
				setState(196);
				match(LPAREN);
				setState(197);
				expr(0);
				setState(198);
				match(RPAREN);
				}
				break;
			case 2:
				{
				_localctx = new ScalarSubqueryExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(200);
				match(LPAREN);
				setState(201);
				selectStmt();
				setState(202);
				match(RPAREN);
				}
				break;
			case 3:
				{
				_localctx = new ColumnExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(204);
				columnRef();
				}
				break;
			case 4:
				{
				_localctx = new LiteralExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(205);
				literal();
				}
				break;
			case 5:
				{
				_localctx = new ParenExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(206);
				match(LPAREN);
				setState(207);
				expr(0);
				setState(208);
				match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(220);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(218);
					switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
					case 1:
						{
						_localctx = new MulExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(212);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(213);
						match(STAR);
						setState(214);
						expr(8);
						}
						break;
					case 2:
						{
						_localctx = new AddSubExprContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(215);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(216);
						_la = _input.LA(1);
						if ( !(_la==PLUS || _la==MINUS) ) {
						_errHandler.recoverInline(this);
						} else {
							consume();
						}
						setState(217);
						expr(7);
						}
						break;
					}
					} 
				}
				setState(222);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class AggFuncContext extends ParserRuleContext {
		public TerminalNode COUNT() { return getToken(sqlParser.COUNT, 0); }
		public TerminalNode SUM() { return getToken(sqlParser.SUM, 0); }
		public TerminalNode AVG() { return getToken(sqlParser.AVG, 0); }
		public TerminalNode MIN() { return getToken(sqlParser.MIN, 0); }
		public TerminalNode MAX() { return getToken(sqlParser.MAX, 0); }
		public AggFuncContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aggFunc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterAggFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitAggFunc(this);
		}
	}

	public final AggFuncContext aggFunc() throws RecognitionException {
		AggFuncContext _localctx = new AggFuncContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_aggFunc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(223);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COUNT) | (1L << SUM) | (1L << AVG) | (1L << MIN) | (1L << MAX))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ColumnRefContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public TerminalNode DOT() { return getToken(sqlParser.DOT, 0); }
		public ColumnRefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnRef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterColumnRef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitColumnRef(this);
		}
	}

	public final ColumnRefContext columnRef() throws RecognitionException {
		ColumnRefContext _localctx = new ColumnRefContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_columnRef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(228);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				{
				setState(225);
				tableName();
				setState(226);
				match(DOT);
				}
				break;
			}
			setState(230);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TableNameContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TableNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitTableName(this);
		}
	}

	public final TableNameContext tableName() throws RecognitionException {
		TableNameContext _localctx = new TableNameContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_tableName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(232);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AliasContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public AliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitAlias(this);
		}
	}

	public final AliasContext alias() throws RecognitionException {
		AliasContext _localctx = new AliasContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_alias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(234);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode NULL_T() { return getToken(sqlParser.NULL_T, 0); }
		public TerminalNode NUMBER() { return getToken(sqlParser.NUMBER, 0); }
		public TerminalNode STRING() { return getToken(sqlParser.STRING, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitLiteral(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(236);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NULL_T) | (1L << NUMBER) | (1L << STRING))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode QUOTED_IDENT() { return getToken(sqlParser.QUOTED_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(sqlParser.IDENT, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof sqlListener ) ((sqlListener)listener).exitIdentifier(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(238);
			_la = _input.LA(1);
			if ( !(_la==IDENT || _la==QUOTED_IDENT) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 18:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 7);
		case 1:
			return precpred(_ctx, 6);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3,\u00f3\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\3\2\3\2\3\2\3\2\7\29\n\2\f\2\16\2<\13\2\3\2\3\2\3\3\3\3\5\3"+
		"B\n\3\3\3\3\3\5\3F\n\3\5\3H\n\3\3\4\3\4\5\4L\n\4\3\4\5\4O\n\4\3\4\5\4"+
		"R\n\4\3\4\5\4U\n\4\3\5\3\5\5\5Y\n\5\3\5\3\5\3\6\3\6\3\6\3\6\7\6a\n\6\f"+
		"\6\16\6d\13\6\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\7\n"+
		"s\n\n\f\n\16\nv\13\n\3\13\3\13\3\13\5\13{\n\13\3\13\5\13~\n\13\5\13\u0080"+
		"\n\13\3\f\3\f\5\f\u0084\n\f\3\f\5\f\u0087\n\f\3\f\3\f\3\f\3\f\5\f\u008d"+
		"\n\f\3\f\3\f\5\f\u0091\n\f\3\r\3\r\3\r\7\r\u0096\n\r\f\r\16\r\u0099\13"+
		"\r\3\16\3\16\3\17\3\17\3\17\7\17\u00a0\n\17\f\17\16\17\u00a3\13\17\3\20"+
		"\3\20\3\20\7\20\u00a8\n\20\f\20\16\20\u00ab\13\20\3\21\3\21\3\21\5\21"+
		"\u00b0\n\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u00b9\n\22\3\22\3"+
		"\22\3\22\3\22\3\22\3\22\5\22\u00c1\n\22\3\23\3\23\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u00d5"+
		"\n\24\3\24\3\24\3\24\3\24\3\24\3\24\7\24\u00dd\n\24\f\24\16\24\u00e0\13"+
		"\24\3\25\3\25\3\26\3\26\3\26\5\26\u00e7\n\26\3\26\3\26\3\27\3\27\3\30"+
		"\3\30\3\31\3\31\3\32\3\32\3\32\2\3&\33\2\4\6\b\n\f\16\20\22\24\26\30\32"+
		"\34\36 \"$&(*,.\60\62\2\7\3\2\30\35\3\2\36\37\3\2\23\27\4\2\22\22()\3"+
		"\2&\'\u00f9\2\64\3\2\2\2\4G\3\2\2\2\6I\3\2\2\2\bV\3\2\2\2\n\\\3\2\2\2"+
		"\fe\3\2\2\2\16h\3\2\2\2\20l\3\2\2\2\22o\3\2\2\2\24\177\3\2\2\2\26\u0090"+
		"\3\2\2\2\30\u0092\3\2\2\2\32\u009a\3\2\2\2\34\u009c\3\2\2\2\36\u00a4\3"+
		"\2\2\2 \u00af\3\2\2\2\"\u00c0\3\2\2\2$\u00c2\3\2\2\2&\u00d4\3\2\2\2(\u00e1"+
		"\3\2\2\2*\u00e6\3\2\2\2,\u00ea\3\2\2\2.\u00ec\3\2\2\2\60\u00ee\3\2\2\2"+
		"\62\u00f0\3\2\2\2\64:\5\6\4\2\65\66\5\4\3\2\66\67\5\6\4\2\679\3\2\2\2"+
		"8\65\3\2\2\29<\3\2\2\2:8\3\2\2\2:;\3\2\2\2;=\3\2\2\2<:\3\2\2\2=>\7\2\2"+
		"\3>\3\3\2\2\2?A\7\13\2\2@B\7\f\2\2A@\3\2\2\2AB\3\2\2\2BH\3\2\2\2CE\7\r"+
		"\2\2DF\7\f\2\2ED\3\2\2\2EF\3\2\2\2FH\3\2\2\2G?\3\2\2\2GC\3\2\2\2H\5\3"+
		"\2\2\2IK\5\b\5\2JL\5\n\6\2KJ\3\2\2\2KL\3\2\2\2LN\3\2\2\2MO\5\f\7\2NM\3"+
		"\2\2\2NO\3\2\2\2OQ\3\2\2\2PR\5\16\b\2QP\3\2\2\2QR\3\2\2\2RT\3\2\2\2SU"+
		"\5\20\t\2TS\3\2\2\2TU\3\2\2\2U\7\3\2\2\2VX\7\3\2\2WY\7\4\2\2XW\3\2\2\2"+
		"XY\3\2\2\2YZ\3\2\2\2Z[\5\22\n\2[\t\3\2\2\2\\]\7\5\2\2]b\5\26\f\2^_\7\""+
		"\2\2_a\5\26\f\2`^\3\2\2\2ad\3\2\2\2b`\3\2\2\2bc\3\2\2\2c\13\3\2\2\2db"+
		"\3\2\2\2ef\7\6\2\2fg\5\32\16\2g\r\3\2\2\2hi\7\7\2\2ij\7\b\2\2jk\5\30\r"+
		"\2k\17\3\2\2\2lm\7\t\2\2mn\5\32\16\2n\21\3\2\2\2ot\5\24\13\2pq\7\"\2\2"+
		"qs\5\24\13\2rp\3\2\2\2sv\3\2\2\2tr\3\2\2\2tu\3\2\2\2u\23\3\2\2\2vt\3\2"+
		"\2\2w\u0080\7 \2\2x}\5&\24\2y{\7\n\2\2zy\3\2\2\2z{\3\2\2\2{|\3\2\2\2|"+
		"~\5.\30\2}z\3\2\2\2}~\3\2\2\2~\u0080\3\2\2\2\177w\3\2\2\2\177x\3\2\2\2"+
		"\u0080\25\3\2\2\2\u0081\u0086\5,\27\2\u0082\u0084\7\n\2\2\u0083\u0082"+
		"\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0085\3\2\2\2\u0085\u0087\5.\30\2\u0086"+
		"\u0083\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0091\3\2\2\2\u0088\u0089\7#"+
		"\2\2\u0089\u008a\5\2\2\2\u008a\u008c\7$\2\2\u008b\u008d\7\n\2\2\u008c"+
		"\u008b\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f\5."+
		"\30\2\u008f\u0091\3\2\2\2\u0090\u0081\3\2\2\2\u0090\u0088\3\2\2\2\u0091"+
		"\27\3\2\2\2\u0092\u0097\5&\24\2\u0093\u0094\7\"\2\2\u0094\u0096\5&\24"+
		"\2\u0095\u0093\3\2\2\2\u0096\u0099\3\2\2\2\u0097\u0095\3\2\2\2\u0097\u0098"+
		"\3\2\2\2\u0098\31\3\2\2\2\u0099\u0097\3\2\2\2\u009a\u009b\5\34\17\2\u009b"+
		"\33\3\2\2\2\u009c\u00a1\5\36\20\2\u009d\u009e\7\21\2\2\u009e\u00a0\5\36"+
		"\20\2\u009f\u009d\3\2\2\2\u00a0\u00a3\3\2\2\2\u00a1\u009f\3\2\2\2\u00a1"+
		"\u00a2\3\2\2\2\u00a2\35\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a4\u00a9\5 \21"+
		"\2\u00a5\u00a6\7\20\2\2\u00a6\u00a8\5 \21\2\u00a7\u00a5\3\2\2\2\u00a8"+
		"\u00ab\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\37\3\2\2"+
		"\2\u00ab\u00a9\3\2\2\2\u00ac\u00ad\7\17\2\2\u00ad\u00b0\5 \21\2\u00ae"+
		"\u00b0\5\"\22\2\u00af\u00ac\3\2\2\2\u00af\u00ae\3\2\2\2\u00b0!\3\2\2\2"+
		"\u00b1\u00b2\5&\24\2\u00b2\u00b3\5$\23\2\u00b3\u00b4\5&\24\2\u00b4\u00c1"+
		"\3\2\2\2\u00b5\u00b6\5&\24\2\u00b6\u00b8\7\16\2\2\u00b7\u00b9\7\17\2\2"+
		"\u00b8\u00b7\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00ba\3\2\2\2\u00ba\u00bb"+
		"\7\22\2\2\u00bb\u00c1\3\2\2\2\u00bc\u00bd\7#\2\2\u00bd\u00be\5\32\16\2"+
		"\u00be\u00bf\7$\2\2\u00bf\u00c1\3\2\2\2\u00c0\u00b1\3\2\2\2\u00c0\u00b5"+
		"\3\2\2\2\u00c0\u00bc\3\2\2\2\u00c1#\3\2\2\2\u00c2\u00c3\t\2\2\2\u00c3"+
		"%\3\2\2\2\u00c4\u00c5\b\24\1\2\u00c5\u00c6\5(\25\2\u00c6\u00c7\7#\2\2"+
		"\u00c7\u00c8\5&\24\2\u00c8\u00c9\7$\2\2\u00c9\u00d5\3\2\2\2\u00ca\u00cb"+
		"\7#\2\2\u00cb\u00cc\5\6\4\2\u00cc\u00cd\7$\2\2\u00cd\u00d5\3\2\2\2\u00ce"+
		"\u00d5\5*\26\2\u00cf\u00d5\5\60\31\2\u00d0\u00d1\7#\2\2\u00d1\u00d2\5"+
		"&\24\2\u00d2\u00d3\7$\2\2\u00d3\u00d5\3\2\2\2\u00d4\u00c4\3\2\2\2\u00d4"+
		"\u00ca\3\2\2\2\u00d4\u00ce\3\2\2\2\u00d4\u00cf\3\2\2\2\u00d4\u00d0\3\2"+
		"\2\2\u00d5\u00de\3\2\2\2\u00d6\u00d7\f\t\2\2\u00d7\u00d8\7 \2\2\u00d8"+
		"\u00dd\5&\24\n\u00d9\u00da\f\b\2\2\u00da\u00db\t\3\2\2\u00db\u00dd\5&"+
		"\24\t\u00dc\u00d6\3\2\2\2\u00dc\u00d9\3\2\2\2\u00dd\u00e0\3\2\2\2\u00de"+
		"\u00dc\3\2\2\2\u00de\u00df\3\2\2\2\u00df\'\3\2\2\2\u00e0\u00de\3\2\2\2"+
		"\u00e1\u00e2\t\4\2\2\u00e2)\3\2\2\2\u00e3\u00e4\5,\27\2\u00e4\u00e5\7"+
		"!\2\2\u00e5\u00e7\3\2\2\2\u00e6\u00e3\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7"+
		"\u00e8\3\2\2\2\u00e8\u00e9\5\62\32\2\u00e9+\3\2\2\2\u00ea\u00eb\5\62\32"+
		"\2\u00eb-\3\2\2\2\u00ec\u00ed\5\62\32\2\u00ed/\3\2\2\2\u00ee\u00ef\t\5"+
		"\2\2\u00ef\61\3\2\2\2\u00f0\u00f1\t\6\2\2\u00f1\63\3\2\2\2\36:AEGKNQT"+
		"Xbtz}\177\u0083\u0086\u008c\u0090\u0097\u00a1\u00a9\u00af\u00b8\u00c0"+
		"\u00d4\u00dc\u00de\u00e6";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}