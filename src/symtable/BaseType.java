package symtable;

public class BaseType implements Type{
    private String name;     //'int' or 'void'

    public BaseType(String name){
        this.name=name;
    }

    public String getName(){
        return name;
    }

    @Override
    public int getNumber() {
        if(name.equals("void")){
            return 0;
        }
        else if(name.equals("int")){
            return 1;
        }
        else {
            return -1;
        }
    }
}
