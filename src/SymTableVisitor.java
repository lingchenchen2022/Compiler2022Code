import org.antlr.v4.runtime.tree.TerminalNode;
import symtable.*;

import java.util.ArrayList;
import java.util.Map;

public class SymTableVisitor extends SysYParserBaseVisitor<Void>{
    private GlobalScope globalScope;
    private Scope currentScope;
    private boolean isFalse;

    private ArrayList<Scope> allScope;

    public SymTableVisitor(){
        this.isFalse=false;
        allScope=new ArrayList<>();
    }

    public boolean getIsFalse() {
        return isFalse;
    }

    public Symbol getReNameSymbol(int lineNo,int column){    //遍历整个符号表根据保存的信息得到哪一个变量需要被重命名
        for (Scope scope:allScope){
            Map<String, Symbol> symbolMap=scope.getSymbols();
            for (String key:symbolMap.keySet()){
                Symbol symbol=symbolMap.get(key);
                if(symbol.isInLineColumn(lineNo,column)){
                    return symbol;
                }
            }
        }
        return null;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        int index=node.getSymbol().getType()-1;
        if(index==32){      //IDENT
            int lineNo=node.getSymbol().getLine();
            int column=node.getSymbol().getCharPositionInLine();
            Symbol symbol=currentScope.resolve(node.getSymbol().getText());
            if(symbol!=null){
                symbol.setLineColumn(lineNo,column);
            }
        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        globalScope=new GlobalScope(null);
        allScope.add(globalScope);
        currentScope=globalScope;
        Void result=super.visitProgram(ctx);
        currentScope=currentScope.getEnclosingScope();
        return result;
    }

    private boolean errorFourCheck(String name){
        return globalScope.resolve(name) != null;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName=ctx.IDENT().getText();
        boolean isValid=true;
        if(errorFourCheck(funcName)){
            isFalse=true;
            isValid=false;
            System.err.println("Error type 4 at Line "
                    +ctx.start.getLine()+": Redefined function: "+ctx.getText());
        }
        String funcType=ctx.funcType().getText();   //返回类型
        FunctionType functionType=new FunctionType(new BaseType(funcType));
        FunctionSymbol functionSymbol=new FunctionSymbol(funcName,currentScope,functionType,isValid);
        if(isValid) {
            currentScope.define(functionSymbol);
        }
        allScope.add(functionSymbol);
        currentScope=functionSymbol;
        Void result=super.visitFuncDef(ctx);
        currentScope=currentScope.getEnclosingScope();
        return result;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        currentScope= new LocalScope(currentScope);
        allScope.add(currentScope);
        Void result=super.visitBlock(ctx);
        currentScope=currentScope.getEnclosingScope();
        return result;
    }



    private boolean errorThreeCheck(String name){
        if(currentScope.equals(globalScope)){
            if(currentScope.resolve(name)!=null){
                return true;
            }
        }
        if(currentScope instanceof LocalScope){
            if(currentScope.currentResolve(name)!=null){
                return true;
            }
            Scope funcScope=((LocalScope)currentScope).findFuncScope();
            return funcScope.currentResolve(name) != null;
        }
        return false;
    }


    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        if(!isValidScope()){
            return super.visitVarDecl(ctx);
        }
        String baseTypeName=ctx.bType().getText();
        for(SysYParser.VarDefContext varDefContext:ctx.varDef()){
            String name=varDefContext.IDENT().getText();
            if(errorThreeCheck(name)){
                isFalse=true;
                System.err.println("Error type 3 at Line "
                        +ctx.start.getLine()+": Redefined variable: "+ctx.getText());
                continue;
            }
            if(varDefContext.L_BRACKT().size()==0){
                BaseType baseType =new BaseType(baseTypeName);
                BaseSymbol baseSymbol=new BaseSymbol(name, baseType);
                currentScope.define(baseSymbol);
                if(varDefContext.initVal()!=null){     //定义时赋值
                    int expTypeNum=getExpTypeNumber(varDefContext.initVal().exp());
                    if(expTypeNum!=-1&&baseType.getNumber()!=expTypeNum){
                        isFalse=true;
                        System.err.println("Error type 5 at Line "
                                +varDefContext.start.getLine()+": "+varDefContext.getText()+": Mismatched type for assignment");
                    }
                }
            }else {
                int arrDimension = varDefContext.constExp().size();  //数组维度
                if(varDefContext.constExp(0)==null){
                    System.err.println(varDefContext.start.getLine());
                }
                int count=Integer.parseInt(varDefContext.constExp(0).getText());
                ArrayType arrayType = new ArrayType(count,arrDimension);
                ArrayType lastArrayType=arrayType;
                for(int i=1;i<arrDimension;i++){
                    count=Integer.parseInt(varDefContext.constExp(i).getText());
                    ArrayType subType=new ArrayType(count,arrDimension-i);
                    lastArrayType.setSubType(subType);
                    lastArrayType=subType;
                }
                lastArrayType.setSubType(new BaseType(baseTypeName));
                ArraySymbol arraySymbol=new ArraySymbol(name,arrayType);
                currentScope.define(arraySymbol);
            }
        }
        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if(!isValidScope()){
            return super.visitConstDecl(ctx);
        }
        String baseTypeName=ctx.bType().getText();
        for(SysYParser.ConstDefContext constDefContext:ctx.constDef()){
            String name=constDefContext.IDENT().getText();
            if(errorThreeCheck(name)){
                isFalse=true;
                System.err.println("Error type 3 at Line "
                        +ctx.start.getLine()+": Redefined variable: "+ctx.getText());
                continue;
            }
            if(constDefContext.L_BRACKT().size()==0){
                BaseType baseType =new BaseType(baseTypeName);
                BaseSymbol baseSymbol=new BaseSymbol(name, baseType);
                currentScope.define(baseSymbol);
                int expTypeNum=getExpTypeNumber(constDefContext.constInitVal().constExp().exp());
                if(expTypeNum!=-1&&baseType.getNumber()!=expTypeNum){
                    isFalse=true;
                    System.err.println("Error type 5 at Line "
                            +constDefContext.start.getLine()+": "+constDefContext.getText()+": Mismatched type for assignment");
                }
            }else {
                int arrDimension = constDefContext.constExp().size();  //数组维度
                int count=Integer.parseInt(constDefContext.constExp(0).getText());
                ArrayType arrayType = new ArrayType(count,arrDimension);
                ArrayType lastArrayType=arrayType;
                for(int i=1;i<arrDimension;i++){
                    count=Integer.parseInt(constDefContext.constExp(i).getText());
                    ArrayType subType=new ArrayType(count,arrDimension-i);
                    lastArrayType.setSubType(subType);
                    lastArrayType=subType;
                }
                lastArrayType.setSubType(new BaseType(baseTypeName));
                ArraySymbol arraySymbol=new ArraySymbol(name,arrayType);
                currentScope.define(arraySymbol);
            }
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        //函数定义时的形参，currentScope isInstanceOf FunctionSymbol
        if(!isValidScope()){
            return super.visitFuncFParam(ctx);
        }
        String name=ctx.IDENT().getText();
        if(currentScope.currentResolve(name)!=null){
            isFalse=true;
            System.err.println("Error type 3 at Line "
                    +ctx.start.getLine()+": Redefined variable: "+ctx.getText());
            return super.visitFuncFParam(ctx);
        }
        String typeName=ctx.bType().getText();
        if(ctx.L_BRACKT().size()==0){    //不是数组类型
            BaseType baseType=new BaseType(typeName);
            BaseSymbol baseSymbol=new BaseSymbol(name,baseType);
            currentScope.define(baseSymbol);
            ((FunctionSymbol)currentScope).addParamType(baseType);
        }else {
            int arrDimension=ctx.L_BRACKT().size();
            ArrayType arrayType=new ArrayType(null,arrDimension);
            ArrayType lastArrayType=arrayType;
            for(int i=1;i<arrDimension;i++){
                int count=Integer.parseInt(ctx.exp(i-1).getText());
                ArrayType subType=new ArrayType(count,arrDimension-i);
                lastArrayType.setSubType(subType);
                lastArrayType=subType;
            }
            lastArrayType.setSubType(new BaseType(typeName));
            ArraySymbol arraySymbol=new ArraySymbol(name,arrayType);
            currentScope.define(arraySymbol);
            ((FunctionSymbol)currentScope).addParamType(arrayType);
        }
        return super.visitFuncFParam(ctx);
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        if(!isValidScope()){
            return super.visitLVal(ctx);
        }
        Symbol symbol=currentScope.resolve(ctx.IDENT().getText());
        if(symbol==null){
            isFalse=true;
            System.err.println("Error type 1 at Line "
                    +ctx.start.getLine()+": Undefined variable: "+ctx.getText());
            return super.visitLVal(ctx);
        }
        if((symbol.getType().getNumber()==1||symbol.getType().getNumber()==3)&&ctx.L_BRACKT().size()!=0){
            isFalse=true;
            System.err.println("Error type 9 at Line "
                    +ctx.start.getLine()+": Not an array: "+ctx.IDENT().getText());
            return super.visitLVal(ctx);
        }
        if(symbol.getType().getNumber()==2){
            if(((ArrayType)symbol.getType()).getDimension()<ctx.L_BRACKT().size()){
                isFalse=true;
                System.err.println("Error type 9 at Line "
                        +ctx.start.getLine()+": Not an array: "+ctx.IDENT().getText());
                return super.visitLVal(ctx);
            }
        }
        return super.visitLVal(ctx);
    }

    @Override
    public Void visitFuncExp(SysYParser.FuncExpContext ctx) {
        if(!isValidScope()){
            return super.visitFuncExp(ctx);
        }
        Symbol symbol=currentScope.resolve(ctx.IDENT().getText());
        if(symbol==null){
            isFalse=true;
            System.err.println("Error type 2 at Line "
                    +ctx.start.getLine()+": Undefined function: "+ctx.getText());
            return super.visitFuncExp(ctx);
        }
        if(!(symbol instanceof FunctionSymbol)){
            isFalse=true;
            System.err.println("Error type 10 at Line "
                    +ctx.start.getLine()+": Not a function: "+ctx.IDENT().getText());
            return super.visitFuncExp(ctx);
        }
        ArrayList<Type> paramsType=((FunctionSymbol)symbol).getParamsType();
        if(ctx.funcRParams()==null){
            if(paramsType.size()!=0){
                isFalse=true;
                System.err.println("Error type 8 at Line "
                        +ctx.start.getLine()+": Function is not applicable for arguments.");
                return super.visitFuncExp(ctx);
            }
        }else {
            if(paramsType.size()!=ctx.funcRParams().param().size()){
                isFalse=true;
                System.err.println("Error type 8 at Line "
                        +ctx.start.getLine()+": Function is not applicable for arguments.");
                return super.visitFuncExp(ctx);
            }
            for (int i=0;i<ctx.funcRParams().param().size();i++){
                SysYParser.ParamContext paramContext=ctx.funcRParams().param(i);
                int typeNum=getExpTypeNumber(paramContext.exp());
                if(typeNum!=-1&&paramsType.get(i).getNumber()!=typeNum){
                    isFalse=true;
                    System.err.println("Error type 8 at Line "
                            +ctx.start.getLine()+": Function is not applicable for arguments.");
                    return super.visitFuncExp(ctx);
                }
            }
        }
        return super.visitFuncExp(ctx);
    }

    @Override
    public Void visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        if(!isValidScope()){
            return super.visitAssignStmt(ctx);
        }
        String leftName=ctx.lVal().IDENT().getText();
        if(currentScope.resolve(leftName)!=null) {
            Type leftType = currentScope.resolve(leftName).getType();
            if(leftType instanceof FunctionType){
                isFalse=true;
                System.err.println("Error type 11 at Line "+ctx.start.getLine()+": The left-hand side of an assignment must be a variable.");
                return super.visitAssignStmt(ctx);
            }
            int rightTypeNum=getExpTypeNumber(ctx.exp());
//            if(rightTypeNum!=-1&&leftType.getNumber()!=rightTypeNum){
//                isFalse = true;
//                //TODO
//                System.err.println("Error type 5 at Line "
//                        + ctx.start.getLine() + ": " + ctx.getText() + ": Mismatched type for assignment");
//                return super.visitAssignStmt(ctx);
//            }
            if(leftType.getNumber()==1){
                if(rightTypeNum!=-1&&rightTypeNum!=1) {
                    isFalse = true;
                    System.err.println("Error type 5 at Line "
                            + ctx.start.getLine() + ": " + ctx.getText() + ": Mismatched type for assignment");
                    return super.visitAssignStmt(ctx);
                }
            }
            if (leftType.getNumber()==2&&rightTypeNum==1){
                int dimension=((ArrayType)leftType).getDimension()-ctx.lVal().L_BRACKT().size();
                if(dimension!=0){
                    isFalse = true;
                    System.err.println("Error type 5 at Line "
                            + ctx.start.getLine() + ": " + ctx.getText() + ": Mismatched type for assignment");
                    return super.visitAssignStmt(ctx);
                }
            }
            if (leftType.getNumber()==2&&rightTypeNum==3){
                isFalse = true;
                System.err.println("Error type 5 at Line "
                        + ctx.start.getLine() + ": " + ctx.getText() + ": Mismatched type for assignment");
                return super.visitAssignStmt(ctx);
            }
            if(leftType.getNumber()==2&&rightTypeNum==2){    //左右两边都为array
                int leftDimension = ((ArrayType) leftType).getDimension() - ctx.lVal().L_BRACKT().size();
                SysYParser.ExpContext expTemp=ctx.exp();
                while(expTemp instanceof SysYParser.ParenExpContext){
                    expTemp=((SysYParser.ParenExpContext) expTemp).exp();
                }
                String rightName=((SysYParser.LValExpContext)expTemp).lVal().IDENT().getText();
                Type rightType=currentScope.resolve(rightName).getType();
                int rightDimension = ((ArrayType) rightType).getDimension()
                        - ((SysYParser.LValExpContext) ctx.exp()).lVal().L_BRACKT().size();
                if (leftDimension != rightDimension) {
                    isFalse = true;
                    System.err.println("Error type 5 at Line "
                            + ctx.start.getLine() + ": " + ctx.getText() + ": Mismatched type for assignment");
                    return super.visitAssignStmt(ctx);
                }
            }
        }
        return super.visitAssignStmt(ctx);
    }


