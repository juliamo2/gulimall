package com.atguigu.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.MemberFeignService;
import com.atguigu.gulimall.authserver.vo.SocialUser;
import com.atguigu.common.vo.MemeberRespVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.atguigu.common.constant.AuthServiceConstant;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;


@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/gitee")
    public String gitee(@RequestParam("code") String code, HttpSession sesson) throws Exception{

        System.out.println(code);

        Map<String, String> header = new HashMap<>();
        Map<String, String> query = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("code",code);
        map.put("client_id","914f17084214092ba1b8823d59800f7683f09dc8601b5e16181135fa1da3e675");
        map.put("client_secret","99ddfeb1449614b5e55684821925000602852e47321c19470010c0fd74ee52e7");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/gitee");
        map.put("grant_type","authorization_code");
        HttpResponse Response = HttpUtils.doPost("https://gitee.com","/oauth/token","post",header,query,map);

        if(Response.getStatusLine().getStatusCode() == 200){
            String json = EntityUtils.toString(Response.getEntity());
            System.out.println(json);
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            String access_token = socialUser.getAccess_token();
            System.out.println("///////////////////////////////////");
            System.out.println(access_token);

            //使用access_token
            //1.用户第一次进入网站，自动注册
            R r = memberFeignService.oauthlogin(socialUser);
            if(r.getCode()==0){

                MemeberRespVo data = r.getData("data", new TypeReference<MemeberRespVo>() {
                });
                System.out.println("登录成功"+data);

                sesson.setAttribute(AuthServiceConstant.SESSION_LOGIN_KEY,data);

                return "redirect:http://gulimall.com";
            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }

            //2.非第一次
        }else{

            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}