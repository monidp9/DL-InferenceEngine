package unina.utility;

public class Container<T> {
    private T object = null;

    public Container(){}

    public Container(T object){
        this.object = object;
    }

    public T getValue(){
        return object;
    }

    public void setValue(T object){
        this.object = object;
    }
}