    @Override
    public Void visitPlusMinusExp(SysYParser.PlusMinusExpContext ctx) {
        if(!isValidScope()){
            return super.visitPlusMinusExp(ctx);
        }
        for(SysYParser.ExpContext expContext:ctx.exp()){
            if(getExpTypeNumber(expContext)!=-1&&getExpTypeNumber(expContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                        +": "+ctx.getText()+" mismatched for operands");
                break;
            }
        }
        return super.visitPlusMinusExp(ctx);
    }

    @Override
    public Void visitMulDivModExp(SysYParser.MulDivModExpContext ctx) {
        if(!isValidScope()){
            return super.visitMulDivModExp(ctx);
        }
        for(SysYParser.ExpContext expContext:ctx.exp()){
            if(getExpTypeNumber(expContext)!=-1&&getExpTypeNumber(expContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                        +": "+ctx.getText()+" mismatched for operands");
                break;
            }
        }
        return super.visitMulDivModExp(ctx);
    }

    @Override
    public Void visitUnaryOpExp(SysYParser.UnaryOpExpContext ctx) {
        if(!isValidScope()){
            return super.visitUnaryOpExp(ctx);
        }
        SysYParser.ExpContext expContext=ctx.exp();
        if(getExpTypeNumber(expContext)!=-1&&getExpTypeNumber(expContext)!=1){
            isFalse=true;
            System.err.println("Error type 6 at Line "+ctx.start.getLine()
                    +": "+ctx.getText()+" mismatched for operands");
        }
        return super.visitUnaryOpExp(ctx);
    }

