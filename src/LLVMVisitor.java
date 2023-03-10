import llvmsymtable.ArraySymbol;
import llvmsymtable.BaseSymbol;
import llvmsymtable.FunctionSymbol;
import llvmsymtable.GlobalScope;
import llvmsymtable.LocalScope;
import llvmsymtable.Scope;
import llvmsymtable.Symbol;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef;
import org.bytedeco.llvm.LLVM.LLVMBuilderRef;
import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.LLVMAddFunction;
import static org.bytedeco.llvm.global.LLVM.LLVMAddGlobal;
import static org.bytedeco.llvm.global.LLVM.LLVMAppendBasicBlock;
import static org.bytedeco.llvm.global.LLVM.LLVMArrayType;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildAdd;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildAlloca;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildAnd;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildBr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildCall;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildCondBr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildGEP;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildICmp;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildLoad;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildMul;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildNeg;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildOr;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildRet;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildRetVoid;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildSDiv;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildSRem;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildStore;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildSub;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildXor;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildZExt;
import static org.bytedeco.llvm.global.LLVM.LLVMConstArray;
import static org.bytedeco.llvm.global.LLVM.LLVMConstInt;
import static org.bytedeco.llvm.global.LLVM.LLVMConstIntGetSExtValue;
import static org.bytedeco.llvm.global.LLVM.LLVMCreateBuilder;
import static org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage;
import static org.bytedeco.llvm.global.LLVM.LLVMFunctionType;
import static org.bytedeco.llvm.global.LLVM.LLVMGetGlobalPassRegistry;
import static org.bytedeco.llvm.global.LLVM.LLVMGetParam;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeCore;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeAsmParser;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeAsmPrinter;
import static org.bytedeco.llvm.global.LLVM.LLVMInitializeNativeTarget;
import static org.bytedeco.llvm.global.LLVM.LLVMInt1Type;
import static org.bytedeco.llvm.global.LLVM.LLVMInt32Type;
import static org.bytedeco.llvm.global.LLVM.LLVMIntEQ;
import static org.bytedeco.llvm.global.LLVM.LLVMIntNE;
import static org.bytedeco.llvm.global.LLVM.LLVMIntSGE;
import static org.bytedeco.llvm.global.LLVM.LLVMIntSGT;
import static org.bytedeco.llvm.global.LLVM.LLVMIntSLE;
import static org.bytedeco.llvm.global.LLVM.LLVMIntSLT;
import static org.bytedeco.llvm.global.LLVM.LLVMLinkInMCJIT;
import static org.bytedeco.llvm.global.LLVM.LLVMModuleCreateWithName;
import static org.bytedeco.llvm.global.LLVM.LLVMPointerType;
import static org.bytedeco.llvm.global.LLVM.LLVMPositionBuilderAtEnd;
import static org.bytedeco.llvm.global.LLVM.LLVMPrintModuleToFile;
import static org.bytedeco.llvm.global.LLVM.LLVMSetInitializer;
import static org.bytedeco.llvm.global.LLVM.LLVMVoidType;

