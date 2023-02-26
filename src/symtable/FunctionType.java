package symtable;

import java.util.ArrayList;

public class FunctionType implements Type{
    private Type retType;
    private ArrayList<Type> paramsType;

    public FunctionType(Type retType){
        this.retType=retType;
        this.paramsType=new ArrayList<>();
    }

    public Type getRetType(){
        return retType;
    }

    public void setParamsType(ArrayList<Type> paramsType) {
        this.paramsType = paramsType;
    }

    public void addParamType(Type paramType){
        paramsType.add(paramType);
    }

    public ArrayList<Type> getParamsType(){
        return paramsType;
    }

    @Override
    public int getNumber() {
        return 3;
    }
}
