package com.yjf.bi.model.vo;

import lombok.Data;

@Data
public class BiResponse {
    private String genChart;
    private String genResult;
    //新生成的图标id
    private Long chartId;
}