public class LLVMVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    private LLVMModuleRef module;

    private LLVMBuilderRef builder;

    private LLVMTypeRef i32Type;

    private String outputPath;

    private GlobalScope globalScope;

    private Scope currentScope;

    private Stack<LLVMBasicBlockRef> whileCondStack;    //??????whileCondition      continue

    private Stack<LLVMBasicBlockRef> entryStack;    //????????????????????????entry     break

    public LLVMVisitor(String outputPath){
        this.outputPath=outputPath;
        //?????????LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
        //??????module
        module = LLVMModuleCreateWithName("moudle");
        //?????????IRBuilder????????????????????????builder?????????LLVM IR????????????????????????builder?????????LLVM IR
        builder = LLVMCreateBuilder();
        //????????????????????????????????????int???????????????????????????????????????????????????LLVM???int??????????????????????????????
        i32Type = LLVMInt32Type();
        whileCondStack=new Stack<>();
        entryStack=new Stack<>();
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        globalScope=new GlobalScope(null);
        currentScope=globalScope;
        LLVMValueRef result=super.visitProgram(ctx);
        currentScope=currentScope.getEnclosingScope();
        return result;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        //?????????????????????
        LLVMTypeRef returnType = null;
        String retTypeStr=ctx.funcType().getText();
        if(retTypeStr.equals("int")){
            returnType=i32Type;
        }
        else if(retTypeStr.equals("void")){
            returnType=LLVMVoidType();
        }
        //????????????????????????
        int paramNum=0;
        if (ctx.funcFParams()!=null) {
            paramNum = ctx.funcFParams().funcFParam().size();
        }
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramNum);
        for (int i=0;i<paramNum;i++){
            //TODO
            if(ctx.funcFParams().funcFParam(i).L_BRACKT().size()==0) {
                argumentTypes.put(i, i32Type);
            } else {    // array
                LLVMTypeRef pointerType = LLVMPointerType(i32Type, 0);
                argumentTypes.put(i,pointerType);
            }
        }
        //??????????????????
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ paramNum, /* isVariadic */ 0);
        String funcName=ctx.IDENT().getText();
        //????????????????????????????????????module???????????????
        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/funcName,ft);
        //???????????????????????????????????????????????????????????????????????????????????????
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, /*blockName:String*/funcName+"Entry");
        //??????????????????????????????????????????
        LLVMPositionBuilderAtEnd(builder, block);//?????????????????????????????????block?????????
        //?????????
        FunctionSymbol functionSymbol=new FunctionSymbol(ctx.IDENT().getText(),globalScope,function,retTypeStr);
        for(int i=0;i<paramNum;i++){     //??????
            String paramName=ctx.funcFParams().funcFParam(i).IDENT().getText();
            if(ctx.funcFParams().funcFParam(i).L_BRACKT().size()==0) {
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/paramName + "Pointer");
                LLVMValueRef argValueRef = LLVMGetParam(function, i);
                LLVMBuildStore(builder, argValueRef, pointer);
                BaseSymbol baseSymbol = new BaseSymbol(paramName, pointer);
                functionSymbol.define(baseSymbol);
            }else {     // array
                LLVMTypeRef pointerType=LLVMPointerType(i32Type,0);
                LLVMValueRef pointer=LLVMBuildAlloca(builder, pointerType, paramName+"FuncPtr");
                LLVMValueRef argValueRef = LLVMGetParam(function, i);
                LLVMBuildStore(builder, argValueRef, pointer);
                ArraySymbol arraySymbol=new ArraySymbol(paramName,pointer,true);
                functionSymbol.define(arraySymbol);
            }
        }
        globalScope.define(functionSymbol);
        currentScope=functionSymbol;
        LLVMValueRef result=super.visitFuncDef(ctx);
        if(retTypeStr.equals("int")){
            LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
            LLVMBuildRet(builder,zero);
        }
        else if(retTypeStr.equals("void")){
            LLVMBuildRetVoid(builder);
        }
        //??????
        BytePointer error = new BytePointer();
        if (LLVMPrintModuleToFile(module, outputPath, error) != 0) {    // moudle??????????????????LLVMModuleRef??????
            LLVMDisposeMessage(error);
        }
        currentScope=currentScope.getEnclosingScope();
        return result;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        currentScope=new LocalScope(currentScope);
        LLVMValueRef result=super.visitBlock(ctx);
        currentScope=currentScope.getEnclosingScope();
        return result;
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        //??????????????????
        if(currentScope.equals(globalScope)){
            for (SysYParser.VarDefContext varDefContext:ctx.varDef()) {
                String varName=varDefContext.IDENT().getText();
                //??????????????????
                if(varDefContext.L_BRACKT().size()==0) {
                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/varName);
                    if(varDefContext.ASSIGN()!=null) {
                        LLVMValueRef valueRef = getExpValueRef(varDefContext.initVal().exp());
                        LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/valueRef);
                    }else {
                        LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
                        LLVMSetInitializer(globalVar,zero);
                    }
                    BaseSymbol baseSymbol=new BaseSymbol(varName,globalVar);
                    currentScope.define(baseSymbol);
                }else {    //??????????????????
                    int arrayNum= (int) LLVMConstIntGetSExtValue(getExpValueRef(varDefContext.constExp(0).exp()));
                    int initNum=0;
                    if(varDefContext.ASSIGN()!=null) {
                        initNum = varDefContext.initVal().initVal().size();
                    }
                    LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayNum);
                    LLVMValueRef globalArray=LLVMAddGlobal(module,arrayType,varName);
                    PointerPointer<LLVMValueRef> pointerPointer=new PointerPointer<>(arrayNum);
                    LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
                    for (int i=0;i<arrayNum;i++){
                        if(i<initNum){
                            LLVMValueRef valueRef=getExpValueRef(varDefContext.initVal().initVal(i).exp());
                            pointerPointer.put(i,valueRef);
                        }else {
                            pointerPointer.put(i,zero);
                        }
                    }
                    LLVMValueRef constArray=LLVMConstArray(arrayType,pointerPointer,arrayNum);
                    LLVMSetInitializer(globalArray, /* constantVal:LLVMValueRef*/constArray);
                    ArraySymbol arraySymbol=new ArraySymbol(varName,globalArray,false);
                    currentScope.define(arraySymbol);
                }
            }
            return super.visitVarDecl(ctx);
        }
        for (SysYParser.VarDefContext varDefContext:ctx.varDef()) {
            String varName=varDefContext.IDENT().getText();
            if(varDefContext.L_BRACKT().size()==0) {
                LLVMValueRef pointer=LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/varName+"_pointer");
                if(varDefContext.ASSIGN()!=null){       //??????????????????
                    LLVMValueRef llvmValueRef=getExpValueRef(varDefContext.initVal().exp());
                    LLVMBuildStore(builder, llvmValueRef, pointer);
                }
                BaseSymbol baseSymbol = new BaseSymbol(varName,pointer);
                currentScope.define(baseSymbol);
            }else {     //????????????
                int arrayNum= (int) LLVMConstIntGetSExtValue(getExpValueRef(varDefContext.constExp(0).exp()));
                int initNum=0;
                if(varDefContext.ASSIGN()!=null) {
                    initNum = varDefContext.initVal().initVal().size();
                }
                LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayNum);
                LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, arrayType, varName+"VectorPointer");
                LLVMValueRef zero=LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                LLVMValueRef[] arrayRef=new LLVMValueRef[arrayNum];
                for(int i=0;i<arrayNum;i++){
                    if(i<initNum){
                        arrayRef[i]=getExpValueRef(varDefContext.initVal().initVal(i).exp());
                    }else {
                        arrayRef[i]=zero;
                    }
                }
                LLVMValueRef[] arrayPointer=new LLVMValueRef[2];
                arrayPointer[0]=zero;
                for (int i=0;i<arrayNum;i++){
                    arrayPointer[1]=LLVMConstInt(i32Type,i,0);
                    PointerPointer<LLVMValueRef> indexPointer=new PointerPointer<>(arrayPointer);
                    LLVMValueRef elementPtr=LLVMBuildGEP(builder,vectorPointer,indexPointer,2,varName+"GEP"+i);
                    LLVMBuildStore(builder,arrayRef[i],elementPtr);
                }
                ArraySymbol arraySymbol=new ArraySymbol(varName,vectorPointer,false);
                currentScope.define(arraySymbol);
            }
        }
        return super.visitVarDecl(ctx);
    }
