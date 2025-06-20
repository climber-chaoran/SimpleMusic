package org.litepal.crud;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.litepal.exceptions.LitePalSupportException;

/* loaded from: classes.dex */
class DynamicExecutor {
    private DynamicExecutor() {
    }

    static Object send(Object obj, String str, Object[] objArr, Class<?> cls, Class<?>[] clsArr) throws SecurityException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (objArr == null) {
            objArr = new Object[0];
        }
        if (clsArr == null) {
            clsArr = new Class[0];
        }
        Method declaredMethod = null;
        try {
            declaredMethod = cls.getDeclaredMethod(str, clsArr);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(obj, objArr);
    }

    static void set(Object obj, String str, Object obj2, Class<?> cls) throws SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        declaredField.set(obj, obj2);
    }

    static void setField(Object obj, String str, Object obj2, Class<?> cls) throws SecurityException, IllegalArgumentException, IllegalAccessException {
        if (cls == LitePalSupport.class || cls == Object.class) {
            throw new LitePalSupportException(LitePalSupportException.noSuchFieldExceptioin(cls.getSimpleName(), str));
        }
        try {
            set(obj, str, obj2, cls);
        } catch (NoSuchFieldException unused) {
            setField(obj, str, obj2, cls.getSuperclass());
        }
    }

    static Object getField(Object obj, String str, Class<?> cls) throws IllegalArgumentException, IllegalAccessException {
        if (cls == LitePalSupport.class || cls == Object.class) {
            throw new LitePalSupportException(LitePalSupportException.noSuchFieldExceptioin(cls.getSimpleName(), str));
        }
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField.get(obj);
        } catch (NoSuchFieldException unused) {
            return getField(obj, str, cls.getSuperclass());
        }
    }
}
