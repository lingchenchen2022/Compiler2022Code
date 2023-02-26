package symtable;

public class LocalScope extends BaseScope{
    public LocalScope(String name,Scope enclosingScope){
        super(name,enclosingScope);
    }

    public LocalScope(Scope enclosingScope){
        super.enclosingScope=enclosingScope;
    }

    public FunctionSymbol findFuncScope(){      //找到当前所在函数作用域
        Scope scope=getEnclosingScope();
        while(!(scope instanceof FunctionSymbol)){
            scope=scope.getEnclosingScope();
        }
        return (FunctionSymbol) scope;
    }

}
