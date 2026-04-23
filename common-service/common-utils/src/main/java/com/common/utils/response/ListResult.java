package com.common.utils.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListResult<T> {

    @Builder.Default
    private int total = 0;

    @Builder.Default
    private List<T> data = new ArrayList<>();

    // 静态工程方法
    public static <T> ListResult<T> result(int total, List<T> data) {
        return ListResult.<T>builder()
                .total(total)
                .data(data)
                .build();
    }
}
