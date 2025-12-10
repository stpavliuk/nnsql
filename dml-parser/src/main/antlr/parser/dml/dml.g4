grammar dml;

dmlStatement
  : (insertStmt SEMICOLON?)+ EOF
  ;
insertStmt
  : INSERT INTO tableName LPAREN columnList RPAREN VALUES valuesList
  ;
columnList
  : columnName (COMMA columnName)*
  ;
valuesList
  : LPAREN literalList RPAREN (COMMA LPAREN literalList RPAREN)*
  ;

// =========================

literalList
  : literal (COMMA literal)*
  ;
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



INSERT : I N S E R T ;
INTO : I N T O ;
VALUES : V A L U E S ;
NULL_T : N U L L ;
TRUE : T R U E ;
FALSE : F A L S E ;


COMMA : ',' ;
LPAREN : '(' ;
RPAREN : ')' ;
SEMICOLON : ';' ;


IDENT
  : [a-zA-Z_] [a-zA-Z_0-9]*
  ;

QUOTED_IDENT
  : '"' (~["\\] | '\\' .)* '"'
  | '`' (~[`\\] | '\\' .)* '`'
  ;

NUMBER
  : '-'? DIGIT+ ('.' DIGIT+)?
  ;

STRING
  : '\'' (~['\\] | '\\' .)* '\''
  ;

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

WS : [ \t\r\n]+ -> skip ;
LINE_COMMENT
  : '--' ~[\r\n]* -> skip
  ;
BLOCK_COMMENT
  : '/*' .*? '*/' -> skip
  ;
