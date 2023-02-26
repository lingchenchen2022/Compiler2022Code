package llvmsymtable;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class ArraySymbol implements Symbol{
    private String name;

    private LLVMValueRef pointer;

    private boolean isFuncParam;    //是否是函数参数

    public ArraySymbol(String name,LLVMValueRef pointer,boolean isFuncParam){
        this.name=name;
        this.pointer=pointer;
        this.isFuncParam=isFuncParam;
    }

    public boolean isFuncParam() {
        return isFuncParam;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LLVMValueRef getPointer(){
        return pointer;
    }

}
