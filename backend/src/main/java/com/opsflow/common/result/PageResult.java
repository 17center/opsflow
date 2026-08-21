package com.opsflow.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 每页条数 */
    private Long size;

    /** 当前页码 */
    private Long current;

    /** 总页数 */
    private Long pages;

    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setSize(size);
        result.setCurrent(current);
        result.setPages(total == 0 ? 0 : (total + size - 1) / size);
        return result;
    }
}