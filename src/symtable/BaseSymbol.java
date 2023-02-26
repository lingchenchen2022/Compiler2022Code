package symtable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseSymbol implements Symbol{
    final String name;
    final Type type;

    Map<Integer, ArrayList<Integer>> lineColumn;
    /**
     *
     * @param name
     * @param type
     */
    public BaseSymbol(String name,Type type){
        this.name=name;
        this.type=type;
        lineColumn= new LinkedHashMap<>();
    }

    @Override
    public void setLineColumn(int lineNo,int column) {
        if(!lineColumn.containsKey(lineNo)){
            lineColumn.put(lineNo,new ArrayList<>());
        }
        lineColumn.get(lineNo).add(column);
    }

    @Override
    public String getName(){
        return name;
    }

    @Override
    public Type getType(){
        return type;
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
