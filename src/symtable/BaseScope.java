package symtable;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope{
    protected Scope enclosingScope;

    protected Map<String,Symbol> symbols=new LinkedHashMap<String,Symbol>();

    protected String name;

    public BaseScope(){};

    public BaseScope(String name,Scope enclosingScope){
        this.name=name;
        this.enclosingScope=enclosingScope;
    }

    @Override
    public void setName(String name) {
        this.name=name;
    }

    @Override
    public Map<String, Symbol> getSymbols() {
        return symbols;
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(),symbol);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Symbol resolve(String name) {
        Symbol symbol=symbols.get(name);
        if(symbol!=null){
            return symbol;
        }
        if(enclosingScope!=null){
            return enclosingScope.resolve(name);
        }
        return null;
    }

    @Override
    public Symbol currentResolve(String name) {
        return symbols.get(name);
    }
}
