package com.yjf.bi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yjf.bi.model.dto.chart.ChartQueryRequest;
import com.yjf.bi.model.dto.chart.GenChartByAiRequest;
import com.yjf.bi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yjf.bi.model.entity.User;
import com.yjf.bi.model.vo.BiResponse;
import org.springframework.web.multipart.MultipartFile;

/**
* @author YJF
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-05-06 22:10:45
*/
public interface ChartService extends IService<Chart> {

    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);

    BiResponse genChartByAiAsync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);

    BiResponse genChartByAiAsyncMq(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);


    void handleChartUpdateError(long chartId, String execMessage);
    String buildUserInput(Chart chart);

    String buildUserInput(String goal, String csvDate, String chartType);

//    String validAiResult(String userInput);

     void validFile(MultipartFile multipartFile) ;
}
