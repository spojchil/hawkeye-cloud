package com.common.utils.enums;

public interface CodeEnum {
    int getCode();
    String getDesc();

    static <E extends Enum<?> & CodeEnum> E fromCode(Class<E> enumClass, int code) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        return null;
    }
}