    private int isCondNum(SysYParser.CondContext condContext){
        if(condContext instanceof SysYParser.ExpCondContext){
            return getExpTypeNumber(((SysYParser.ExpCondContext) condContext).exp());
        }
        else if(condContext instanceof SysYParser.LtCondContext){
            if(isCondNum(((SysYParser.LtCondContext) condContext).cond(0))==-1
                    || isCondNum(((SysYParser.LtCondContext) condContext).cond(1))==-1){
                return -1;
            }
            if(isCondNum(((SysYParser.LtCondContext) condContext).cond(0))==1
                    && isCondNum(((SysYParser.LtCondContext) condContext).cond(1))==1){
                return 1;
            }
        }
        else if(condContext instanceof SysYParser.EqCondContext){
            if (isCondNum(((SysYParser.EqCondContext) condContext).cond(0))==-1
                    || isCondNum(((SysYParser.EqCondContext) condContext).cond(1))==-1){
                return -1;
            }
            if (isCondNum(((SysYParser.EqCondContext) condContext).cond(0))==1
                    && isCondNum(((SysYParser.EqCondContext) condContext).cond(1))==1){
                return 1;
            }
        }
        else if(condContext instanceof SysYParser.AndCondContext){
            if(isCondNum(((SysYParser.AndCondContext) condContext).cond(0))==-1
                    || isCondNum(((SysYParser.AndCondContext) condContext).cond(1))==-1){
                return -1;
            }
            if(isCondNum(((SysYParser.AndCondContext) condContext).cond(0))==1
                    && isCondNum(((SysYParser.AndCondContext) condContext).cond(1))==1){
                return 1;
            }
        }
        else if(condContext instanceof SysYParser.OrCondContext){
            if(isCondNum(((SysYParser.OrCondContext) condContext).cond(0))==-1
                    || isCondNum(((SysYParser.OrCondContext) condContext).cond(1))==-1){
                return -1;
            }
            if(isCondNum(((SysYParser.OrCondContext) condContext).cond(0))==1
                    && isCondNum(((SysYParser.OrCondContext) condContext).cond(1))==1){
                return 1;
            }
        }
        return -1;
    }

