package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServiceConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemeberRespVo;
import com.atguigu.gulimall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {



        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();

//        System.out.println("session////////////////////////////////////////////////////////////////////////////");
//        System.out.println(session.getId());

        MemeberRespVo memeber = (MemeberRespVo) session.getAttribute(AuthServiceConstant.SESSION_LOGIN_KEY);
//        System.out.println("member");
//        System.out.println(memeber);
        if(memeber!=null){
//            System.out.println("???????");
            userInfoTo.setUserId(memeber.getId());
        }

        Cookie[] cookies = request.getCookies();

//        System.out.println(cookies.length);

        if (cookies!=null&&cookies.length>0){
            for(Cookie cookie : cookies){
                String name = cookie.getName();
//                System.out.println(name);
                if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
//                    System.out.println(cookie.getValue());
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }

        if (StringUtils.isEmpty(userInfoTo.getUserKey())){
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        threadLocal.set(userInfoTo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        if (!userInfoTo.getTempUser()){
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME,userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }
}
