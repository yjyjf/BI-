package com.yjf.bi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yjf.bi.bizmq.BiMessageProducer;
import com.yjf.bi.common.ErrorCode;
import com.yjf.bi.constant.CommonConstant;
import com.yjf.bi.exception.BusinessException;
import com.yjf.bi.exception.ThrowUtils;
import com.yjf.bi.manager.AiManager;
import com.yjf.bi.manager.RedisLimiterManager;
import com.yjf.bi.mapper.ChartMapper;
import com.yjf.bi.model.dto.chart.ChartQueryRequest;
import com.yjf.bi.model.dto.chart.GenChartByAiRequest;
import com.yjf.bi.model.entity.Chart;
import com.yjf.bi.model.entity.User;
import com.yjf.bi.model.enums.ChartStatusEnum;
import com.yjf.bi.model.vo.BiResponse;
import com.yjf.bi.retry.GuavaRetrying;
import com.yjf.bi.service.ChartService;
import com.yjf.bi.service.UserService;
import com.yjf.bi.utils.ExcelUtils;
import com.yjf.bi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author YJF
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2024-05-06 22:10:45
 */
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private AiManager aiManager;

    @Resource
    private UserService userService;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private GuavaRetrying guavaRetrying;

    private static final long ONE_MB = 1024 * 1024L;

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        String name = chartQueryRequest.getName();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();

        // 校验数据
        // 分析目标不为空
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果名称存在且大于100 名称过长
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        this.validFile(multipartFile);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAi+" + loginUser.getId());

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = this.buildUserInput(goal, csvData, chartType);

        // 拿到已校验过的结果
//        String result = this.validAiResult(userInput);
        // 三次重试机会
        String result = guavaRetrying.retryDoChart(userInput);
        if(result == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String[] splits = result.split("#####");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();


        // 将结果插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatusEnum.SUCCEED.getValue());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 封装返回结果
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        log.info("{}使用了AI分析功能", loginUser.getId());
        return biResponse;
    }


    /**
     * 智能分析（异步）
     *`
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    @Override
    public BiResponse genChartByAiAsync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验数据
        // 分析目标不为空
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果名称存在且大于100 名称过长
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 文件大小
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件内容过大");
        // 后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffix = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式非法");

        // 限流
        Long userId = loginUser.getId();
        redisLimiterManager.doRateLimit("genChartByAi+" + userId);

        String csvData = ExcelUtils.excelToCsv(multipartFile);
        String userInput = this.buildUserInput(goal, csvData, chartType);


        // 将结果插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(userId);
        chart.setStatus(ChartStatusEnum.WAIT.getValue());
        boolean saveResult = this.save(chart);
        Long chartId = chart.getId();
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 在最终的返回结果前提交一个任务给线程池
        // todo 建议处理任务队列满了后,抛异常的情况(因为提交任务报错了,前端会返回异常)
        // 调用ai拿到结果
        CompletableFuture.runAsync(() -> {
            // / 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
            Chart running = new Chart();
            running.setId(chartId);
            running.setStatus(ChartStatusEnum.RUNNING.getValue());
            boolean runningResult = this.updateById(running);
            if (!runningResult) {
                this.handleChartUpdateError(chartId, "更新图表执行中状态失败");
                return;
            }

//            String result = aiManager.doChat(userInput);
//            String result = this.validAiResult(userInput);
            // 使用重试机制 三次机会
            String result = guavaRetrying.retryDoChart(userInput);
            if(result ==null){
                this.handleChartUpdateError(chartId, "AI 生成错误");
                return;
            }
            String[] splits = result.split("#####");
            if (splits.length < 3) {
                this.handleChartUpdateError(chartId, "AI 生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            Chart succeed = new Chart();
            succeed.setId(chartId);
            succeed.setGenChart(genChart);
            succeed.setGenResult(genResult);
            succeed.setStatus(ChartStatusEnum.SUCCEED.getValue());
            boolean succeedResult = this.updateById(succeed);
            if (!succeedResult) {
                this.handleChartUpdateError(chartId, "更新图表成功状态失败");
                return;
            }


            // 异步             // 调用完后剩余次数-1
            // 消费者执行后用户剩余次数-1
            User producerUser = userService.getById(userId);
            Integer leftNum = producerUser.getLeftNum();
            Integer totalNum = producerUser.getTotalNum();

            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setLeftNum(leftNum - 1);
            updateUser.setTotalNum(totalNum + 1);
            // 更新用户信息
            if(!userService.updateById(updateUser)){
                log.error("用户id={}：使用次数减少失败！",userId);
                this.handleChartUpdateError(chartId, "使用次数减少失败！");
                return ;
            }
        }, threadPoolExecutor);

        // 封装返回结果
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        log.info("{}使用了AI分析功能（异步）", userId);
        return biResponse;
    }

    /**
     * 智能分析（异步 消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    @Override
    public BiResponse genChartByAiAsyncMq(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验数据
        // 分析目标不为空
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果名称存在且大于100 名称过长
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 文件大小
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件内容过大");
        // 后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffix = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式非法");

        // 限流
        redisLimiterManager.doRateLimit("genChartByAi+" + loginUser.getId());

        //将表格内容转换为csv格式
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        // 将结果插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus(ChartStatusEnum.WAIT.getValue());
        boolean saveResult = this.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 将消息提交到消息队列
        // 消费者接收到消息后调用ai拿到结果
        Long chartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(chartId));

        // 封装返回结果
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        log.info("{}使用了AI分析功能（异步提交到消息队列）", loginUser.getId());
        return biResponse;
    }

    /**
     * 处理异常 设置图标状态为失败
     *
     * @param chartId
     * @param execMessage
     */
    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getValue());
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表更新失败");
        }
    }

    /**
     * 构建用户输入
     *
     * @param chart 图表对象
     * @return 用户输入字符串
     */
    @Override
    public String buildUserInput(Chart chart) {
        // 获取图表的目标、类型和数据
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        // 将StringBuilder转换为String并返回
        return userInput.toString();
    }

    /**
     * 构建用户输入
     *
     * @param goal
     * @param csvData
     * @param chartType
     * @return
     */
    @Override
    public String buildUserInput(String goal, String csvData, String chartType) {

        // 实现分析
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal);
        userInput.append("数据：").append("\n");
        // 压缩后的数据（把multipartFile传进来，其他的东西先注释）
        userInput.append(csvData);

        return userInput.toString();
    }

//    /**
//     * 校验ai生成的代码是否为json
//     * 总共三次机会 如果三次都生成错误则抛出异常
//     * @param userInput
//     * @return
//     */
//
//    public String validAiResult(String userInput) {
//        int cnt = 0;
//        String result = null;
//        // 总共三次机会 如果三次都生成错误则抛出异常
//        while (cnt++ < 3) {
//            result = aiManager.doChat(userInput);
//            String[] splits = result.split("#####");
//            if (splits.length < 3) {
//                log.error("ai生成失败第{}次", cnt);
//                continue;
//            }
//            String genChart = splits[1].trim();
//            if (com.yjf.bi.utils.StringUtils.isValidStrictly(genChart)) {
//                break;
//            }
//        }
//        ThrowUtils.throwIf(cnt >= 3, ErrorCode.SYSTEM_ERROR, "ai生成分析失败");
//        return result;
//    }



    /**
     * 校验文件是否合规
     * @param multipartFile
     */
    @Override
    public void validFile(MultipartFile multipartFile) {
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 文件大小

        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件内容过大");
        // 后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        List<String> validFileSuffix = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式非法");
    }
}




