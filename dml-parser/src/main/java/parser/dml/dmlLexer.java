// Generated from parser/dml/dml.g4 by ANTLR 4.5
package parser.dml;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class dmlLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		INSERT=1, INTO=2, VALUES=3, NULL_T=4, TRUE=5, FALSE=6, COMMA=7, LPAREN=8, 
		RPAREN=9, SEMICOLON=10, IDENT=11, QUOTED_IDENT=12, NUMBER=13, STRING=14, 
		WS=15, LINE_COMMENT=16, BLOCK_COMMENT=17;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"INSERT", "INTO", "VALUES", "NULL_T", "TRUE", "FALSE", "COMMA", "LPAREN", 
		"RPAREN", "SEMICOLON", "IDENT", "QUOTED_IDENT", "NUMBER", "STRING", "A", 
		"B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", 
		"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "DIGIT", "WS", 
		"LINE_COMMENT", "BLOCK_COMMENT"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, null, "','", "'('", "')'", "';'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "INSERT", "INTO", "VALUES", "NULL_T", "TRUE", "FALSE", "COMMA", 
		"LPAREN", "RPAREN", "SEMICOLON", "IDENT", "QUOTED_IDENT", "NUMBER", "STRING", 
		"WS", "LINE_COMMENT", "BLOCK_COMMENT"
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


	public dmlLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "dml.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\23\u0114\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\7\f\u0089\n\f"+
		"\f\f\16\f\u008c\13\f\3\r\3\r\3\r\3\r\7\r\u0092\n\r\f\r\16\r\u0095\13\r"+
		"\3\r\3\r\3\r\3\r\3\r\7\r\u009c\n\r\f\r\16\r\u009f\13\r\3\r\5\r\u00a2\n"+
		"\r\3\16\5\16\u00a5\n\16\3\16\6\16\u00a8\n\16\r\16\16\16\u00a9\3\16\3\16"+
		"\6\16\u00ae\n\16\r\16\16\16\u00af\5\16\u00b2\n\16\3\17\3\17\3\17\3\17"+
		"\7\17\u00b8\n\17\f\17\16\17\u00bb\13\17\3\17\3\17\3\20\3\20\3\21\3\21"+
		"\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30"+
		"\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37"+
		"\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3"+
		"*\3+\6+\u00f6\n+\r+\16+\u00f7\3+\3+\3,\3,\3,\3,\7,\u0100\n,\f,\16,\u0103"+
		"\13,\3,\3,\3-\3-\3-\3-\7-\u010b\n-\f-\16-\u010e\13-\3-\3-\3-\3-\3-\3\u010c"+
		"\2.\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35"+
		"\20\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63\2\65\2\67\29\2;\2=\2?\2A\2C"+
		"\2E\2G\2I\2K\2M\2O\2Q\2S\2U\21W\22Y\23\3\2$\5\2C\\aac|\6\2\62;C\\aac|"+
		"\4\2$$^^\4\2^^bb\4\2))^^\4\2CCcc\4\2DDdd\4\2EEee\4\2FFff\4\2GGgg\4\2H"+
		"Hhh\4\2IIii\4\2JJjj\4\2KKkk\4\2LLll\4\2MMmm\4\2NNnn\4\2OOoo\4\2PPpp\4"+
		"\2QQqq\4\2RRrr\4\2SSss\4\2TTtt\4\2UUuu\4\2VVvv\4\2WWww\4\2XXxx\4\2YYy"+
		"y\4\2ZZzz\4\2[[{{\4\2\\\\||\3\2\62;\5\2\13\f\17\17\"\"\4\2\f\f\17\17\u0107"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2"+
		"\2\3[\3\2\2\2\5b\3\2\2\2\7g\3\2\2\2\tn\3\2\2\2\13s\3\2\2\2\rx\3\2\2\2"+
		"\17~\3\2\2\2\21\u0080\3\2\2\2\23\u0082\3\2\2\2\25\u0084\3\2\2\2\27\u0086"+
		"\3\2\2\2\31\u00a1\3\2\2\2\33\u00a4\3\2\2\2\35\u00b3\3\2\2\2\37\u00be\3"+
		"\2\2\2!\u00c0\3\2\2\2#\u00c2\3\2\2\2%\u00c4\3\2\2\2\'\u00c6\3\2\2\2)\u00c8"+
		"\3\2\2\2+\u00ca\3\2\2\2-\u00cc\3\2\2\2/\u00ce\3\2\2\2\61\u00d0\3\2\2\2"+
		"\63\u00d2\3\2\2\2\65\u00d4\3\2\2\2\67\u00d6\3\2\2\29\u00d8\3\2\2\2;\u00da"+
		"\3\2\2\2=\u00dc\3\2\2\2?\u00de\3\2\2\2A\u00e0\3\2\2\2C\u00e2\3\2\2\2E"+
		"\u00e4\3\2\2\2G\u00e6\3\2\2\2I\u00e8\3\2\2\2K\u00ea\3\2\2\2M\u00ec\3\2"+
		"\2\2O\u00ee\3\2\2\2Q\u00f0\3\2\2\2S\u00f2\3\2\2\2U\u00f5\3\2\2\2W\u00fb"+
		"\3\2\2\2Y\u0106\3\2\2\2[\\\5/\30\2\\]\59\35\2]^\5C\"\2^_\5\'\24\2_`\5"+
		"A!\2`a\5E#\2a\4\3\2\2\2bc\5/\30\2cd\59\35\2de\5E#\2ef\5;\36\2f\6\3\2\2"+
		"\2gh\5I%\2hi\5\37\20\2ij\5\65\33\2jk\5G$\2kl\5\'\24\2lm\5C\"\2m\b\3\2"+
		"\2\2no\59\35\2op\5G$\2pq\5\65\33\2qr\5\65\33\2r\n\3\2\2\2st\5E#\2tu\5"+
		"A!\2uv\5G$\2vw\5\'\24\2w\f\3\2\2\2xy\5)\25\2yz\5\37\20\2z{\5\65\33\2{"+
		"|\5C\"\2|}\5\'\24\2}\16\3\2\2\2~\177\7.\2\2\177\20\3\2\2\2\u0080\u0081"+
		"\7*\2\2\u0081\22\3\2\2\2\u0082\u0083\7+\2\2\u0083\24\3\2\2\2\u0084\u0085"+
		"\7=\2\2\u0085\26\3\2\2\2\u0086\u008a\t\2\2\2\u0087\u0089\t\3\2\2\u0088"+
		"\u0087\3\2\2\2\u0089\u008c\3\2\2\2\u008a\u0088\3\2\2\2\u008a\u008b\3\2"+
		"\2\2\u008b\30\3\2\2\2\u008c\u008a\3\2\2\2\u008d\u0093\7$\2\2\u008e\u0092"+
		"\n\4\2\2\u008f\u0090\7^\2\2\u0090\u0092\13\2\2\2\u0091\u008e\3\2\2\2\u0091"+
		"\u008f\3\2\2\2\u0092\u0095\3\2\2\2\u0093\u0091\3\2\2\2\u0093\u0094\3\2"+
		"\2\2\u0094\u0096\3\2\2\2\u0095\u0093\3\2\2\2\u0096\u00a2\7$\2\2\u0097"+
		"\u009d\7b\2\2\u0098\u009c\n\5\2\2\u0099\u009a\7^\2\2\u009a\u009c\13\2"+
		"\2\2\u009b\u0098\3\2\2\2\u009b\u0099\3\2\2\2\u009c\u009f\3\2\2\2\u009d"+
		"\u009b\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u00a0\3\2\2\2\u009f\u009d\3\2"+
		"\2\2\u00a0\u00a2\7b\2\2\u00a1\u008d\3\2\2\2\u00a1\u0097\3\2\2\2\u00a2"+
		"\32\3\2\2\2\u00a3\u00a5\7/\2\2\u00a4\u00a3\3\2\2\2\u00a4\u00a5\3\2\2\2"+
		"\u00a5\u00a7\3\2\2\2\u00a6\u00a8\5S*\2\u00a7\u00a6\3\2\2\2\u00a8\u00a9"+
		"\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00b1\3\2\2\2\u00ab"+
		"\u00ad\7\60\2\2\u00ac\u00ae\5S*\2\u00ad\u00ac\3\2\2\2\u00ae\u00af\3\2"+
		"\2\2\u00af\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00b2\3\2\2\2\u00b1"+
		"\u00ab\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\34\3\2\2\2\u00b3\u00b9\7)\2\2"+
		"\u00b4\u00b8\n\6\2\2\u00b5\u00b6\7^\2\2\u00b6\u00b8\13\2\2\2\u00b7\u00b4"+
		"\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b8\u00bb\3\2\2\2\u00b9\u00b7\3\2\2\2\u00b9"+
		"\u00ba\3\2\2\2\u00ba\u00bc\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bc\u00bd\7)"+
		"\2\2\u00bd\36\3\2\2\2\u00be\u00bf\t\7\2\2\u00bf \3\2\2\2\u00c0\u00c1\t"+
		"\b\2\2\u00c1\"\3\2\2\2\u00c2\u00c3\t\t\2\2\u00c3$\3\2\2\2\u00c4\u00c5"+
		"\t\n\2\2\u00c5&\3\2\2\2\u00c6\u00c7\t\13\2\2\u00c7(\3\2\2\2\u00c8\u00c9"+
		"\t\f\2\2\u00c9*\3\2\2\2\u00ca\u00cb\t\r\2\2\u00cb,\3\2\2\2\u00cc\u00cd"+
		"\t\16\2\2\u00cd.\3\2\2\2\u00ce\u00cf\t\17\2\2\u00cf\60\3\2\2\2\u00d0\u00d1"+
		"\t\20\2\2\u00d1\62\3\2\2\2\u00d2\u00d3\t\21\2\2\u00d3\64\3\2\2\2\u00d4"+
		"\u00d5\t\22\2\2\u00d5\66\3\2\2\2\u00d6\u00d7\t\23\2\2\u00d78\3\2\2\2\u00d8"+
		"\u00d9\t\24\2\2\u00d9:\3\2\2\2\u00da\u00db\t\25\2\2\u00db<\3\2\2\2\u00dc"+
		"\u00dd\t\26\2\2\u00dd>\3\2\2\2\u00de\u00df\t\27\2\2\u00df@\3\2\2\2\u00e0"+
		"\u00e1\t\30\2\2\u00e1B\3\2\2\2\u00e2\u00e3\t\31\2\2\u00e3D\3\2\2\2\u00e4"+
		"\u00e5\t\32\2\2\u00e5F\3\2\2\2\u00e6\u00e7\t\33\2\2\u00e7H\3\2\2\2\u00e8"+
		"\u00e9\t\34\2\2\u00e9J\3\2\2\2\u00ea\u00eb\t\35\2\2\u00ebL\3\2\2\2\u00ec"+
		"\u00ed\t\36\2\2\u00edN\3\2\2\2\u00ee\u00ef\t\37\2\2\u00efP\3\2\2\2\u00f0"+
		"\u00f1\t \2\2\u00f1R\3\2\2\2\u00f2\u00f3\t!\2\2\u00f3T\3\2\2\2\u00f4\u00f6"+
		"\t\"\2\2\u00f5\u00f4\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f7"+
		"\u00f8\3\2\2\2\u00f8\u00f9\3\2\2\2\u00f9\u00fa\b+\2\2\u00faV\3\2\2\2\u00fb"+
		"\u00fc\7/\2\2\u00fc\u00fd\7/\2\2\u00fd\u0101\3\2\2\2\u00fe\u0100\n#\2"+
		"\2\u00ff\u00fe\3\2\2\2\u0100\u0103\3\2\2\2\u0101\u00ff\3\2\2\2\u0101\u0102"+
		"\3\2\2\2\u0102\u0104\3\2\2\2\u0103\u0101\3\2\2\2\u0104\u0105\b,\2\2\u0105"+
		"X\3\2\2\2\u0106\u0107\7\61\2\2\u0107\u0108\7,\2\2\u0108\u010c\3\2\2\2"+
		"\u0109\u010b\13\2\2\2\u010a\u0109\3\2\2\2\u010b\u010e\3\2\2\2\u010c\u010d"+
		"\3\2\2\2\u010c\u010a\3\2\2\2\u010d\u010f\3\2\2\2\u010e\u010c\3\2\2\2\u010f"+
		"\u0110\7,\2\2\u0110\u0111\7\61\2\2\u0111\u0112\3\2\2\2\u0112\u0113\b-"+
		"\2\2\u0113Z\3\2\2\2\22\2\u008a\u0091\u0093\u009b\u009d\u00a1\u00a4\u00a9"+
		"\u00af\u00b1\u00b7\u00b9\u00f7\u0101\u010c\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}