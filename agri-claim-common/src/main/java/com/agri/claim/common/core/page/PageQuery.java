package com.agri.claim.common.core.page;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Data
public class PageQuery {

    private static final String PAGE_NUM = "pageNum";
    private static final String PAGE_SIZE = "pageSize";
    private static final String ORDER_BY_COLUMN = "orderByColumn";
    private static final String IS_ASC = "isAsc";

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderByColumn;
    private String isAsc = "asc";

    public static PageQuery build() {
        PageQuery pageQuery = new PageQuery();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return pageQuery;
        }
        HttpServletRequest request = attributes.getRequest();

        String pageNumStr = request.getParameter(PAGE_NUM);
        String pageSizeStr = request.getParameter(PAGE_SIZE);
        String orderByColumnStr = request.getParameter(ORDER_BY_COLUMN);
        String isAscStr = request.getParameter(IS_ASC);

        if (StrUtil.isNotBlank(pageNumStr)) {
            pageQuery.setPageNum(Convert.toInt(pageNumStr, 1));
        }
        if (StrUtil.isNotBlank(pageSizeStr)) {
            pageQuery.setPageSize(Convert.toInt(pageSizeStr, 10));
        }
        if (StrUtil.isNotBlank(orderByColumnStr)) {
            pageQuery.setOrderByColumn(orderByColumnStr);
        }
        if (StrUtil.isNotBlank(isAscStr)) {
            pageQuery.setIsAsc(isAscStr);
        }
        return pageQuery;
    }

    public <T> IPage<T> toPage() {
        Page<T> page = new Page<>(pageNum, pageSize);
        if (StrUtil.isNotBlank(orderByColumn)) {
            boolean asc = "asc".equalsIgnoreCase(isAsc);
            OrderItem orderItem = asc ? OrderItem.asc(orderByColumn) : OrderItem.desc(orderByColumn);
            page.setOrders(List.of(orderItem));
        }
        return page;
    }
}
