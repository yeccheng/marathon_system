package com.ape.marathon.common;

import java.util.List;

public class PageResult<T> {

    private Long total;
    private Long pages;
    private Integer pageNum;
    private Integer pageSize;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(Long total, Long pages, Integer pageNum, Integer pageSize, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        long pages = pageSize == null || pageSize == 0 ? 1 : (total + pageSize - 1) / pageSize;
        return new PageResult<T>(total, pages, pageNum, pageSize, list);
    }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getPages() { return pages; }
    public void setPages(Long pages) { this.pages = pages; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
