package symtable;

public interface Symbol {
    public String getName();

    public Type getType();

    public void setLineColumn(int lineNo,int column);

    public boolean isInLineColumn(int lineNo,int column);
}
