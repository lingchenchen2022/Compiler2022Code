import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main
{    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
//        int lineNo = Integer.parseInt(args[1]);
//        int column = Integer.parseInt(args[2]);
//        String name = args[3];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer=new SysYLexer(input);
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(new MyErrorListener());
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);
        sysYParser.removeErrorListeners();
        ParserErrorListener parserErrorListener=new ParserErrorListener();
        sysYParser.addErrorListener(parserErrorListener);
        ParseTree tree=sysYParser.program();
//        SymTableVisitor symtableVisitor =new SymTableVisitor();
//        symtableVisitor.visit(tree);
//        if(!(parserErrorListener.getIsError() || symtableVisitor.getIsFalse())) {
//            Symbol symbol=symtableVisitor.getReNameSymbol(lineNo,column);
//            MyParserVisitor myParserVisitor = new MyParserVisitor(sysYParser,symbol,name);
//            myParserVisitor.visit(tree);
//        }
        String outputPath=args[1];
        LLVMVisitor llvmVisitor=new LLVMVisitor(outputPath);
        llvmVisitor.visit(tree);
    }
}