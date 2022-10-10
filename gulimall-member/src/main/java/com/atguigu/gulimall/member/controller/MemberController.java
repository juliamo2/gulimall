package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberUserRegisterVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * 会员
 *
 * @author leifengyang
 * @email leifengyang@gmail.com
 * @date 2019-10-08 09:47:05
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");

        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member",memberEntity).put("coupons",membercoupons.get("coupons"));
    }

    @PostMapping("/regist")
    public R regist(@RequestBody MemberUserRegisterVo vo){


        try{
            memberService.regist(vo);
        }catch (PhoneExistException e){
            return R.error(BizCodeEnume.PHONE_EXIST_EXCETION.getCode(), BizCodeEnume.PHONE_EXIST_EXCETION.getMsg());
        }catch (UsernameExistException e){
            return R.error(BizCodeEnume.UER_EXIST_EXCEPTON.getCode(),BizCodeEnume.UER_EXIST_EXCEPTON.getMsg());
        }

        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){

        try {
            System.out.println(vo);
            MemberEntity memberEntity = memberService.login(vo);
            if(memberEntity!=null){
                return R.ok().setData(memberEntity);
            }else {
                R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_EXCEPTION.getCode(),BizCodeEnume.LOGINACCOUNT_PASSWORD_EXCEPTION.getMsg());
            }
        }catch (Exception e){
            return R.error();
        }
        return R.ok();
    }

    @PostMapping("/oauth2/login")
    public R oauthlogin(@RequestBody SocialUser socialUser){

        try {
            MemberEntity memberEntity = memberService.login(socialUser);
            if(memberEntity!=null){
                return R.ok().setData(memberEntity);
            }else {
                R.error(BizCodeEnume.LOGINACCOUNT_PASSWORD_EXCEPTION.getCode(),BizCodeEnume.LOGINACCOUNT_PASSWORD_EXCEPTION.getMsg());
            }
        }catch (Exception e){
            return R.error();
        }
        return R.ok();
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
