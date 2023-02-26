package symtable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArraySymbol implements Symbol{
    private String name;
    private ArrayType arrayType;
    Map<Integer, ArrayList<Integer>> lineColumn;

    public ArraySymbol(String name,ArrayType arrayType){
        this.name=name;
        this.arrayType=arrayType;
        lineColumn= new LinkedHashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return arrayType;
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
