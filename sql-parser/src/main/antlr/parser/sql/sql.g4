grammar sql;

// --------------------
// QUERY
// --------------------
query
  : selectStmt (setOp selectStmt)* EOF
  ;

setOp
  : UNION ALL?
  | EXCEPT ALL?
  ;

// -----------------------------------------
// SELECT / FROM / WHERE / GROUP BY / HAVING
// -----------------------------------------
selectStmt
  : selectClause
    fromClause?
    whereClause?
    groupByClause?
    havingClause?
  ;

selectClause: SELECT DISTINCT? selectList;
fromClause: FROM fromItem (COMMA fromItem)*;
whereClause: WHERE boolExpr;
groupByClause: GROUP BY groupByList;
havingClause: HAVING boolExpr;

selectList
  : selectItem (COMMA selectItem)*
  ;

selectItem
  : STAR | expr (AS? alias)?
  ;

fromItem
  : tableName (AS? alias)?            #FromTableItem
  | LPAREN query RPAREN (AS? alias)   #FromQueryItem
  ;

groupByList
  : expr (COMMA expr)*
  ;

// --------------------
// Boolean expressions
// --------------------
boolExpr
  : orExpr
  ;

orExpr
  : andExpr (OR andExpr)*
  ;

andExpr
  : notExpr (AND notExpr)*
  ;

notExpr
  : NOT notExpr
  | predicate
  ;

predicate
  : expr compOp expr
  | expr IS NOT? NULL_T
  | LPAREN boolExpr RPAREN
  ;

compOp
  : EQ
  | NEQ
  | LT
  | GT
  | LTE
  | GTE
  ;

// --------------------
// arithmetic expressions
// --------------------
expr
  : expr STAR expr                        #mulExpr
  | expr (PLUS | MINUS) expr              #addSubExpr
  | aggFunc LPAREN expr RPAREN            #aggCallExpr
  | LPAREN selectStmt RPAREN              #scalarSubqueryExpr
  | columnRef                             #columnExpr
  | literal                               #literalExpr
  | LPAREN expr RPAREN                    #parenExpr
  ;

aggFunc
  : COUNT | SUM | AVG | MIN | MAX
  ;

columnRef
  : (tableName DOT)? identifier
  ;

tableName
  : identifier
  ;

alias
  : identifier
  ;

literal
  : NULL_T
  | NUMBER
  | STRING
  ;

SELECT : S E L E C T ;
DISTINCT : D I S T I N C T ;
FROM : F R O M ;
WHERE : W H E R E ;
GROUP : G R O U P ;
BY : B Y ;
HAVING : H A V I N G ;
AS : A S ;
UNION : U N I O N ;
ALL : A L L ;
EXCEPT : E X C E P T ;
IS : I S ;
NOT : N O T ;
AND : A N D ;
OR : O R ;
NULL_T : N U L L ;

COUNT : C O U N T ;
SUM : S U M ;
AVG : A V G ;
MIN : M I N ;
MAX : M A X ;

// Operators
EQ : '=' ;
NEQ : '!=' ;
LTE : '<=' ;
GTE : '>=' ;
LT : '<' ;
GT : '>' ;

PLUS : '+' ;
MINUS : '-' ;
STAR : '*' ;

DOT : '.' ;
COMMA : ',' ;
LPAREN : '(' ;
RPAREN : ')' ;
SEMICOLON : ';' ;

identifier
  : QUOTED_IDENT
  | IDENT
  ;

IDENT
  : [a-zA-Z_] [a-zA-Z_0-9]*
  ;

QUOTED_IDENT
  : '"' (~["\\] | '\\' .)* '"'
  ;

NUMBER
  : DIGIT+ ('.' DIGIT+)?
  ;

STRING
  : '\'' (~['\\] | '\\' .)* '\''
  ;

// --------------------
// Case insensitive queries
// --------------------
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

// --------------------
// Whitespace & comments
// --------------------
WS : [ \t\r\n]+ -> skip ;

LINE_COMMENT
  : '--' ~[\r\n]* -> skip
  ;

BLOCK_COMMENT
  : '/*' .*? '*/' -> skip
  ;
