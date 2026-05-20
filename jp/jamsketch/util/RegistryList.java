package jp.jamsketch.util;

import java.util.HashMap;
import java.util.function.Supplier;

public class RegistryList<T> {
    private final HashMap<String, RegistryObject<T>> registries = new HashMap<>();

    private RegistryList(){}


    public RegistryObject<T> create(Class<? extends T> cl, String id){
        RegistryObject<T> reg = RegistryObject.register(cl);
        registries.put(id, reg);
        return reg;
    }

    public static <T> RegistryList<T> create(){
        return new RegistryList<>();
    }

    public T get(String search, Object... objects){
        RegistryObject<T> s = registries.get(search);
        return s != null ? s.get(objects) : null;
    }
}
