grammar ddl;

ddlStatement
  : (createTableStmt SEMICOLON?)+ EOF
  ;

createTableStmt
  : CREATE TABLE tableName LPAREN tableElementList RPAREN
  ;

tableElementList
  : columnDef (COMMA columnDef)*
  ;

columnDef
  : columnName dataType columnConstraint*
  ;

columnConstraint
  : PRIMARY KEY
  | NOT NULL_T
  ;

// data types
dataType
  : INT                                  #intType
  | INTEGER                              #integerType
  | SMALLINT                             #smallintType
  | BIGINT                               #bigintType
  | TINYINT                              #tinyintType
  | FLOAT                                #floatType
  | DOUBLE                               #doubleType
  | DECIMAL (LPAREN precision (COMMA scale)? RPAREN)?  #decimalType
  | NUMERIC (LPAREN precision (COMMA scale)? RPAREN)?  #numericType
  | CHAR (LPAREN length RPAREN)?         #charType
  | VARCHAR (LPAREN length RPAREN)?      #varcharType
  | TEXT                                 #textType
  | DATE                                 #dateType
  | TIME                                 #timeType
  | DATETIME                             #datetimeType
  | TIMESTAMP                            #timestampType
  | BOOLEAN                              #booleanType
  | BOOL                                 #boolType
  | BLOB                                 #blobType
  ;

precision
  : NUMBER
  ;

scale
  : NUMBER
  ;

length
  : NUMBER
  ;

// table def identifiers and literals
tableName
  : identifier
  ;

columnName
  : identifier
  ;

identifier
  : QUOTED_IDENT
  | IDENT
  ;

literal
  : NULL_T
  | NUMBER
  | STRING
  | TRUE
  | FALSE
  ;

// keywords
CREATE : C R E A T E ;
TABLE : T A B L E ;
PRIMARY : P R I M A R Y ;
KEY : K E Y ;
NOT : N O T ;
NULL_T : N U L L ;
TRUE : T R U E ;
FALSE : F A L S E ;

// Data type keywords
INT : I N T ;
INTEGER : I N T E G E R ;
SMALLINT : S M A L L I N T ;
BIGINT : B I G I N T ;
TINYINT : T I N Y I N T ;
FLOAT : F L O A T ;
DOUBLE : D O U B L E ;
DECIMAL : D E C I M A L ;
NUMERIC : N U M E R I C ;
CHAR : C H A R ;
VARCHAR : V A R C H A R ;
TEXT : T E X T ;
DATE : D A T E ;
TIME : T I M E ;
DATETIME : D A T E T I M E ;
TIMESTAMP : T I M E S T A M P ;
BOOLEAN : B O O L E A N ;
BOOL : B O O L ;
BLOB : B L O B ;

// Operators and punctuation
DOT : '.' ;
COMMA : ',' ;
LPAREN : '(' ;
RPAREN : ')' ;
SEMICOLON : ';' ;

// identifiers and literals
IDENT
  : [a-zA-Z_] [a-zA-Z_0-9]*
  ;

QUOTED_IDENT
  : '"' (~["\\] | '\\' .)* '"'
  | '`' (~[`\\] | '\\' .)* '`'
  ;

NUMBER
  : DIGIT+ ('.' DIGIT+)?
  ;

STRING
  : '\'' (~['\\] | '\\' .)* '\''
  ;

// case insensitive
fragment A : [aA] ; fragment B : [bB] ; fragment C : [cC] ;
fragment D : [dD] ; fragment E : [eE] ; fragment F : [fF] ;
fragment G : [gG] ; fragment H : [hH] ; fragment I : [iI] ;
fragment J : [jJ] ; fragment K : [kK] ; fragment L : [lL] ;
fragment M : [mM] ; fragment N : [nN] ; fragment O : [oO] ;
fragment P : [pP] ; fragment Q : [qQ] ; fragment R : [rR] ;
fragment S : [sS] ; fragment T : [tT] ; fragment U : [uU] ;
fragment V : [vV] ; fragment W : [wW] ; fragment X : [xX] ;
fragment Y : [yY] ; fragment Z : [zZ] ;
fragment DIGIT : [0-9] ;

// whitespace
WS : [ \t\r\n]+ -> skip ;

// comments
LINE_COMMENT
  : '--' ~[\r\n]* -> skip
  ;
