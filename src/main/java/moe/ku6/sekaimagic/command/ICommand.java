package moe.ku6.sekaimagic.command;

import com.beust.jcommander.JCommander;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract interface ICommand<T> {
    String[] GetNames();
    String GetManual();
    void HandleInternal(T args) throws Exception;

    default void Handle(Object args) throws Exception {
        HandleInternal((T)args);
    }

    default T CreateParameterObject() {
        try {
            // Get the actual type argument T from the handler's class hierarchy
            Class<?> handlerClass = getClass();
            Type type = FindParameterizedType(handlerClass, ICommand.class);
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)type;
                Class<T> clazz = (Class<T>)parameterizedType.getActualTypeArguments()[0];

                // Create a new instance of T using the default constructor
                Constructor<T> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true); // In case the constructor is not public
                return constructor.newInstance();
            }
            throw new IllegalArgumentException("Type parameter T is not specified correctly.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create an instance of T", e);
        }
    }


    default String GetUsage() {
        var obj = JCommander.newBuilder()
                .addObject(CreateParameterObject())
                .build();
        StringBuilder sb = new StringBuilder();
        obj.usage(sb);

        return sb.toString();
    }

    private static Type FindParameterizedType(Class<?> clazz, Class<?> targetInterface) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType && ((ParameterizedType)type).getRawType().equals(targetInterface)) {
                return type;
            }
        }

        if (clazz.getSuperclass() != null) {
            return FindParameterizedType(clazz.getSuperclass(), targetInterface);
        }

        throw new IllegalArgumentException("Specified interface not found in class hierarchy.");
    }
}
