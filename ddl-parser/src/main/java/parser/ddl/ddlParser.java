// Generated from parser/ddl/ddl.g4 by ANTLR 4.5
package parser.ddl;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ddlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CREATE=1, TABLE=2, PRIMARY=3, KEY=4, NOT=5, NULL_T=6, TRUE=7, FALSE=8, 
		INT=9, INTEGER=10, SMALLINT=11, BIGINT=12, TINYINT=13, FLOAT=14, DOUBLE=15, 
		DECIMAL=16, NUMERIC=17, CHAR=18, VARCHAR=19, TEXT=20, DATE=21, TIME=22, 
		DATETIME=23, TIMESTAMP=24, BOOLEAN=25, BOOL=26, BLOB=27, DOT=28, COMMA=29, 
		LPAREN=30, RPAREN=31, SEMICOLON=32, IDENT=33, QUOTED_IDENT=34, NUMBER=35, 
		STRING=36, WS=37, LINE_COMMENT=38;
	public static final int
		RULE_ddlStatement = 0, RULE_createTableStmt = 1, RULE_tableElementList = 2, 
		RULE_columnDef = 3, RULE_columnConstraint = 4, RULE_dataType = 5, RULE_precision = 6, 
		RULE_scale = 7, RULE_length = 8, RULE_tableName = 9, RULE_columnName = 10, 
		RULE_identifier = 11, RULE_literal = 12;
	public static final String[] ruleNames = {
		"ddlStatement", "createTableStmt", "tableElementList", "columnDef", "columnConstraint", 
		"dataType", "precision", "scale", "length", "tableName", "columnName", 
		"identifier", "literal"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, "'.'", "','", "'('", "')'", "';'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "CREATE", "TABLE", "PRIMARY", "KEY", "NOT", "NULL_T", "TRUE", "FALSE", 
		"INT", "INTEGER", "SMALLINT", "BIGINT", "TINYINT", "FLOAT", "DOUBLE", 
		"DECIMAL", "NUMERIC", "CHAR", "VARCHAR", "TEXT", "DATE", "TIME", "DATETIME", 
		"TIMESTAMP", "BOOLEAN", "BOOL", "BLOB", "DOT", "COMMA", "LPAREN", "RPAREN", 
		"SEMICOLON", "IDENT", "QUOTED_IDENT", "NUMBER", "STRING", "WS", "LINE_COMMENT"
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
	public String getGrammarFileName() { return "ddl.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public ddlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class DdlStatementContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(ddlParser.EOF, 0); }
		public List<CreateTableStmtContext> createTableStmt() {
			return getRuleContexts(CreateTableStmtContext.class);
		}
		public CreateTableStmtContext createTableStmt(int i) {
			return getRuleContext(CreateTableStmtContext.class,i);
		}
		public List<TerminalNode> SEMICOLON() { return getTokens(ddlParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(ddlParser.SEMICOLON, i);
		}
		public DdlStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ddlStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterDdlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitDdlStatement(this);
		}
	}

	public final DdlStatementContext ddlStatement() throws RecognitionException {
		DdlStatementContext _localctx = new DdlStatementContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_ddlStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(26);
				createTableStmt();
				setState(28);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(27);
					match(SEMICOLON);
					}
				}

				}
				}
				setState(32); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CREATE );
			setState(34);
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

	public static class CreateTableStmtContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(ddlParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(ddlParser.TABLE, 0); }
		public TableNameContext tableName() {
			return getRuleContext(TableNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(ddlParser.LPAREN, 0); }
		public TableElementListContext tableElementList() {
			return getRuleContext(TableElementListContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(ddlParser.RPAREN, 0); }
		public CreateTableStmtContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createTableStmt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterCreateTableStmt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitCreateTableStmt(this);
		}
	}

	public final CreateTableStmtContext createTableStmt() throws RecognitionException {
		CreateTableStmtContext _localctx = new CreateTableStmtContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_createTableStmt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			match(CREATE);
			setState(37);
			match(TABLE);
			setState(38);
			tableName();
			setState(39);
			match(LPAREN);
			setState(40);
			tableElementList();
			setState(41);
			match(RPAREN);
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

	public static class TableElementListContext extends ParserRuleContext {
		public List<ColumnDefContext> columnDef() {
			return getRuleContexts(ColumnDefContext.class);
		}
		public ColumnDefContext columnDef(int i) {
			return getRuleContext(ColumnDefContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(ddlParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(ddlParser.COMMA, i);
		}
		public TableElementListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tableElementList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTableElementList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTableElementList(this);
		}
	}

	public final TableElementListContext tableElementList() throws RecognitionException {
		TableElementListContext _localctx = new TableElementListContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_tableElementList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			columnDef();
			setState(48);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(44);
				match(COMMA);
				setState(45);
				columnDef();
				}
				}
				setState(50);
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

	public static class ColumnDefContext extends ParserRuleContext {
		public ColumnNameContext columnName() {
			return getRuleContext(ColumnNameContext.class,0);
		}
		public DataTypeContext dataType() {
			return getRuleContext(DataTypeContext.class,0);
		}
		public List<ColumnConstraintContext> columnConstraint() {
			return getRuleContexts(ColumnConstraintContext.class);
		}
		public ColumnConstraintContext columnConstraint(int i) {
			return getRuleContext(ColumnConstraintContext.class,i);
		}
		public ColumnDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnDef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterColumnDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitColumnDef(this);
		}
	}

	public final ColumnDefContext columnDef() throws RecognitionException {
		ColumnDefContext _localctx = new ColumnDefContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_columnDef);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51);
			columnName();
			setState(52);
			dataType();
			setState(56);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PRIMARY || _la==NOT) {
				{
				{
				setState(53);
				columnConstraint();
				}
				}
				setState(58);
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

	public static class ColumnConstraintContext extends ParserRuleContext {
		public TerminalNode PRIMARY() { return getToken(ddlParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(ddlParser.KEY, 0); }
		public TerminalNode NOT() { return getToken(ddlParser.NOT, 0); }
		public TerminalNode NULL_T() { return getToken(ddlParser.NULL_T, 0); }
		public ColumnConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnConstraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterColumnConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitColumnConstraint(this);
		}
	}

	public final ColumnConstraintContext columnConstraint() throws RecognitionException {
		ColumnConstraintContext _localctx = new ColumnConstraintContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_columnConstraint);
		try {
			setState(63);
			switch (_input.LA(1)) {
			case PRIMARY:
				enterOuterAlt(_localctx, 1);
				{
				setState(59);
				match(PRIMARY);
				setState(60);
				match(KEY);
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(61);
				match(NOT);
				setState(62);
				match(NULL_T);
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

	public static class DataTypeContext extends ParserRuleContext {
		public DataTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dataType; }
	 
		public DataTypeContext() { }
		public void copyFrom(DataTypeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DoubleTypeContext extends DataTypeContext {
		public TerminalNode DOUBLE() { return getToken(ddlParser.DOUBLE, 0); }
		public DoubleTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterDoubleType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitDoubleType(this);
		}
	}
	public static class CharTypeContext extends DataTypeContext {
		public TerminalNode CHAR() { return getToken(ddlParser.CHAR, 0); }
		public TerminalNode LPAREN() { return getToken(ddlParser.LPAREN, 0); }
		public LengthContext length() {
			return getRuleContext(LengthContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(ddlParser.RPAREN, 0); }
		public CharTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterCharType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitCharType(this);
		}
	}
	public static class DecimalTypeContext extends DataTypeContext {
		public TerminalNode DECIMAL() { return getToken(ddlParser.DECIMAL, 0); }
		public TerminalNode LPAREN() { return getToken(ddlParser.LPAREN, 0); }
		public PrecisionContext precision() {
			return getRuleContext(PrecisionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(ddlParser.RPAREN, 0); }
		public TerminalNode COMMA() { return getToken(ddlParser.COMMA, 0); }
		public ScaleContext scale() {
			return getRuleContext(ScaleContext.class,0);
		}
		public DecimalTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterDecimalType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitDecimalType(this);
		}
	}
	public static class BooleanTypeContext extends DataTypeContext {
		public TerminalNode BOOLEAN() { return getToken(ddlParser.BOOLEAN, 0); }
		public BooleanTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterBooleanType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitBooleanType(this);
		}
	}
	public static class IntegerTypeContext extends DataTypeContext {
		public TerminalNode INTEGER() { return getToken(ddlParser.INTEGER, 0); }
		public IntegerTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterIntegerType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitIntegerType(this);
		}
	}
	public static class IntTypeContext extends DataTypeContext {
		public TerminalNode INT() { return getToken(ddlParser.INT, 0); }
		public IntTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterIntType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitIntType(this);
		}
	}
	public static class TimeTypeContext extends DataTypeContext {
		public TerminalNode TIME() { return getToken(ddlParser.TIME, 0); }
		public TimeTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTimeType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTimeType(this);
		}
	}
	public static class TextTypeContext extends DataTypeContext {
		public TerminalNode TEXT() { return getToken(ddlParser.TEXT, 0); }
		public TextTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTextType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTextType(this);
		}
	}
	public static class TinyintTypeContext extends DataTypeContext {
		public TerminalNode TINYINT() { return getToken(ddlParser.TINYINT, 0); }
		public TinyintTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTinyintType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTinyintType(this);
		}
	}
	public static class BoolTypeContext extends DataTypeContext {
		public TerminalNode BOOL() { return getToken(ddlParser.BOOL, 0); }
		public BoolTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterBoolType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitBoolType(this);
		}
	}
	public static class SmallintTypeContext extends DataTypeContext {
		public TerminalNode SMALLINT() { return getToken(ddlParser.SMALLINT, 0); }
		public SmallintTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterSmallintType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitSmallintType(this);
		}
	}
	public static class FloatTypeContext extends DataTypeContext {
		public TerminalNode FLOAT() { return getToken(ddlParser.FLOAT, 0); }
		public FloatTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterFloatType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitFloatType(this);
		}
	}
	public static class DateTypeContext extends DataTypeContext {
		public TerminalNode DATE() { return getToken(ddlParser.DATE, 0); }
		public DateTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterDateType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitDateType(this);
		}
	}
	public static class DatetimeTypeContext extends DataTypeContext {
		public TerminalNode DATETIME() { return getToken(ddlParser.DATETIME, 0); }
		public DatetimeTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterDatetimeType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitDatetimeType(this);
		}
	}
	public static class NumericTypeContext extends DataTypeContext {
		public TerminalNode NUMERIC() { return getToken(ddlParser.NUMERIC, 0); }
		public TerminalNode LPAREN() { return getToken(ddlParser.LPAREN, 0); }
		public PrecisionContext precision() {
			return getRuleContext(PrecisionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(ddlParser.RPAREN, 0); }
		public TerminalNode COMMA() { return getToken(ddlParser.COMMA, 0); }
		public ScaleContext scale() {
			return getRuleContext(ScaleContext.class,0);
		}
		public NumericTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterNumericType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitNumericType(this);
		}
	}
	public static class TimestampTypeContext extends DataTypeContext {
		public TerminalNode TIMESTAMP() { return getToken(ddlParser.TIMESTAMP, 0); }
		public TimestampTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTimestampType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTimestampType(this);
		}
	}
	public static class BigintTypeContext extends DataTypeContext {
		public TerminalNode BIGINT() { return getToken(ddlParser.BIGINT, 0); }
		public BigintTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterBigintType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitBigintType(this);
		}
	}
	public static class BlobTypeContext extends DataTypeContext {
		public TerminalNode BLOB() { return getToken(ddlParser.BLOB, 0); }
		public BlobTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterBlobType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitBlobType(this);
		}
	}
	public static class VarcharTypeContext extends DataTypeContext {
		public TerminalNode VARCHAR() { return getToken(ddlParser.VARCHAR, 0); }
		public TerminalNode LPAREN() { return getToken(ddlParser.LPAREN, 0); }
		public LengthContext length() {
			return getRuleContext(LengthContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(ddlParser.RPAREN, 0); }
		public VarcharTypeContext(DataTypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterVarcharType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitVarcharType(this);
		}
	}

	public final DataTypeContext dataType() throws RecognitionException {
		DataTypeContext _localctx = new DataTypeContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_dataType);
		int _la;
		try {
			setState(116);
			switch (_input.LA(1)) {
			case INT:
				_localctx = new IntTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(65);
				match(INT);
				}
				break;
			case INTEGER:
				_localctx = new IntegerTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				match(INTEGER);
				}
				break;
			case SMALLINT:
				_localctx = new SmallintTypeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(67);
				match(SMALLINT);
				}
				break;
			case BIGINT:
				_localctx = new BigintTypeContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(68);
				match(BIGINT);
				}
				break;
			case TINYINT:
				_localctx = new TinyintTypeContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(69);
				match(TINYINT);
				}
				break;
			case FLOAT:
				_localctx = new FloatTypeContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(70);
				match(FLOAT);
				}
				break;
			case DOUBLE:
				_localctx = new DoubleTypeContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(71);
				match(DOUBLE);
				}
				break;
			case DECIMAL:
				_localctx = new DecimalTypeContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(72);
				match(DECIMAL);
				setState(81);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(73);
					match(LPAREN);
					setState(74);
					precision();
					setState(77);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(75);
						match(COMMA);
						setState(76);
						scale();
						}
					}

					setState(79);
					match(RPAREN);
					}
				}

				}
				break;
			case NUMERIC:
				_localctx = new NumericTypeContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(83);
				match(NUMERIC);
				setState(92);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(84);
					match(LPAREN);
					setState(85);
					precision();
					setState(88);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(86);
						match(COMMA);
						setState(87);
						scale();
						}
					}

					setState(90);
					match(RPAREN);
					}
				}

				}
				break;
			case CHAR:
				_localctx = new CharTypeContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(94);
				match(CHAR);
				setState(99);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(95);
					match(LPAREN);
					setState(96);
					length();
					setState(97);
					match(RPAREN);
					}
				}

				}
				break;
			case VARCHAR:
				_localctx = new VarcharTypeContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(101);
				match(VARCHAR);
				setState(106);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(102);
					match(LPAREN);
					setState(103);
					length();
					setState(104);
					match(RPAREN);
					}
				}

				}
				break;
			case TEXT:
				_localctx = new TextTypeContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(108);
				match(TEXT);
				}
				break;
			case DATE:
				_localctx = new DateTypeContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(109);
				match(DATE);
				}
				break;
			case TIME:
				_localctx = new TimeTypeContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(110);
				match(TIME);
				}
				break;
			case DATETIME:
				_localctx = new DatetimeTypeContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(111);
				match(DATETIME);
				}
				break;
			case TIMESTAMP:
				_localctx = new TimestampTypeContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(112);
				match(TIMESTAMP);
				}
				break;
			case BOOLEAN:
				_localctx = new BooleanTypeContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(113);
				match(BOOLEAN);
				}
				break;
			case BOOL:
				_localctx = new BoolTypeContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(114);
				match(BOOL);
				}
				break;
			case BLOB:
				_localctx = new BlobTypeContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(115);
				match(BLOB);
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

	public static class PrecisionContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(ddlParser.NUMBER, 0); }
		public PrecisionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_precision; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterPrecision(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitPrecision(this);
		}
	}

	public final PrecisionContext precision() throws RecognitionException {
		PrecisionContext _localctx = new PrecisionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_precision);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			match(NUMBER);
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

	public static class ScaleContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(ddlParser.NUMBER, 0); }
		public ScaleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scale; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterScale(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitScale(this);
		}
	}

	public final ScaleContext scale() throws RecognitionException {
		ScaleContext _localctx = new ScaleContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_scale);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(120);
			match(NUMBER);
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

	public static class LengthContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(ddlParser.NUMBER, 0); }
		public LengthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_length; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterLength(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitLength(this);
		}
	}

	public final LengthContext length() throws RecognitionException {
		LengthContext _localctx = new LengthContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_length);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			match(NUMBER);
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
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterTableName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitTableName(this);
		}
	}

	public final TableNameContext tableName() throws RecognitionException {
		TableNameContext _localctx = new TableNameContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_tableName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
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

	public static class ColumnNameContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public ColumnNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterColumnName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitColumnName(this);
		}
	}

	public final ColumnNameContext columnName() throws RecognitionException {
		ColumnNameContext _localctx = new ColumnNameContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_columnName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(126);
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

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode QUOTED_IDENT() { return getToken(ddlParser.QUOTED_IDENT, 0); }
		public TerminalNode IDENT() { return getToken(ddlParser.IDENT, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitIdentifier(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
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

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode NULL_T() { return getToken(ddlParser.NULL_T, 0); }
		public TerminalNode NUMBER() { return getToken(ddlParser.NUMBER, 0); }
		public TerminalNode STRING() { return getToken(ddlParser.STRING, 0); }
		public TerminalNode TRUE() { return getToken(ddlParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(ddlParser.FALSE, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof ddlListener ) ((ddlListener)listener).exitLiteral(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(130);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NULL_T) | (1L << TRUE) | (1L << FALSE) | (1L << NUMBER) | (1L << STRING))) != 0)) ) {
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

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3(\u0087\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\5\2\37\n\2\6\2!\n\2\r\2\16\2\"\3"+
		"\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\7\4\61\n\4\f\4\16\4\64"+
		"\13\4\3\5\3\5\3\5\7\59\n\5\f\5\16\5<\13\5\3\6\3\6\3\6\3\6\5\6B\n\6\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7P\n\7\3\7\3\7\5\7T\n\7"+
		"\3\7\3\7\3\7\3\7\3\7\5\7[\n\7\3\7\3\7\5\7_\n\7\3\7\3\7\3\7\3\7\3\7\5\7"+
		"f\n\7\3\7\3\7\3\7\3\7\3\7\5\7m\n\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7"+
		"w\n\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\16"+
		"\2\2\17\2\4\6\b\n\f\16\20\22\24\26\30\32\2\4\3\2#$\4\2\b\n%&\u0096\2 "+
		"\3\2\2\2\4&\3\2\2\2\6-\3\2\2\2\b\65\3\2\2\2\nA\3\2\2\2\fv\3\2\2\2\16x"+
		"\3\2\2\2\20z\3\2\2\2\22|\3\2\2\2\24~\3\2\2\2\26\u0080\3\2\2\2\30\u0082"+
		"\3\2\2\2\32\u0084\3\2\2\2\34\36\5\4\3\2\35\37\7\"\2\2\36\35\3\2\2\2\36"+
		"\37\3\2\2\2\37!\3\2\2\2 \34\3\2\2\2!\"\3\2\2\2\" \3\2\2\2\"#\3\2\2\2#"+
		"$\3\2\2\2$%\7\2\2\3%\3\3\2\2\2&\'\7\3\2\2\'(\7\4\2\2()\5\24\13\2)*\7 "+
		"\2\2*+\5\6\4\2+,\7!\2\2,\5\3\2\2\2-\62\5\b\5\2./\7\37\2\2/\61\5\b\5\2"+
		"\60.\3\2\2\2\61\64\3\2\2\2\62\60\3\2\2\2\62\63\3\2\2\2\63\7\3\2\2\2\64"+
		"\62\3\2\2\2\65\66\5\26\f\2\66:\5\f\7\2\679\5\n\6\28\67\3\2\2\29<\3\2\2"+
		"\2:8\3\2\2\2:;\3\2\2\2;\t\3\2\2\2<:\3\2\2\2=>\7\5\2\2>B\7\6\2\2?@\7\7"+
		"\2\2@B\7\b\2\2A=\3\2\2\2A?\3\2\2\2B\13\3\2\2\2Cw\7\13\2\2Dw\7\f\2\2Ew"+
		"\7\r\2\2Fw\7\16\2\2Gw\7\17\2\2Hw\7\20\2\2Iw\7\21\2\2JS\7\22\2\2KL\7 \2"+
		"\2LO\5\16\b\2MN\7\37\2\2NP\5\20\t\2OM\3\2\2\2OP\3\2\2\2PQ\3\2\2\2QR\7"+
		"!\2\2RT\3\2\2\2SK\3\2\2\2ST\3\2\2\2Tw\3\2\2\2U^\7\23\2\2VW\7 \2\2WZ\5"+
		"\16\b\2XY\7\37\2\2Y[\5\20\t\2ZX\3\2\2\2Z[\3\2\2\2[\\\3\2\2\2\\]\7!\2\2"+
		"]_\3\2\2\2^V\3\2\2\2^_\3\2\2\2_w\3\2\2\2`e\7\24\2\2ab\7 \2\2bc\5\22\n"+
		"\2cd\7!\2\2df\3\2\2\2ea\3\2\2\2ef\3\2\2\2fw\3\2\2\2gl\7\25\2\2hi\7 \2"+
		"\2ij\5\22\n\2jk\7!\2\2km\3\2\2\2lh\3\2\2\2lm\3\2\2\2mw\3\2\2\2nw\7\26"+
		"\2\2ow\7\27\2\2pw\7\30\2\2qw\7\31\2\2rw\7\32\2\2sw\7\33\2\2tw\7\34\2\2"+
		"uw\7\35\2\2vC\3\2\2\2vD\3\2\2\2vE\3\2\2\2vF\3\2\2\2vG\3\2\2\2vH\3\2\2"+
		"\2vI\3\2\2\2vJ\3\2\2\2vU\3\2\2\2v`\3\2\2\2vg\3\2\2\2vn\3\2\2\2vo\3\2\2"+
		"\2vp\3\2\2\2vq\3\2\2\2vr\3\2\2\2vs\3\2\2\2vt\3\2\2\2vu\3\2\2\2w\r\3\2"+
		"\2\2xy\7%\2\2y\17\3\2\2\2z{\7%\2\2{\21\3\2\2\2|}\7%\2\2}\23\3\2\2\2~\177"+
		"\5\30\r\2\177\25\3\2\2\2\u0080\u0081\5\30\r\2\u0081\27\3\2\2\2\u0082\u0083"+
		"\t\2\2\2\u0083\31\3\2\2\2\u0084\u0085\t\3\2\2\u0085\33\3\2\2\2\16\36\""+
		"\62:AOSZ^elv";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}