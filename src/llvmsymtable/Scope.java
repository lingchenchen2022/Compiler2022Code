package llvmsymtable;

import java.util.Map;

public interface Scope {
    public void setName(String name);

    public Map<String, Symbol> getSymbols();

    public Scope getEnclosingScope();

    public void define(Symbol symbol);

    public String getName();

    /**
     * 符号解析
     * @param name
     * @return
     */
    public Symbol resolve(String name);

    public Symbol currentResolve(String name);
}
