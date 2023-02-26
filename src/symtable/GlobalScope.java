package symtable;

public class GlobalScope extends BaseScope{
    public GlobalScope(String name,Scope enclosingScope){
        super(name,enclosingScope);
    }

    public GlobalScope(Scope enclosingScope){
        super.enclosingScope=enclosingScope;
    }
}
