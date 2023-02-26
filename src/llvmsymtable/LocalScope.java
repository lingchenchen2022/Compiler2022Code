package llvmsymtable;

public class LocalScope extends BaseScope{

    public LocalScope(String name,Scope enclosingScope){
        super(name,enclosingScope);
    }

    public LocalScope(Scope enclosingScope){
        super.enclosingScope=enclosingScope;
    }
}