    @Override
    public Void visitLtCond(SysYParser.LtCondContext ctx) {
        if(!isValidScope()){
            return super.visitLtCond(ctx);
        }
        for(SysYParser.CondContext condContext:ctx.cond()){
            if(isCondNum(condContext)!=-1&&isCondNum(condContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                    +": "+ctx.getText()+" mismatched for operands");
                break;
            }
        }
        return super.visitLtCond(ctx);
    }

    @Override
    public Void visitOrCond(SysYParser.OrCondContext ctx) {
        if(!isValidScope()){
            return super.visitOrCond(ctx);
        }
        for(SysYParser.CondContext condContext:ctx.cond()){
            if(isCondNum(condContext)!=-1&&isCondNum(condContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                        +": "+ctx.getText()+" mismatched for operands");

            }
        }
        return super.visitOrCond(ctx);
    }

    @Override
    public Void visitAndCond(SysYParser.AndCondContext ctx) {
        if(!isValidScope()){
            return super.visitAndCond(ctx);
        }
        for(SysYParser.CondContext condContext:ctx.cond()){
            if(isCondNum(condContext)!=-1&&isCondNum(condContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                        +": "+ctx.getText()+" mismatched for operands");
                break;
            }
        }
        return super.visitAndCond(ctx);
    }

