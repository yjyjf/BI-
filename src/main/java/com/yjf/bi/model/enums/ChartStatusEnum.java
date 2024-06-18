package com.yjf.bi.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图表状态枚举
 * 排队中0-wait、执行中1-running、已完成2-succeed、失败3-failed
 */
public enum ChartStatusEnum {
    WAIT(0,"wait"),
    RUNNING(1,"running"),
    SUCCEED(2,"succeed"),
    FAILED(3,"failed");

    private final Integer value;
    private final String text;

    ChartStatusEnum(Integer value,String text){
        this.value= value;
        this.text=text;
    }

    /**
     * 获取值列表
     */
    public static List<Integer> getValues(){
//        1.将枚举类型的数组转换为Stream；
//        2.提取每个枚举常量的value属性；
//        3.创建一个包含所有value属性的新List列表；
//        4.返回这个包含value属性值的List列表。
       return Arrays.stream(values()).map(item->item.value).collect(Collectors.toList());
    }

    /**
     * 根据value获取枚举
     */
    public static ChartStatusEnum getEnumByValue(String value){
        if (ObjectUtils.isEmpty(value)){
            return null;
        }
        for (ChartStatusEnum Enum: ChartStatusEnum.values()){
            if (Enum.value.equals(value)){
                return Enum;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
