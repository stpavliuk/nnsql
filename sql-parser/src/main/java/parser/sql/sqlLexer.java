// Generated from parser/sql/sql.g4 by ANTLR 4.5
package parser.sql;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class sqlLexer extends Lexer {
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
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"SELECT", "DISTINCT", "FROM", "WHERE", "GROUP", "BY", "HAVING", "AS", 
		"UNION", "ALL", "EXCEPT", "IS", "NOT", "AND", "OR", "NULL_T", "COUNT", 
		"SUM", "AVG", "MIN", "MAX", "EQ", "NEQ", "LTE", "GTE", "LT", "GT", "PLUS", 
		"MINUS", "STAR", "DOT", "COMMA", "LPAREN", "RPAREN", "SEMICOLON", "IDENT", 
		"QUOTED_IDENT", "NUMBER", "STRING", "A", "B", "C", "D", "E", "F", "G", 
		"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", 
		"V", "W", "X", "Y", "Z", "DIGIT", "WS", "LINE_COMMENT", "BLOCK_COMMENT"
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


	public sqlLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "sql.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2,\u0194\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\b\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3"+
		"\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3"+
		"\17\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3"+
		"\22\3\22\3\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3"+
		"\25\3\26\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\30\3\31\3\31\3\31\3\32\3"+
		"\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3"+
		"!\3\"\3\"\3#\3#\3$\3$\3%\3%\7%\u0117\n%\f%\16%\u011a\13%\3&\3&\3&\3&\7"+
		"&\u0120\n&\f&\16&\u0123\13&\3&\3&\3\'\6\'\u0128\n\'\r\'\16\'\u0129\3\'"+
		"\3\'\6\'\u012e\n\'\r\'\16\'\u012f\5\'\u0132\n\'\3(\3(\3(\3(\7(\u0138\n"+
		"(\f(\16(\u013b\13(\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/\3\60"+
		"\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66\3\67"+
		"\3\67\38\38\39\39\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3?\3@\3@\3A\3A\3B\3"+
		"B\3C\3C\3D\6D\u0176\nD\rD\16D\u0177\3D\3D\3E\3E\3E\3E\7E\u0180\nE\fE\16"+
		"E\u0183\13E\3E\3E\3F\3F\3F\3F\7F\u018b\nF\fF\16F\u018e\13F\3F\3F\3F\3"+
		"F\3F\3\u018c\2G\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31"+
		"\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65"+
		"\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q\2S\2U\2W\2Y\2[\2]\2_\2a\2c\2"+
		"e\2g\2i\2k\2m\2o\2q\2s\2u\2w\2y\2{\2}\2\177\2\u0081\2\u0083\2\u0085\2"+
		"\u0087*\u0089+\u008b,\3\2#\5\2C\\aac|\6\2\62;C\\aac|\4\2$$^^\4\2))^^\4"+
		"\2CCcc\4\2DDdd\4\2EEee\4\2FFff\4\2GGgg\4\2HHhh\4\2IIii\4\2JJjj\4\2KKk"+
		"k\4\2LLll\4\2MMmm\4\2NNnn\4\2OOoo\4\2PPpp\4\2QQqq\4\2RRrr\4\2SSss\4\2"+
		"TTtt\4\2UUuu\4\2VVvv\4\2WWww\4\2XXxx\4\2YYyy\4\2ZZzz\4\2[[{{\4\2\\\\|"+
		"|\3\2\62;\5\2\13\f\17\17\"\"\4\2\f\f\17\17\u0183\2\3\3\2\2\2\2\5\3\2\2"+
		"\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21"+
		"\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2"+
		"\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3"+
		"\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3"+
		"\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3"+
		"\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2"+
		"\2\2M\3\2\2\2\2O\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\2\u008b\3\2\2"+
		"\2\3\u008d\3\2\2\2\5\u0094\3\2\2\2\7\u009d\3\2\2\2\t\u00a2\3\2\2\2\13"+
		"\u00a8\3\2\2\2\r\u00ae\3\2\2\2\17\u00b1\3\2\2\2\21\u00b8\3\2\2\2\23\u00bb"+
		"\3\2\2\2\25\u00c1\3\2\2\2\27\u00c5\3\2\2\2\31\u00cc\3\2\2\2\33\u00cf\3"+
		"\2\2\2\35\u00d3\3\2\2\2\37\u00d7\3\2\2\2!\u00da\3\2\2\2#\u00df\3\2\2\2"+
		"%\u00e5\3\2\2\2\'\u00e9\3\2\2\2)\u00ed\3\2\2\2+\u00f1\3\2\2\2-\u00f5\3"+
		"\2\2\2/\u00f7\3\2\2\2\61\u00fa\3\2\2\2\63\u00fd\3\2\2\2\65\u0100\3\2\2"+
		"\2\67\u0102\3\2\2\29\u0104\3\2\2\2;\u0106\3\2\2\2=\u0108\3\2\2\2?\u010a"+
		"\3\2\2\2A\u010c\3\2\2\2C\u010e\3\2\2\2E\u0110\3\2\2\2G\u0112\3\2\2\2I"+
		"\u0114\3\2\2\2K\u011b\3\2\2\2M\u0127\3\2\2\2O\u0133\3\2\2\2Q\u013e\3\2"+
		"\2\2S\u0140\3\2\2\2U\u0142\3\2\2\2W\u0144\3\2\2\2Y\u0146\3\2\2\2[\u0148"+
		"\3\2\2\2]\u014a\3\2\2\2_\u014c\3\2\2\2a\u014e\3\2\2\2c\u0150\3\2\2\2e"+
		"\u0152\3\2\2\2g\u0154\3\2\2\2i\u0156\3\2\2\2k\u0158\3\2\2\2m\u015a\3\2"+
		"\2\2o\u015c\3\2\2\2q\u015e\3\2\2\2s\u0160\3\2\2\2u\u0162\3\2\2\2w\u0164"+
		"\3\2\2\2y\u0166\3\2\2\2{\u0168\3\2\2\2}\u016a\3\2\2\2\177\u016c\3\2\2"+
		"\2\u0081\u016e\3\2\2\2\u0083\u0170\3\2\2\2\u0085\u0172\3\2\2\2\u0087\u0175"+
		"\3\2\2\2\u0089\u017b\3\2\2\2\u008b\u0186\3\2\2\2\u008d\u008e\5u;\2\u008e"+
		"\u008f\5Y-\2\u008f\u0090\5g\64\2\u0090\u0091\5Y-\2\u0091\u0092\5U+\2\u0092"+
		"\u0093\5w<\2\u0093\4\3\2\2\2\u0094\u0095\5W,\2\u0095\u0096\5a\61\2\u0096"+
		"\u0097\5u;\2\u0097\u0098\5w<\2\u0098\u0099\5a\61\2\u0099\u009a\5k\66\2"+
		"\u009a\u009b\5U+\2\u009b\u009c\5w<\2\u009c\6\3\2\2\2\u009d\u009e\5[.\2"+
		"\u009e\u009f\5s:\2\u009f\u00a0\5m\67\2\u00a0\u00a1\5i\65\2\u00a1\b\3\2"+
		"\2\2\u00a2\u00a3\5}?\2\u00a3\u00a4\5_\60\2\u00a4\u00a5\5Y-\2\u00a5\u00a6"+
		"\5s:\2\u00a6\u00a7\5Y-\2\u00a7\n\3\2\2\2\u00a8\u00a9\5]/\2\u00a9\u00aa"+
		"\5s:\2\u00aa\u00ab\5m\67\2\u00ab\u00ac\5y=\2\u00ac\u00ad\5o8\2\u00ad\f"+
		"\3\2\2\2\u00ae\u00af\5S*\2\u00af\u00b0\5\u0081A\2\u00b0\16\3\2\2\2\u00b1"+
		"\u00b2\5_\60\2\u00b2\u00b3\5Q)\2\u00b3\u00b4\5{>\2\u00b4\u00b5\5a\61\2"+
		"\u00b5\u00b6\5k\66\2\u00b6\u00b7\5]/\2\u00b7\20\3\2\2\2\u00b8\u00b9\5"+
		"Q)\2\u00b9\u00ba\5u;\2\u00ba\22\3\2\2\2\u00bb\u00bc\5y=\2\u00bc\u00bd"+
		"\5k\66\2\u00bd\u00be\5a\61\2\u00be\u00bf\5m\67\2\u00bf\u00c0\5k\66\2\u00c0"+
		"\24\3\2\2\2\u00c1\u00c2\5Q)\2\u00c2\u00c3\5g\64\2\u00c3\u00c4\5g\64\2"+
		"\u00c4\26\3\2\2\2\u00c5\u00c6\5Y-\2\u00c6\u00c7\5\177@\2\u00c7\u00c8\5"+
		"U+\2\u00c8\u00c9\5Y-\2\u00c9\u00ca\5o8\2\u00ca\u00cb\5w<\2\u00cb\30\3"+
		"\2\2\2\u00cc\u00cd\5a\61\2\u00cd\u00ce\5u;\2\u00ce\32\3\2\2\2\u00cf\u00d0"+
		"\5k\66\2\u00d0\u00d1\5m\67\2\u00d1\u00d2\5w<\2\u00d2\34\3\2\2\2\u00d3"+
		"\u00d4\5Q)\2\u00d4\u00d5\5k\66\2\u00d5\u00d6\5W,\2\u00d6\36\3\2\2\2\u00d7"+
		"\u00d8\5m\67\2\u00d8\u00d9\5s:\2\u00d9 \3\2\2\2\u00da\u00db\5k\66\2\u00db"+
		"\u00dc\5y=\2\u00dc\u00dd\5g\64\2\u00dd\u00de\5g\64\2\u00de\"\3\2\2\2\u00df"+
		"\u00e0\5U+\2\u00e0\u00e1\5m\67\2\u00e1\u00e2\5y=\2\u00e2\u00e3\5k\66\2"+
		"\u00e3\u00e4\5w<\2\u00e4$\3\2\2\2\u00e5\u00e6\5u;\2\u00e6\u00e7\5y=\2"+
		"\u00e7\u00e8\5i\65\2\u00e8&\3\2\2\2\u00e9\u00ea\5Q)\2\u00ea\u00eb\5{>"+
		"\2\u00eb\u00ec\5]/\2\u00ec(\3\2\2\2\u00ed\u00ee\5i\65\2\u00ee\u00ef\5"+
		"a\61\2\u00ef\u00f0\5k\66\2\u00f0*\3\2\2\2\u00f1\u00f2\5i\65\2\u00f2\u00f3"+
		"\5Q)\2\u00f3\u00f4\5\177@\2\u00f4,\3\2\2\2\u00f5\u00f6\7?\2\2\u00f6.\3"+
		"\2\2\2\u00f7\u00f8\7#\2\2\u00f8\u00f9\7?\2\2\u00f9\60\3\2\2\2\u00fa\u00fb"+
		"\7>\2\2\u00fb\u00fc\7?\2\2\u00fc\62\3\2\2\2\u00fd\u00fe\7@\2\2\u00fe\u00ff"+
		"\7?\2\2\u00ff\64\3\2\2\2\u0100\u0101\7>\2\2\u0101\66\3\2\2\2\u0102\u0103"+
		"\7@\2\2\u01038\3\2\2\2\u0104\u0105\7-\2\2\u0105:\3\2\2\2\u0106\u0107\7"+
		"/\2\2\u0107<\3\2\2\2\u0108\u0109\7,\2\2\u0109>\3\2\2\2\u010a\u010b\7\60"+
		"\2\2\u010b@\3\2\2\2\u010c\u010d\7.\2\2\u010dB\3\2\2\2\u010e\u010f\7*\2"+
		"\2\u010fD\3\2\2\2\u0110\u0111\7+\2\2\u0111F\3\2\2\2\u0112\u0113\7=\2\2"+
		"\u0113H\3\2\2\2\u0114\u0118\t\2\2\2\u0115\u0117\t\3\2\2\u0116\u0115\3"+
		"\2\2\2\u0117\u011a\3\2\2\2\u0118\u0116\3\2\2\2\u0118\u0119\3\2\2\2\u0119"+
		"J\3\2\2\2\u011a\u0118\3\2\2\2\u011b\u0121\7$\2\2\u011c\u0120\n\4\2\2\u011d"+
		"\u011e\7^\2\2\u011e\u0120\13\2\2\2\u011f\u011c\3\2\2\2\u011f\u011d\3\2"+
		"\2\2\u0120\u0123\3\2\2\2\u0121\u011f\3\2\2\2\u0121\u0122\3\2\2\2\u0122"+
		"\u0124\3\2\2\2\u0123\u0121\3\2\2\2\u0124\u0125\7$\2\2\u0125L\3\2\2\2\u0126"+
		"\u0128\5\u0085C\2\u0127\u0126\3\2\2\2\u0128\u0129\3\2\2\2\u0129\u0127"+
		"\3\2\2\2\u0129\u012a\3\2\2\2\u012a\u0131\3\2\2\2\u012b\u012d\7\60\2\2"+
		"\u012c\u012e\5\u0085C\2\u012d\u012c\3\2\2\2\u012e\u012f\3\2\2\2\u012f"+
		"\u012d\3\2\2\2\u012f\u0130\3\2\2\2\u0130\u0132\3\2\2\2\u0131\u012b\3\2"+
		"\2\2\u0131\u0132\3\2\2\2\u0132N\3\2\2\2\u0133\u0139\7)\2\2\u0134\u0138"+
		"\n\5\2\2\u0135\u0136\7^\2\2\u0136\u0138\13\2\2\2\u0137\u0134\3\2\2\2\u0137"+
		"\u0135\3\2\2\2\u0138\u013b\3\2\2\2\u0139\u0137\3\2\2\2\u0139\u013a\3\2"+
		"\2\2\u013a\u013c\3\2\2\2\u013b\u0139\3\2\2\2\u013c\u013d\7)\2\2\u013d"+
		"P\3\2\2\2\u013e\u013f\t\6\2\2\u013fR\3\2\2\2\u0140\u0141\t\7\2\2\u0141"+
		"T\3\2\2\2\u0142\u0143\t\b\2\2\u0143V\3\2\2\2\u0144\u0145\t\t\2\2\u0145"+
		"X\3\2\2\2\u0146\u0147\t\n\2\2\u0147Z\3\2\2\2\u0148\u0149\t\13\2\2\u0149"+
		"\\\3\2\2\2\u014a\u014b\t\f\2\2\u014b^\3\2\2\2\u014c\u014d\t\r\2\2\u014d"+
		"`\3\2\2\2\u014e\u014f\t\16\2\2\u014fb\3\2\2\2\u0150\u0151\t\17\2\2\u0151"+
		"d\3\2\2\2\u0152\u0153\t\20\2\2\u0153f\3\2\2\2\u0154\u0155\t\21\2\2\u0155"+
		"h\3\2\2\2\u0156\u0157\t\22\2\2\u0157j\3\2\2\2\u0158\u0159\t\23\2\2\u0159"+
		"l\3\2\2\2\u015a\u015b\t\24\2\2\u015bn\3\2\2\2\u015c\u015d\t\25\2\2\u015d"+
		"p\3\2\2\2\u015e\u015f\t\26\2\2\u015fr\3\2\2\2\u0160\u0161\t\27\2\2\u0161"+
		"t\3\2\2\2\u0162\u0163\t\30\2\2\u0163v\3\2\2\2\u0164\u0165\t\31\2\2\u0165"+
		"x\3\2\2\2\u0166\u0167\t\32\2\2\u0167z\3\2\2\2\u0168\u0169\t\33\2\2\u0169"+
		"|\3\2\2\2\u016a\u016b\t\34\2\2\u016b~\3\2\2\2\u016c\u016d\t\35\2\2\u016d"+
		"\u0080\3\2\2\2\u016e\u016f\t\36\2\2\u016f\u0082\3\2\2\2\u0170\u0171\t"+
		"\37\2\2\u0171\u0084\3\2\2\2\u0172\u0173\t \2\2\u0173\u0086\3\2\2\2\u0174"+
		"\u0176\t!\2\2\u0175\u0174\3\2\2\2\u0176\u0177\3\2\2\2\u0177\u0175\3\2"+
		"\2\2\u0177\u0178\3\2\2\2\u0178\u0179\3\2\2\2\u0179\u017a\bD\2\2\u017a"+
		"\u0088\3\2\2\2\u017b\u017c\7/\2\2\u017c\u017d\7/\2\2\u017d\u0181\3\2\2"+
		"\2\u017e\u0180\n\"\2\2\u017f\u017e\3\2\2\2\u0180\u0183\3\2\2\2\u0181\u017f"+
		"\3\2\2\2\u0181\u0182\3\2\2\2\u0182\u0184\3\2\2\2\u0183\u0181\3\2\2\2\u0184"+
		"\u0185\bE\2\2\u0185\u008a\3\2\2\2\u0186\u0187\7\61\2\2\u0187\u0188\7,"+
		"\2\2\u0188\u018c\3\2\2\2\u0189\u018b\13\2\2\2\u018a\u0189\3\2\2\2\u018b"+
		"\u018e\3\2\2\2\u018c\u018d\3\2\2\2\u018c\u018a\3\2\2\2\u018d\u018f\3\2"+
		"\2\2\u018e\u018c\3\2\2\2\u018f\u0190\7,\2\2\u0190\u0191\7\61\2\2\u0191"+
		"\u0192\3\2\2\2\u0192\u0193\bF\2\2\u0193\u008c\3\2\2\2\16\2\u0118\u011f"+
		"\u0121\u0129\u012f\u0131\u0137\u0139\u0177\u0181\u018c\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}