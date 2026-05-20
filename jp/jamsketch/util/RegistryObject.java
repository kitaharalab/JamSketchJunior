package jp.jamsketch.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public class RegistryObject<T> {

    private final Class<? extends T> register;

    private RegistryObject(Class<? extends T> register){
        this.register = register;
    }

    public static <T> RegistryObject<T> register(Class<? extends T>  cl){
        return new RegistryObject<>(cl);
    }

    public T get(Object... objects){
        try {
            Constructor<? extends T> constructor = register.getConstructor();
            return constructor.newInstance(objects);

        } catch (NoSuchMethodException e)
        {
            System.out.println("Not Found Constructor :" + register);
            throw new RuntimeException(e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);

        }
    }
}
