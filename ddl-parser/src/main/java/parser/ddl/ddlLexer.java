// Generated from parser/ddl/ddl.g4 by ANTLR 4.5
package parser.ddl;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class ddlLexer extends Lexer {
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
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"CREATE", "TABLE", "PRIMARY", "KEY", "NOT", "NULL_T", "TRUE", "FALSE", 
		"INT", "INTEGER", "SMALLINT", "BIGINT", "TINYINT", "FLOAT", "DOUBLE", 
		"DECIMAL", "NUMERIC", "CHAR", "VARCHAR", "TEXT", "DATE", "TIME", "DATETIME", 
		"TIMESTAMP", "BOOLEAN", "BOOL", "BLOB", "DOT", "COMMA", "LPAREN", "RPAREN", 
		"SEMICOLON", "IDENT", "QUOTED_IDENT", "NUMBER", "STRING", "A", "B", "C", 
		"D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", 
		"R", "S", "T", "U", "V", "W", "X", "Y", "Z", "DIGIT", "WS", "LINE_COMMENT"
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


	public ddlLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "ddl.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2(\u01bb\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3"+
		"\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3"+
		"\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26"+
		"\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34"+
		"\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\7\"\u0141\n\"\f\"\16"+
		"\"\u0144\13\"\3#\3#\3#\3#\7#\u014a\n#\f#\16#\u014d\13#\3#\3#\3#\3#\3#"+
		"\7#\u0154\n#\f#\16#\u0157\13#\3#\5#\u015a\n#\3$\6$\u015d\n$\r$\16$\u015e"+
		"\3$\3$\6$\u0163\n$\r$\16$\u0164\5$\u0167\n$\3%\3%\3%\3%\7%\u016d\n%\f"+
		"%\16%\u0170\13%\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3"+
		"-\3.\3.\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3"+
		"\65\3\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3"+
		"?\3@\3@\3A\6A\u01ab\nA\rA\16A\u01ac\3A\3A\3B\3B\3B\3B\7B\u01b5\nB\fB\16"+
		"B\u01b8\13B\3B\3B\2\2C\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27"+
		"\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
		"\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\2M\2O\2Q\2S\2U\2W\2Y\2[\2]\2_\2a"+
		"\2c\2e\2g\2i\2k\2m\2o\2q\2s\2u\2w\2y\2{\2}\2\177\2\u0081\'\u0083(\3\2"+
		"$\5\2C\\aac|\6\2\62;C\\aac|\4\2$$^^\4\2^^bb\4\2))^^\4\2CCcc\4\2DDdd\4"+
		"\2EEee\4\2FFff\4\2GGgg\4\2HHhh\4\2IIii\4\2JJjj\4\2KKkk\4\2LLll\4\2MMm"+
		"m\4\2NNnn\4\2OOoo\4\2PPpp\4\2QQqq\4\2RRrr\4\2SSss\4\2TTtt\4\2UUuu\4\2"+
		"VVvv\4\2WWww\4\2XXxx\4\2YYyy\4\2ZZzz\4\2[[{{\4\2\\\\||\3\2\62;\5\2\13"+
		"\f\17\17\"\"\4\2\f\f\17\17\u01ac\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2"+
		"\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2"+
		"\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2"+
		"\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2"+
		"\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2"+
		"\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2"+
		"\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3"+
		"\2\2\2\3\u0085\3\2\2\2\5\u008c\3\2\2\2\7\u0092\3\2\2\2\t\u009a\3\2\2\2"+
		"\13\u009e\3\2\2\2\r\u00a2\3\2\2\2\17\u00a7\3\2\2\2\21\u00ac\3\2\2\2\23"+
		"\u00b2\3\2\2\2\25\u00b6\3\2\2\2\27\u00be\3\2\2\2\31\u00c7\3\2\2\2\33\u00ce"+
		"\3\2\2\2\35\u00d6\3\2\2\2\37\u00dc\3\2\2\2!\u00e3\3\2\2\2#\u00eb\3\2\2"+
		"\2%\u00f3\3\2\2\2\'\u00f8\3\2\2\2)\u0100\3\2\2\2+\u0105\3\2\2\2-\u010a"+
		"\3\2\2\2/\u010f\3\2\2\2\61\u0118\3\2\2\2\63\u0122\3\2\2\2\65\u012a\3\2"+
		"\2\2\67\u012f\3\2\2\29\u0134\3\2\2\2;\u0136\3\2\2\2=\u0138\3\2\2\2?\u013a"+
		"\3\2\2\2A\u013c\3\2\2\2C\u013e\3\2\2\2E\u0159\3\2\2\2G\u015c\3\2\2\2I"+
		"\u0168\3\2\2\2K\u0173\3\2\2\2M\u0175\3\2\2\2O\u0177\3\2\2\2Q\u0179\3\2"+
		"\2\2S\u017b\3\2\2\2U\u017d\3\2\2\2W\u017f\3\2\2\2Y\u0181\3\2\2\2[\u0183"+
		"\3\2\2\2]\u0185\3\2\2\2_\u0187\3\2\2\2a\u0189\3\2\2\2c\u018b\3\2\2\2e"+
		"\u018d\3\2\2\2g\u018f\3\2\2\2i\u0191\3\2\2\2k\u0193\3\2\2\2m\u0195\3\2"+
		"\2\2o\u0197\3\2\2\2q\u0199\3\2\2\2s\u019b\3\2\2\2u\u019d\3\2\2\2w\u019f"+
		"\3\2\2\2y\u01a1\3\2\2\2{\u01a3\3\2\2\2}\u01a5\3\2\2\2\177\u01a7\3\2\2"+
		"\2\u0081\u01aa\3\2\2\2\u0083\u01b0\3\2\2\2\u0085\u0086\5O(\2\u0086\u0087"+
		"\5m\67\2\u0087\u0088\5S*\2\u0088\u0089\5K&\2\u0089\u008a\5q9\2\u008a\u008b"+
		"\5S*\2\u008b\4\3\2\2\2\u008c\u008d\5q9\2\u008d\u008e\5K&\2\u008e\u008f"+
		"\5M\'\2\u008f\u0090\5a\61\2\u0090\u0091\5S*\2\u0091\6\3\2\2\2\u0092\u0093"+
		"\5i\65\2\u0093\u0094\5m\67\2\u0094\u0095\5[.\2\u0095\u0096\5c\62\2\u0096"+
		"\u0097\5K&\2\u0097\u0098\5m\67\2\u0098\u0099\5{>\2\u0099\b\3\2\2\2\u009a"+
		"\u009b\5_\60\2\u009b\u009c\5S*\2\u009c\u009d\5{>\2\u009d\n\3\2\2\2\u009e"+
		"\u009f\5e\63\2\u009f\u00a0\5g\64\2\u00a0\u00a1\5q9\2\u00a1\f\3\2\2\2\u00a2"+
		"\u00a3\5e\63\2\u00a3\u00a4\5s:\2\u00a4\u00a5\5a\61\2\u00a5\u00a6\5a\61"+
		"\2\u00a6\16\3\2\2\2\u00a7\u00a8\5q9\2\u00a8\u00a9\5m\67\2\u00a9\u00aa"+
		"\5s:\2\u00aa\u00ab\5S*\2\u00ab\20\3\2\2\2\u00ac\u00ad\5U+\2\u00ad\u00ae"+
		"\5K&\2\u00ae\u00af\5a\61\2\u00af\u00b0\5o8\2\u00b0\u00b1\5S*\2\u00b1\22"+
		"\3\2\2\2\u00b2\u00b3\5[.\2\u00b3\u00b4\5e\63\2\u00b4\u00b5\5q9\2\u00b5"+
		"\24\3\2\2\2\u00b6\u00b7\5[.\2\u00b7\u00b8\5e\63\2\u00b8\u00b9\5q9\2\u00b9"+
		"\u00ba\5S*\2\u00ba\u00bb\5W,\2\u00bb\u00bc\5S*\2\u00bc\u00bd\5m\67\2\u00bd"+
		"\26\3\2\2\2\u00be\u00bf\5o8\2\u00bf\u00c0\5c\62\2\u00c0\u00c1\5K&\2\u00c1"+
		"\u00c2\5a\61\2\u00c2\u00c3\5a\61\2\u00c3\u00c4\5[.\2\u00c4\u00c5\5e\63"+
		"\2\u00c5\u00c6\5q9\2\u00c6\30\3\2\2\2\u00c7\u00c8\5M\'\2\u00c8\u00c9\5"+
		"[.\2\u00c9\u00ca\5W,\2\u00ca\u00cb\5[.\2\u00cb\u00cc\5e\63\2\u00cc\u00cd"+
		"\5q9\2\u00cd\32\3\2\2\2\u00ce\u00cf\5q9\2\u00cf\u00d0\5[.\2\u00d0\u00d1"+
		"\5e\63\2\u00d1\u00d2\5{>\2\u00d2\u00d3\5[.\2\u00d3\u00d4\5e\63\2\u00d4"+
		"\u00d5\5q9\2\u00d5\34\3\2\2\2\u00d6\u00d7\5U+\2\u00d7\u00d8\5a\61\2\u00d8"+
		"\u00d9\5g\64\2\u00d9\u00da\5K&\2\u00da\u00db\5q9\2\u00db\36\3\2\2\2\u00dc"+
		"\u00dd\5Q)\2\u00dd\u00de\5g\64\2\u00de\u00df\5s:\2\u00df\u00e0\5M\'\2"+
		"\u00e0\u00e1\5a\61\2\u00e1\u00e2\5S*\2\u00e2 \3\2\2\2\u00e3\u00e4\5Q)"+
		"\2\u00e4\u00e5\5S*\2\u00e5\u00e6\5O(\2\u00e6\u00e7\5[.\2\u00e7\u00e8\5"+
		"c\62\2\u00e8\u00e9\5K&\2\u00e9\u00ea\5a\61\2\u00ea\"\3\2\2\2\u00eb\u00ec"+
		"\5e\63\2\u00ec\u00ed\5s:\2\u00ed\u00ee\5c\62\2\u00ee\u00ef\5S*\2\u00ef"+
		"\u00f0\5m\67\2\u00f0\u00f1\5[.\2\u00f1\u00f2\5O(\2\u00f2$\3\2\2\2\u00f3"+
		"\u00f4\5O(\2\u00f4\u00f5\5Y-\2\u00f5\u00f6\5K&\2\u00f6\u00f7\5m\67\2\u00f7"+
		"&\3\2\2\2\u00f8\u00f9\5u;\2\u00f9\u00fa\5K&\2\u00fa\u00fb\5m\67\2\u00fb"+
		"\u00fc\5O(\2\u00fc\u00fd\5Y-\2\u00fd\u00fe\5K&\2\u00fe\u00ff\5m\67\2\u00ff"+
		"(\3\2\2\2\u0100\u0101\5q9\2\u0101\u0102\5S*\2\u0102\u0103\5y=\2\u0103"+
		"\u0104\5q9\2\u0104*\3\2\2\2\u0105\u0106\5Q)\2\u0106\u0107\5K&\2\u0107"+
		"\u0108\5q9\2\u0108\u0109\5S*\2\u0109,\3\2\2\2\u010a\u010b\5q9\2\u010b"+
		"\u010c\5[.\2\u010c\u010d\5c\62\2\u010d\u010e\5S*\2\u010e.\3\2\2\2\u010f"+
		"\u0110\5Q)\2\u0110\u0111\5K&\2\u0111\u0112\5q9\2\u0112\u0113\5S*\2\u0113"+
		"\u0114\5q9\2\u0114\u0115\5[.\2\u0115\u0116\5c\62\2\u0116\u0117\5S*\2\u0117"+
		"\60\3\2\2\2\u0118\u0119\5q9\2\u0119\u011a\5[.\2\u011a\u011b\5c\62\2\u011b"+
		"\u011c\5S*\2\u011c\u011d\5o8\2\u011d\u011e\5q9\2\u011e\u011f\5K&\2\u011f"+
		"\u0120\5c\62\2\u0120\u0121\5i\65\2\u0121\62\3\2\2\2\u0122\u0123\5M\'\2"+
		"\u0123\u0124\5g\64\2\u0124\u0125\5g\64\2\u0125\u0126\5a\61\2\u0126\u0127"+
		"\5S*\2\u0127\u0128\5K&\2\u0128\u0129\5e\63\2\u0129\64\3\2\2\2\u012a\u012b"+
		"\5M\'\2\u012b\u012c\5g\64\2\u012c\u012d\5g\64\2\u012d\u012e\5a\61\2\u012e"+
		"\66\3\2\2\2\u012f\u0130\5M\'\2\u0130\u0131\5a\61\2\u0131\u0132\5g\64\2"+
		"\u0132\u0133\5M\'\2\u01338\3\2\2\2\u0134\u0135\7\60\2\2\u0135:\3\2\2\2"+
		"\u0136\u0137\7.\2\2\u0137<\3\2\2\2\u0138\u0139\7*\2\2\u0139>\3\2\2\2\u013a"+
		"\u013b\7+\2\2\u013b@\3\2\2\2\u013c\u013d\7=\2\2\u013dB\3\2\2\2\u013e\u0142"+
		"\t\2\2\2\u013f\u0141\t\3\2\2\u0140\u013f\3\2\2\2\u0141\u0144\3\2\2\2\u0142"+
		"\u0140\3\2\2\2\u0142\u0143\3\2\2\2\u0143D\3\2\2\2\u0144\u0142\3\2\2\2"+
		"\u0145\u014b\7$\2\2\u0146\u014a\n\4\2\2\u0147\u0148\7^\2\2\u0148\u014a"+
		"\13\2\2\2\u0149\u0146\3\2\2\2\u0149\u0147\3\2\2\2\u014a\u014d\3\2\2\2"+
		"\u014b\u0149\3\2\2\2\u014b\u014c\3\2\2\2\u014c\u014e\3\2\2\2\u014d\u014b"+
		"\3\2\2\2\u014e\u015a\7$\2\2\u014f\u0155\7b\2\2\u0150\u0154\n\5\2\2\u0151"+
		"\u0152\7^\2\2\u0152\u0154\13\2\2\2\u0153\u0150\3\2\2\2\u0153\u0151\3\2"+
		"\2\2\u0154\u0157\3\2\2\2\u0155\u0153\3\2\2\2\u0155\u0156\3\2\2\2\u0156"+
		"\u0158\3\2\2\2\u0157\u0155\3\2\2\2\u0158\u015a\7b\2\2\u0159\u0145\3\2"+
		"\2\2\u0159\u014f\3\2\2\2\u015aF\3\2\2\2\u015b\u015d\5\177@\2\u015c\u015b"+
		"\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015c\3\2\2\2\u015e\u015f\3\2\2\2\u015f"+
		"\u0166\3\2\2\2\u0160\u0162\7\60\2\2\u0161\u0163\5\177@\2\u0162\u0161\3"+
		"\2\2\2\u0163\u0164\3\2\2\2\u0164\u0162\3\2\2\2\u0164\u0165\3\2\2\2\u0165"+
		"\u0167\3\2\2\2\u0166\u0160\3\2\2\2\u0166\u0167\3\2\2\2\u0167H\3\2\2\2"+
		"\u0168\u016e\7)\2\2\u0169\u016d\n\6\2\2\u016a\u016b\7^\2\2\u016b\u016d"+
		"\13\2\2\2\u016c\u0169\3\2\2\2\u016c\u016a\3\2\2\2\u016d\u0170\3\2\2\2"+
		"\u016e\u016c\3\2\2\2\u016e\u016f\3\2\2\2\u016f\u0171\3\2\2\2\u0170\u016e"+
		"\3\2\2\2\u0171\u0172\7)\2\2\u0172J\3\2\2\2\u0173\u0174\t\7\2\2\u0174L"+
		"\3\2\2\2\u0175\u0176\t\b\2\2\u0176N\3\2\2\2\u0177\u0178\t\t\2\2\u0178"+
		"P\3\2\2\2\u0179\u017a\t\n\2\2\u017aR\3\2\2\2\u017b\u017c\t\13\2\2\u017c"+
		"T\3\2\2\2\u017d\u017e\t\f\2\2\u017eV\3\2\2\2\u017f\u0180\t\r\2\2\u0180"+
		"X\3\2\2\2\u0181\u0182\t\16\2\2\u0182Z\3\2\2\2\u0183\u0184\t\17\2\2\u0184"+
		"\\\3\2\2\2\u0185\u0186\t\20\2\2\u0186^\3\2\2\2\u0187\u0188\t\21\2\2\u0188"+
		"`\3\2\2\2\u0189\u018a\t\22\2\2\u018ab\3\2\2\2\u018b\u018c\t\23\2\2\u018c"+
		"d\3\2\2\2\u018d\u018e\t\24\2\2\u018ef\3\2\2\2\u018f\u0190\t\25\2\2\u0190"+
		"h\3\2\2\2\u0191\u0192\t\26\2\2\u0192j\3\2\2\2\u0193\u0194\t\27\2\2\u0194"+
		"l\3\2\2\2\u0195\u0196\t\30\2\2\u0196n\3\2\2\2\u0197\u0198\t\31\2\2\u0198"+
		"p\3\2\2\2\u0199\u019a\t\32\2\2\u019ar\3\2\2\2\u019b\u019c\t\33\2\2\u019c"+
		"t\3\2\2\2\u019d\u019e\t\34\2\2\u019ev\3\2\2\2\u019f\u01a0\t\35\2\2\u01a0"+
		"x\3\2\2\2\u01a1\u01a2\t\36\2\2\u01a2z\3\2\2\2\u01a3\u01a4\t\37\2\2\u01a4"+
		"|\3\2\2\2\u01a5\u01a6\t \2\2\u01a6~\3\2\2\2\u01a7\u01a8\t!\2\2\u01a8\u0080"+
		"\3\2\2\2\u01a9\u01ab\t\"\2\2\u01aa\u01a9\3\2\2\2\u01ab\u01ac\3\2\2\2\u01ac"+
		"\u01aa\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01af\bA"+
		"\2\2\u01af\u0082\3\2\2\2\u01b0\u01b1\7/\2\2\u01b1\u01b2\7/\2\2\u01b2\u01b6"+
		"\3\2\2\2\u01b3\u01b5\n#\2\2\u01b4\u01b3\3\2\2\2\u01b5\u01b8\3\2\2\2\u01b6"+
		"\u01b4\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b7\u01b9\3\2\2\2\u01b8\u01b6\3\2"+
		"\2\2\u01b9\u01ba\bB\2\2\u01ba\u0084\3\2\2\2\20\2\u0142\u0149\u014b\u0153"+
		"\u0155\u0159\u015e\u0164\u0166\u016c\u016e\u01ac\u01b6\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}