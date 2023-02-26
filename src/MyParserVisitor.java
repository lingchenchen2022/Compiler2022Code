import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import symtable.Symbol;

public class MyParserVisitor extends SysYParserBaseVisitor<Void> {
    private final SysYParser sysYParser;
    private Symbol symbol;
    String reName;

    private String[] highLight = {
            "CONST[orange]", "INT[orange]", "VOID[orange]", "IF[orange]", "ELSE[orange]", "WHILE[orange]", "BREAK[orange]", "CONTINUE[orange]", "RETURN[orange]",
            "PLUS[blue]", "MINUS[blue]", "MUL[blue]", "DIV[blue]", "MOD[blue]", "ASSIGN[blue]", "EQ[blue]", "NEQ[blue]", "LT[blue]", "GT[blue]",
            "LE[blue]", "GE[blue]", "NOT[blue]", "AND[blue]", "OR[blue]", "L_PAREN", "R_PAREN", "L_BRACE", "R_BRACE",
            "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON", "IDENT[red]", "INTEGR_CONST[green]",
            "WS", "LINE_COMMENT", "MULTILINE_COMMENT", "LETTER", "DIGIT"
    };

    public MyParserVisitor(SysYParser sysYParser,Symbol symbol,String reName) {
        this.sysYParser = sysYParser;
        this.symbol=symbol;
        this.reName=reName;
    }


    @Override
    public Void visitChildren(RuleNode node) {
        for (int i = 0; i < node.getRuleContext().depth() - 1; i++) {
            System.err.print("  ");
        }
        String ruleName = sysYParser.getRuleNames()[node.getRuleContext().getRuleIndex()];
        ruleName = ruleName.substring(0, 1).toUpperCase() + ruleName.substring(1);
        System.err.println(ruleName);
        return super.visitChildren(node);
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        int index = node.getSymbol().getType() - 1;
        if ((index >= 0 && index <= 23) || index == 32 || index == 33) {
            for (int i = 0; i < ((RuleNode) node.getParent()).getRuleContext().depth(); i++) {
                System.err.print("  ");
            }
            if (index == 33) {
                String text = node.getText();
                int num;
                if (text.matches("0x[0-9a-fA-F]+") || text.matches("0X[0-9a-fA-F]+")) {
                    num = Integer.parseInt(text.substring(2), 16);
                } else if (text.matches("0[0-7]+")) {
                    num = Integer.parseInt(text.substring(1), 8);
                } else {
                    num = Integer.parseInt(text);
                }
                System.err.println(num + " " + highLight[index]);
            } else {
                if(symbol!=null&&symbol.isInLineColumn(node.getSymbol().getLine(),node.getSymbol().getCharPositionInLine())){
                    System.err.println(reName + " " + highLight[index]);
                }else {
                    System.err.println(node.getText() + " " + highLight[index]);
                }
            }
        }
        return super.visitTerminal(node);
    }

}