//
    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        if(currentScope.equals(globalScope)){
            for (SysYParser.ConstDefContext constDefContext:ctx.constDef()) {
                String name=constDefContext.IDENT().getText();
                //??????????????????
                if(constDefContext.L_BRACKT().size()==0) {
                    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/name);
                    LLVMValueRef valueRef = getExpValueRef(constDefContext.constInitVal().constExp().exp());
                    LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/valueRef);
                    BaseSymbol baseSymbol=new BaseSymbol(name,globalVar);
                    currentScope.define(baseSymbol);
                }else {    //??????????????????
                    int arrayNum= (int) LLVMConstIntGetSExtValue(getExpValueRef(constDefContext.constExp(0).exp()));
                    int initNum=constDefContext.constInitVal().constInitVal().size();
                    LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayNum);
                    LLVMValueRef globalArray=LLVMAddGlobal(module,arrayType,name);
                    PointerPointer<LLVMValueRef> pointerPointer=new PointerPointer<>(arrayNum);
                    LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
                    for (int i=0;i<arrayNum;i++){
                        if(i<initNum){
                            LLVMValueRef valueRef=getExpValueRef(constDefContext.constInitVal().constInitVal(i).constExp().exp());
                            pointerPointer.put(i,valueRef);
                        }else {
                            pointerPointer.put(i,zero);
                        }
                    }
                    LLVMValueRef constArray=LLVMConstArray(arrayType,pointerPointer,arrayNum);
                    LLVMSetInitializer(globalArray, /* constantVal:LLVMValueRef*/constArray);
                    ArraySymbol arraySymbol=new ArraySymbol(name,globalArray,false);
                    currentScope.define(arraySymbol);
                }
            }
            return super.visitConstDecl(ctx);
        }
        for(SysYParser.ConstDefContext constDefContext:ctx.constDef()){
            String name=constDefContext.IDENT().getText();
            if(constDefContext.L_BRACKT().size()==0){
                LLVMValueRef llvmValueRef=getExpValueRef(constDefContext.constInitVal().constExp().exp());
                LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/name+"_pointer");
                LLVMBuildStore(builder, llvmValueRef, pointer);
                BaseSymbol baseSymbol=new BaseSymbol(name,pointer);
                currentScope.define(baseSymbol);
            } else {    //????????????
                int arrayNum= (int) LLVMConstIntGetSExtValue(getExpValueRef(constDefContext.constExp(0).exp()));
                int initNum=constDefContext.constInitVal().constInitVal().size();
                LLVMTypeRef arrayType = LLVMArrayType(i32Type, arrayNum);
                LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, arrayType, name+"VectorPointer");
                LLVMValueRef zero=LLVMConstInt(i32Type, 0, /* signExtend */ 0);
                LLVMValueRef[] arrayRef=new LLVMValueRef[arrayNum];
                for(int i=0;i<arrayNum;i++){
                    if(i<initNum){
                        arrayRef[i]=getExpValueRef(constDefContext.constInitVal().constInitVal(i).constExp().exp());
                    }else {
                        arrayRef[i]=zero;
                    }
                }
                LLVMValueRef[] arrayPointer=new LLVMValueRef[2];
                arrayPointer[0]=zero;
                for (int i=0;i<arrayNum;i++){
                    arrayPointer[1]=LLVMConstInt(i32Type,i,0);
                    PointerPointer<LLVMValueRef> indexPointer=new PointerPointer<>(arrayPointer);
                    LLVMValueRef elementPtr=LLVMBuildGEP(builder,vectorPointer,indexPointer,2,name+"GEP"+i);
                    LLVMBuildStore(builder,arrayRef[i],elementPtr);
                }
                ArraySymbol arraySymbol=new ArraySymbol(name,vectorPointer,false);
                currentScope.define(arraySymbol);
            }
        }
        return super.visitConstDecl(ctx);
    }

    @Override
    public LLVMValueRef visitAssignStmt(SysYParser.AssignStmtContext ctx) {
        LLVMValueRef valueRef=getExpValueRef(ctx.exp());
        LLVMValueRef pointer=getLValPointer(ctx.lVal());
        LLVMBuildStore(builder,valueRef,pointer);
        return super.visitAssignStmt(ctx);
    }

    private LLVMValueRef getLValPointer(SysYParser.LValContext lValContext){
        String name=lValContext.IDENT().getText();
        Symbol symbol=currentScope.resolve(name);
        if (symbol instanceof BaseSymbol){
            return symbol.getPointer();
        }
        else if(symbol instanceof ArraySymbol){
            LLVMValueRef pointer=symbol.getPointer();
            if(((ArraySymbol) symbol).isFuncParam()){
                if(lValContext.L_BRACKT().size()==0){
                    return LLVMBuildLoad(builder,pointer,name+"ArrayPtr");
                }else {
                    LLVMValueRef arrayPtr=LLVMBuildLoad(builder,pointer,name+"ArrayPtr");
                    LLVMValueRef index = getExpValueRef((lValContext.exp(0)));
                    LLVMValueRef[] arrayPointer = new LLVMValueRef[1];
                    arrayPointer[0]=index;
                    PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
                    return LLVMBuildGEP(builder, arrayPtr, indexPointer, 1, name + "ArrPtrGEP");
                }
            }else {
                if(lValContext.L_BRACKT().size()==0){
                    LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
                    LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
                    arrayPointer[0] = zero;
                    arrayPointer[1] = zero;
                    PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
                    return LLVMBuildGEP(builder, pointer, indexPointer, 2, name + "GEPPtr");
                }else {
                    LLVMValueRef index = getExpValueRef((lValContext.exp(0)));
                    LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
                    LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);
                    arrayPointer[0] = zero;
                    arrayPointer[1] = index;
                    PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
                    return LLVMBuildGEP(builder, pointer, indexPointer, 2, name + "GEP");
                }
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitExpStmt(SysYParser.ExpStmtContext ctx) {
        if(ctx.exp()!=null) {
            getExpValueRef(ctx.exp());
        }
        return super.visitExpStmt(ctx);
    }


    @Override
    public LLVMValueRef visitIfStmt(SysYParser.IfStmtContext ctx) {
        LLVMValueRef condValueRef=getCondValueRef(ctx.cond());
        LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
        LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntNE,zero,condValueRef,"condition");
        LLVMValueRef function=currentFunctionSymbol().getPointer();
        LLVMBasicBlockRef ifTrue=LLVMAppendBasicBlock(function,"true");
        LLVMBasicBlockRef ifFalse=LLVMAppendBasicBlock(function,"false");
        LLVMBasicBlockRef entry=LLVMAppendBasicBlock(function,"entry");
        LLVMBuildCondBr(builder,condition,ifTrue,ifFalse);  //?????????????????????????????????????????????
        LLVMPositionBuilderAtEnd(builder,ifTrue);
        visit(ctx.stmt(0));
        LLVMBuildBr(builder,entry);
        LLVMPositionBuilderAtEnd(builder,ifFalse);
        if(ctx.ELSE()!=null) {
            visit(ctx.stmt(1));
        }
        LLVMBuildBr(builder,entry);
        LLVMPositionBuilderAtEnd(builder,entry);
        return null;
    }

    @Override
    public LLVMValueRef visitWhileStmt(SysYParser.WhileStmtContext ctx) {
        LLVMValueRef function=currentFunctionSymbol().getPointer();
        LLVMBasicBlockRef whileCondition=LLVMAppendBasicBlock(function,"whileCondition");
        LLVMBasicBlockRef whileBody=LLVMAppendBasicBlock(function,"whileBody");
        LLVMBasicBlockRef entry=LLVMAppendBasicBlock(function,"entry");
        whileCondStack.push(whileCondition);
        entryStack.push(entry);
        LLVMBuildBr(builder,whileCondition);
        LLVMPositionBuilderAtEnd(builder,whileCondition);
        LLVMValueRef zero=LLVMConstInt(i32Type,0,0);
        LLVMValueRef condRef=getCondValueRef(ctx.cond());
        LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntNE,zero,condRef,"condition != zero");
        LLVMBuildCondBr(builder,condition,whileBody,entry);
        LLVMPositionBuilderAtEnd(builder,whileBody);
        visit(ctx.stmt());
        LLVMBuildBr(builder,whileCondition);
        if(!entryStack.empty()&&entryStack.peek().equals(entry)){   //??????
            whileCondStack.pop();
            entryStack.pop();
        }
        LLVMPositionBuilderAtEnd(builder,entry);
        return null;
    }

    @Override
    public LLVMValueRef visitBreakStmt(SysYParser.BreakStmtContext ctx) {
        if(!entryStack.empty()) {
            whileCondStack.pop();
            LLVMBasicBlockRef entry = entryStack.pop();
            LLVMBuildBr(builder,entry);
        }
        return super.visitBreakStmt(ctx);
    }

    @Override
    public LLVMValueRef visitContinueStmt(SysYParser.ContinueStmtContext ctx) {
        if (!whileCondStack.empty()){
            LLVMBasicBlockRef whileCondition=whileCondStack.peek();
            LLVMBuildBr(builder,whileCondition);
        }
        return super.visitContinueStmt(ctx);
    }

    private LLVMValueRef getCondValueRef(SysYParser.CondContext condContext){
        if(condContext instanceof SysYParser.ExpCondContext){
            return getExpValueRef(((SysYParser.ExpCondContext) condContext).exp());
        }
        else if(condContext instanceof SysYParser.LtCondContext){
            LLVMValueRef numRef1=getCondValueRef(((SysYParser.LtCondContext) condContext).cond(0));
            LLVMValueRef numRef2=getCondValueRef(((SysYParser.LtCondContext) condContext).cond(1));
            if(((SysYParser.LtCondContext) condContext).LT()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntSLT,numRef1,numRef2,"condition = n1 < n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
            else if(((SysYParser.LtCondContext) condContext).GT()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntSGT,numRef1,numRef2,"condition = n1 > n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
            else if(((SysYParser.LtCondContext) condContext).LE()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntSLE,numRef1,numRef2,"condition = n1 <= n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
            else if(((SysYParser.LtCondContext) condContext).GE()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntSGE,numRef1,numRef2,"condition = n1 >= n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
        }
        else if(condContext instanceof SysYParser.EqCondContext){
            LLVMValueRef numRef1=getCondValueRef(((SysYParser.EqCondContext) condContext).cond(0));
            LLVMValueRef numRef2=getCondValueRef(((SysYParser.EqCondContext) condContext).cond(1));
            if(((SysYParser.EqCondContext) condContext).EQ()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntEQ,numRef1,numRef2,"condition = n1 == n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
            else if(((SysYParser.EqCondContext) condContext).NEQ()!=null){
                LLVMValueRef condition=LLVMBuildICmp(builder,LLVMIntNE,numRef1,numRef2,"condition = n1 != n2");
                return LLVMBuildZExt(builder,condition,i32Type,"temp");
            }
        }
        else if(condContext instanceof SysYParser.AndCondContext){
            LLVMValueRef numRef1=getCondValueRef(((SysYParser.AndCondContext) condContext).cond(0));
            LLVMValueRef numRef2=getCondValueRef(((SysYParser.AndCondContext) condContext).cond(1));
            return LLVMBuildAnd(builder,numRef1,numRef2,"and");
        }
        else if(condContext instanceof SysYParser.OrCondContext){
            LLVMValueRef numRef1=getCondValueRef(((SysYParser.OrCondContext) condContext).cond(0));
            LLVMValueRef numRef2=getCondValueRef(((SysYParser.OrCondContext) condContext).cond(1));
            return LLVMBuildOr(builder,numRef1,numRef2,"or");
        }
        return null;
    }

    private FunctionSymbol currentFunctionSymbol(){
        Scope result=currentScope;
        while (!(result instanceof FunctionSymbol)){
            result=result.getEnclosingScope();
        }
        return (FunctionSymbol) result;
    }

    @Override
    public LLVMValueRef visitReturnStmt(SysYParser.ReturnStmtContext ctx) {
        //??????????????????
        LLVMValueRef result=getExpValueRef(ctx.exp());
        LLVMBuildRet(builder, /*result:LLVMValueRef*/result);
        FunctionSymbol functionSymbol=currentFunctionSymbol();
        return super.visitReturnStmt(ctx);
    }

    private LLVMValueRef getExpValueRef(SysYParser.ExpContext expContext){
        if(expContext instanceof SysYParser.ParenExpContext){
            return getExpValueRef(((SysYParser.ParenExpContext) expContext).exp());
        }
        else if(expContext instanceof SysYParser.LValExpContext){
            //TODO
            String name=((SysYParser.LValExpContext) expContext).lVal().IDENT().getText();
            LLVMValueRef pointer=getLValPointer(((SysYParser.LValExpContext) expContext).lVal());
            Symbol symbol=currentScope.resolve(name);
            if(symbol instanceof ArraySymbol && ((SysYParser.LValExpContext) expContext).lVal().L_BRACKT().size()==0){
                return pointer;
            }else {
                return LLVMBuildLoad(builder, pointer,/*varName:String*/name);
            }
        }
        else if(expContext instanceof SysYParser.NumberExpContext){
            String text = expContext.getText();
            int num=convertNum(text);
            //??????????????????
            LLVMValueRef numRef = LLVMConstInt(i32Type, num, /* signExtend */ 0);
            return numRef;
        }
        else if(expContext instanceof SysYParser.FuncExpContext){   //????????????
            String funcName=((SysYParser.FuncExpContext) expContext).IDENT().getText();
            FunctionSymbol functionSymbol=(FunctionSymbol) currentScope.resolve(funcName);
            LLVMValueRef function=functionSymbol.getPointer();
            int argNum=0;
            if(((SysYParser.FuncExpContext) expContext).funcRParams()!=null) {
                argNum = ((SysYParser.FuncExpContext) expContext).funcRParams().param().size();
            }
            PointerPointer<LLVMValueRef> arguments=new PointerPointer<>(argNum);
            for (int i=0;i<argNum;i++){
                LLVMValueRef argValueRef=getExpValueRef(((SysYParser.FuncExpContext) expContext).funcRParams().param(i).exp());
                arguments.put(i,argValueRef);
            }
            if(functionSymbol.getRetTypeStr().equals("void")){
                return LLVMBuildCall(builder,function,arguments,argNum,"");
            }else {
                return LLVMBuildCall(builder, function, arguments, argNum, funcName);
            }
        }
        else if(expContext instanceof SysYParser.MulDivModExpContext){
            LLVMValueRef numRef1=getExpValueRef(((SysYParser.MulDivModExpContext) expContext).exp(0));
            LLVMValueRef numRef2=getExpValueRef(((SysYParser.MulDivModExpContext) expContext).exp(1));
            if(numRef1!=null&&numRef2!=null) {
                if (((SysYParser.MulDivModExpContext) expContext).MUL() != null) {
                    return LLVMBuildMul(builder, numRef1, numRef2, /* varName:String */"mul");
                } else if (((SysYParser.MulDivModExpContext) expContext).DIV() != null) {
                    return LLVMBuildSDiv(builder, numRef1, numRef2, /* varName:String */"div");
                } else if (((SysYParser.MulDivModExpContext) expContext).MOD()!=null){
                    return LLVMBuildSRem(builder,numRef1,numRef2,"mod");
                }
            }
        }
        else if(expContext instanceof SysYParser.PlusMinusExpContext){
            LLVMValueRef numRef1=getExpValueRef(((SysYParser.PlusMinusExpContext) expContext).exp(0));
            LLVMValueRef numRef2=getExpValueRef(((SysYParser.PlusMinusExpContext) expContext).exp(1));
            if(numRef1!=null&&numRef2!=null){
                if(((SysYParser.PlusMinusExpContext) expContext).PLUS()!=null){
                    return LLVMBuildAdd(builder,numRef1,numRef2,"add");
                }
                else if(((SysYParser.PlusMinusExpContext) expContext).MINUS()!=null){
                    return LLVMBuildSub(builder,numRef1,numRef2,"minus");
                }
            }
        }
        else if(expContext instanceof SysYParser.UnaryOpExpContext){
            LLVMValueRef numRef=getExpValueRef(((SysYParser.UnaryOpExpContext) expContext).exp());
            if(((SysYParser.UnaryOpExpContext) expContext).unaryOp().PLUS()!=null){
                return numRef;
            }
            else if(((SysYParser.UnaryOpExpContext) expContext).unaryOp().MINUS()!=null){
                return LLVMBuildNeg(builder,numRef,"neg");
            }
            else if(((SysYParser.UnaryOpExpContext) expContext).unaryOp().NOT()!=null){
                LLVMValueRef notZero=LLVMBuildICmp(builder,LLVMIntNE,LLVMConstInt(i32Type, 0, 0),numRef,"cmp");
                LLVMValueRef xorOne=LLVMBuildXor(builder, notZero, LLVMConstInt(LLVMInt1Type(), 1, 0), "xor");   //int1??????????????????
                return LLVMBuildZExt(builder, xorOne, i32Type, "i32");
            }
        }
        return null;
    }

    private int convertNum(String text){    //??????????????????
        int num;
        if (text.matches("0x[0-9a-fA-F]+") || text.matches("0X[0-9a-fA-F]+")) {
            num = Integer.parseInt(text.substring(2), 16);
        } else if (text.matches("0[0-7]+")) {
            num = Integer.parseInt(text.substring(1), 8);
        } else {
            num = Integer.parseInt(text);
        }
        return num;
    }
}
