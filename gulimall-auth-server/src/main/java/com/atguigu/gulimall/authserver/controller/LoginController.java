package com.atguigu.gulimall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.MemberFeignService;
import com.atguigu.gulimall.authserver.feign.ThirdPartFeignService;
import com.atguigu.gulimall.authserver.vo.UserLoginVo;
import com.atguigu.gulimall.authserver.vo.UserRegisterVo;
import com.atguigu.common.vo.MemeberRespVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.atguigu.common.constant.AuthServiceConstant;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        String rediscode = stringRedisTemplate.opsForValue().get(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(rediscode)){
            long l = Long.parseLong(rediscode.split("_")[1]);
            if(System.currentTimeMillis() - l < 60000){
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMsg());
            }
        }

        //接口防刷

        //验证码再次校验 redis key:phone code:
        String code = "123456"+"_"+System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(AuthServiceConstant.SMS_CODE_CACHE_PREFIX+phone,code,10, TimeUnit.MINUTES);


        thirdPartFeignService.sendCode(phone,"123456");
        System.out.println(phone);

        return R.ok();
    }

    @PostMapping("/register")
    public String register(@Valid UserRegisterVo vo, BindingResult result,
                           RedirectAttributes redirectAttributes){

        if(result.hasErrors()){
//            log.error(result.getAllErrors().toString());

            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField,FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("error",errors);

            return "redirect:http://auth.gulimall.com/reg.html";
        }

        //校验验证码
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){

                //远程调用

                //删除验证码
                stringRedisTemplate.delete(AuthServiceConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
//                System.out.println(vo);
                R r = memberFeignService.regist(vo);

                if(r.getCode()==0){

                    return "redirect:http://auth.gulimall.com/login.html";
                }else {

                    Map<String,String> errors = new HashMap<>();
                    errors.put("msg",r.getData(new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("error","");
                    return "redirect:http://auth.gulimall.com/reg.html";
                }

            }else{
                Map<String,String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("error",errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }else {
            Map<String,String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("error",errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServiceConstant.SESSION_LOGIN_KEY);
        if(attribute==null){
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes,
                        HttpSession session){

        System.out.println(vo);
        R login = memberFeignService.login(vo);

        if(login.getCode()==0){

            MemeberRespVo data = login.getData("data", new TypeReference<MemeberRespVo>() {
            });
            System.out.println(data);
            System.out.println("///////////////////////////////////////////////////////");
            if(data==null){
                return "redirect:http://auth.gulimall.com/login.html";
            }
            session.setAttribute(AuthServiceConstant.SESSION_LOGIN_KEY,data);
            return "redirect:http://gulimall.com";
        }else {
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            System.out.println(errors);
            System.out.println("?????????????????????????????????????????????????????????");
            redirectAttributes.addFlashAttribute("error",errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
