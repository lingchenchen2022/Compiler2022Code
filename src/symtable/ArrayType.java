package symtable;

public class ArrayType implements Type{
    Type subType;
    Integer count;
    int dimension;     //数组维度

    public ArrayType(Integer count,int dimension){
        this.subType=null;
        this.count=count;
        this.dimension=dimension;
    }

    public void setSubType(Type subType){
        this.subType=subType;
    }

    public Type getSubType(){
        return subType;
    }

    public Type getDimensionType(int dimension){
        if(dimension>this.dimension){
            return null;
        }
        Type result=this;
        for (int i=0;i<dimension;i++){
            result=((ArrayType)result).getSubType();
        }
        return result;
    }

    public int getDimension() {
        return dimension;
    }

    @Override
    public int getNumber() {
        return 2;
    }
}
