package com.yjf.bi.aop;

import com.yjf.bi.annotation.AuthCheck;
import com.yjf.bi.annotation.UseCheck;
import com.yjf.bi.common.ErrorCode;
import com.yjf.bi.exception.BusinessException;
import com.yjf.bi.model.entity.User;
import com.yjf.bi.model.enums.UserRoleEnum;
import com.yjf.bi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.yjf.bi.constant.ChartConstant.SYNC_METHOD;

/**
 * 权限校验 切面拦截器
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 必须有该权限才通过
        if (StringUtils.isNotBlank(mustRole)) {
            UserRoleEnum mustUserRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            if (mustUserRoleEnum == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            String userRole = loginUser.getUserRole();
            // 如果被封号，直接拒绝
            if (UserRoleEnum.BAN.equals(mustUserRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 必须有管理员权限
            if (UserRoleEnum.ADMIN.equals(mustUserRoleEnum)) {
                if (!mustRole.equals(userRole)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
                }
            }
        }
        // 通过权限校验，放行，上述代码在调用目标方法前执行
        return joinPoint.proceed();
    }

    /**
     * 校验是否还有调用次数
     */
    @Around("@annotation(useCheck)")
    public Object doInterceptor2(ProceedingJoinPoint joinPoint,UseCheck useCheck) throws Throwable{
        //获取调用的方法（同步还是异步）
        String method = useCheck.method();
        if (StringUtils.isBlank(method) ) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //判断是否还有调用次数
        //1.获取request
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request =( (ServletRequestAttributes) requestAttributes).getRequest();
        //2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        if (loginUser==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"未登录");
        }
        Integer leftNum =loginUser.getLeftNum();
        Integer totalNum = loginUser.getTotalNum();
        //剩余次数是否大于0
        if (leftNum<=0){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"剩余调用次数不足");
        }
        //异步到此结束，等消息队列或线程执行完成后，再减次数

        //同步，减次数
        if(SYNC_METHOD.equals(method)){
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setTotalNum(totalNum+1);
            updateUser.setLeftNum(leftNum-1);

            //更新信息
            //根据updateUser对象的ID属性，找到对应ID的用户记录，并使用updateUser对象中的非空字段来更新数据库中的记录
            boolean result = userService.updateById(updateUser);
            if (!result){
                log.error("用户id={}:减少调用次数的操作失败",userId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新操作失败，未能减少调用次数");
            }

        }
        //继续执行被拦截的方法，上述代码在调用方法前执行
        return joinPoint.proceed();

    }
}

