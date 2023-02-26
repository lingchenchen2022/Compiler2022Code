package llvmsymtable;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class BaseSymbol implements Symbol{
    private String name;
    private LLVMValueRef pointer;


    public BaseSymbol(String name,LLVMValueRef pointer){
        this.name=name;
        this.pointer=pointer;
    }

    @Override
    public String getName(){
        return name;
    }

    @Override
    public LLVMValueRef getPointer(){
        return pointer;
    }

}