    @Override
    public Void visitEqCond(SysYParser.EqCondContext ctx) {
        if(!isValidScope()){
            return super.visitEqCond(ctx);
        }
        for(SysYParser.CondContext condContext:ctx.cond()){
            if(isCondNum(condContext)!=-1&&isCondNum(condContext)!=1){
                isFalse=true;
                System.err.println("Error type 6 at Line "+ctx.start.getLine()
                        +": "+ctx.getText()+" mismatched for operands");
                break;
            }
        }
        return super.visitEqCond(ctx);
    }


    private int getExpTypeNumber(SysYParser.ExpContext exp){
        if(exp instanceof SysYParser.ParenExpContext){
            return getExpTypeNumber(((SysYParser.ParenExpContext) exp).exp());
        }
        else if(exp instanceof SysYParser.NumberExpContext){
            return 1;     //int
        }
        else if(exp instanceof SysYParser.LValExpContext){
            String name=((SysYParser.LValExpContext) exp).lVal().IDENT().getText();
            Symbol symbol=currentScope.resolve(name);
            if(symbol!=null){
                int typeNumber=symbol.getType().getNumber();
                if(typeNumber==1){  //int
                    return 1;
                }
                else if(typeNumber==2){     //array
                    int dimension=((ArrayType)symbol.getType()).getDimension()
                            -((SysYParser.LValExpContext) exp).lVal().L_BRACKT().size();
                    if(dimension==0){
                        return 1;
                    }else if(dimension>0){
                        return 2;
                    }
                }
                else if(typeNumber==3){
                    return 3;
                }
            }
            return -1;
        }
        else if(exp instanceof SysYParser.FuncExpContext){
            String name=((SysYParser.FuncExpContext) exp).IDENT().getText();
            Symbol symbol=globalScope.resolve(name);
            if(symbol!=null){
                if(symbol.getType() instanceof FunctionType){    //函数类型
                    return ((FunctionType)symbol.getType()).getRetType().getNumber();
                }
            }
            return -1;
        }
        else if(exp instanceof SysYParser.UnaryOpExpContext){
            if(getExpTypeNumber(((SysYParser.UnaryOpExpContext) exp).exp())==1){
                return 1;
            }
            return -1;
        }
        else if(exp instanceof SysYParser.MulDivModExpContext){
            for (SysYParser.ExpContext expTemp:((SysYParser.MulDivModExpContext) exp).exp()){
                if(getExpTypeNumber(expTemp)!=1){
                    return -1;
                }
            }
            return 1;
        }
        else if(exp instanceof SysYParser.PlusMinusExpContext){
            for (SysYParser.ExpContext expTemp:((SysYParser.PlusMinusExpContext) exp).exp()){
                if(getExpTypeNumber(expTemp)!=1){
                    return -1;
                }
            }
            return 1;
        }
        return -1;
    }

    private boolean isValidScope(){
        if(currentScope.equals(globalScope)){
            return true;
        }
        if (currentScope instanceof FunctionSymbol){
            return ((FunctionSymbol) currentScope).isValid();
        }
        FunctionSymbol functionSymbol=((LocalScope)currentScope).findFuncScope();
        return functionSymbol.isValid();
    }



    @Override
    public Void visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        if (!isValidScope()){
            return super.visitReturnStmt(ctx);
        }
        if(ctx.exp()!=null) {
            FunctionSymbol functionSymbol = ((LocalScope) currentScope).findFuncScope();
            Type retType = ((FunctionType) functionSymbol.getType()).getRetType();
            int expTypeNum = getExpTypeNumber(ctx.exp());
            if (expTypeNum != -1 && retType.getNumber() != expTypeNum) {
                isFalse = true;
                System.err.println("Error type 7 at Line " + ctx.start.getLine() + ": " + ctx.getText() + ": mismatched for return.");
            }
        }
        return super.visitReturnStmt(ctx);
    }
}