package llvmsymtable;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class FunctionSymbol extends BaseScope implements Symbol{
    private LLVMValueRef funcRef;
    private String retTypeStr;

    public FunctionSymbol(String name,Scope enclosingScope,LLVMValueRef funcRef,String retTypeStr){
        super(name,enclosingScope);
        this.funcRef=funcRef;
        this.retTypeStr=retTypeStr;
    }

    @Override
    public LLVMValueRef getPointer(){
        return funcRef;
    }

    public String getRetTypeStr(){
        return retTypeStr;
    }
}
