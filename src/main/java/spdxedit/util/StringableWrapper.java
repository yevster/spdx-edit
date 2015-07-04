package spdxedit.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Trivial decorator of any object with a dynamically assignable toString() method.
 * Useful for populating items in JavaFX tables/list without extending their cell classes (yuck!)
 * Delegates equals() and hashcode() to the underlying value.
 */
public class StringableWrapper<T>{
    private T value;
    private Function<T, String> toString;

    public static <T> StringableWrapper<T> wrap(T value, Function<T, String> toString){
        return new StringableWrapper<>(value, toString);
    }

    private StringableWrapper(T value, Function<T, String> toString){
        this.value = value;
        this.toString = toString;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(value, obj);
    }

    @Override
    public String toString(){
        return this.toString.apply(value);
    }

    public T getValue(){
        return value;
    }
}
