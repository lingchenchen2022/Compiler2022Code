lexer grammar SysYLexer;

CONST:'const';
INT:'int';
VOID:'void';
IF:'if';
ELSE:'else';
WHILE:'while';
BREAK:'break';
CONTINUE:'continue';
RETURN:'return';
PLUS:'+';
MINUS:'-';
MUL:'*';
DIV:'/';
MOD:'%';
ASSIGN:'=';
EQ:'==';
NEQ:'!=';
LT:'<';
GT:'>';
LE:'<=';
GE:'>=';
NOT:'!';
AND:'&&';
OR:'||';
L_PAREN:'(';
R_PAREN:')';
L_BRACE:'{';
R_BRACE:'}';
L_BRACKT:'[';
R_BRACKT:']';
COMMA:',';
SEMICOLON:';';


IDENT : (LETTER |'_') (LETTER | DIGIT | '_')*  ;
INTEGR_CONST:('0'[0-7]+|'0x'[0-9a-fA-F]+|'0X'[0-9a-fA-F]+)|('0'|[1-9][0-9]*);

WS : [ \t\r\n]+ -> skip ;
LINE_COMMENT:'//' .*? '\n' ->skip;
MULTILINE_COMMENT:'/*' .*? '*/'->skip;

fragment LETTER : [a-zA-Z]  ;
fragment DIGIT : [0-9] ;