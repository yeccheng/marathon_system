package com.ape.marathon.common;

import java.util.List;

/**
 * 分页返回结果封装
 * 对应接口文档中的分页返回值：
 * { total, pages, pageNum, pageSize, list }
 */
public class PageResult<T> {

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long pages;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 当前页数据 */
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

    /**
     * 构造分页结果：根据 total 与 pageSize 自动计算总页数
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页条数
     * @param list     当前页数据
     */
    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> list) {
        // 总页数 = 向上取整（total / pageSize）
        long pages = pageSize == null || pageSize == 0 ? 1 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(total, pages, pageNum, pageSize, list);
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