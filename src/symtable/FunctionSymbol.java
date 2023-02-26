package symtable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionSymbol extends BaseScope implements Symbol{
    private FunctionType functionType;

    private boolean isValid;

    Map<Integer, ArrayList<Integer>> lineColumn;

    public FunctionSymbol(String name,Scope enclosingScope,FunctionType functionType,boolean isValid){
        super(name,enclosingScope);
        this.functionType=functionType;
        this.isValid=isValid;
        lineColumn= new LinkedHashMap<>();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public Type getType() {
        return functionType;
    }

    public boolean isValid(){
        return isValid;
    }

    public void setParamsType(ArrayList<Type> paramsType) {
        functionType.setParamsType(paramsType);
    }

    public void addParamType(Type paramType){
        functionType.addParamType(paramType);
    }

    public ArrayList<Type> getParamsType(){
        return functionType.getParamsType();
    }

    @Override
    public void setLineColumn(int lineNo,int column) {
        if(!lineColumn.containsKey(lineNo)){
            lineColumn.put(lineNo,new ArrayList<>());
        }
        lineColumn.get(lineNo).add(column);
    }

    @Override
    public boolean isInLineColumn(int lineNo,int column){
        if(!lineColumn.containsKey(lineNo)){
            return false;
        }
        ArrayList<Integer> columnArr=lineColumn.get(lineNo);
        for (Integer columnTemp:columnArr){
            if(columnTemp==column){
                return true;
            }
        }
        return false;
    }
}
