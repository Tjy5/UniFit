package com.authguard.usersystem.util; // 或者放在更通用的包下

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;       // 总记录数
    private List<T> list;     // 当前页数据列表
    private int pageNum;      // 当前页码
    private int pageSize;     // 每页数量
    private int pages;        // 总页数

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }
}
