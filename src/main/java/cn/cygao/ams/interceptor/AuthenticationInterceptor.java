package cn.cygao.ams.interceptor;

import cn.cygao.ams.annotation.PassAuthentication;
import cn.cygao.ams.exception.apiException.AuthenticationException;
import cn.cygao.ams.exception.apiException.authenticationException.JwtExpiredException;
import cn.cygao.ams.exception.apiException.authenticationException.PermissionDeniedException;
import cn.cygao.ams.exception.apiException.authenticationException.TokenCheckException;
import cn.cygao.ams.exception.apiException.authenticationException.TokenNotFoundException;
import cn.cygao.ams.annotation.RequiresLogin;
import cn.cygao.ams.config.JwtConfig;
import cn.cygao.ams.constants.RedisConstants;
import cn.cygao.ams.dto.JwtPayloadDto;
import com.cygao.ams.exception.apiException.authenticationException.*;
import cn.cygao.ams.service.RedisService;
import cn.cygao.ams.util.IpUtil;
import cn.cygao.ams.util.JwtUtil;
import cn.cygao.ams.util.ThreadLocalUtil;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 鉴权拦截器
 *
 * @author STEA_YY
 **/
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Resource
    private RedisService redisService;
    @Resource
    private JwtConfig jwtConfig;

    /**
     * 处理鉴权请求
     *
     * @param request  用户请求
     * @param response HTTP响应
     * @param handler  被拦截对象
     * @return 是否通过
     * @throws AuthenticationException 鉴权失败异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthenticationException {
        // 从 http 请求头中取出 token
        String token = request.getHeader("Authorization");
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        //检查是否有passtoken注释，有则跳过认证
        if (method.isAnnotationPresent(PassAuthentication.class)) {
            PassAuthentication passAuthentication = method.getAnnotation(PassAuthentication.class);
            if (passAuthentication.required()) {
                return true;
            }
        }
        //检查有没有需要用户权限的注解
        if (method.isAnnotationPresent(RequiresLogin.class)) {
            RequiresLogin requiresLogin = method.getAnnotation(RequiresLogin.class);
            if (requiresLogin.required()) {
                // 执行认证
                if (token == null) {
                    throw new TokenNotFoundException("无token，请重新登录");
                }
                JwtPayloadDto jwtPayloadDto = JwtUtil.getPayload(token);
                try {
                    boolean verify = JwtUtil.verify(token, jwtPayloadDto, jwtConfig.getSecret());
                    if (!verify) {
                        throw new TokenCheckException();
                    }
                } catch (TokenExpiredException e) {
                    if (!refreshToken(token, jwtPayloadDto)) {
                        throw new JwtExpiredException();
                    }
                }
                if (requiresLogin.requiresRoles().length > 0) {
                    boolean hasPermission = false;
                    for (String roleName : requiresLogin.requiresRoles()) {
                        if (roleName.equals(jwtPayloadDto.getRoleName())) {
                            hasPermission = true;
                            break;
                        }
                    }
                    if (!hasPermission) {
                        throw new PermissionDeniedException();
                    }
                }

                ThreadLocalUtil.setCurrentUser(jwtPayloadDto.getAccount());
                MDC.put("userId", jwtPayloadDto.getAccount().toString());
                MDC.put("ip", IpUtil.getIpAddr(request));
                return true;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalUtil.removeCurrentUser();
        MDC.clear();
    }

    /**
     * 尝试刷新用户token
     *
     * @param token         过期token
     * @param jwtPayloadDto 当前用户的payload
     * @return 是否刷新成功
     */
    protected boolean refreshToken(String token, JwtPayloadDto jwtPayloadDto) {
        String possibleToken = (String) redisService.get(RedisConstants.REFRESH_TOKEN_PREFIX + jwtPayloadDto.getAccount());
        if (possibleToken != null && possibleToken.equals(token)) {
            String newToken = JwtUtil.sign(jwtPayloadDto, jwtConfig.getSecret(), jwtConfig.getExpireTime());
            redisService.set(RedisConstants.REFRESH_TOKEN_PREFIX + jwtPayloadDto.getAccount(), newToken, jwtConfig.getRefreshTokenExpireTime());
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletResponse response;
            if (requestAttributes != null) {
                response = requestAttributes.getResponse();
                if (response != null) {
                    response.setHeader("Authorization", newToken);
                }
            }

            return true;
        }
        return false;
    }
}
