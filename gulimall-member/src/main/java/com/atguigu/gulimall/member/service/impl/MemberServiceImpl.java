package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.atguigu.gulimall.member.vo.IdVo;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberUserRegisterVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.apache.catalina.User;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberUserRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        MemberDao memberDao = this.baseMapper;

        //设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        //检查唯一性
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());

        //加密 加盐hash
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassWord());
        memberEntity.setPassword(encode);


        memberDao.insert(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String Phone) throws PhoneExistException{
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", Phone));
        if(mobile>0){
            throw new PhoneExistException();
        }
    }

    @Override
    public void checkUsernameUnique(String Username) throws UsernameExistException{
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount((new QueryWrapper<MemberEntity>().eq("username", Username)));
        if(count>0){
            throw new UsernameExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAccount = vo.getLoginAccount();
        String passWord = vo.getPassWord();

        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginAccount).or().
                eq("mobile", loginAccount));

        System.out.println("mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm");
        System.out.println(memberEntity);
        if(memberEntity==null){
            return null;
        }else {
            String password = memberEntity.getPassword();
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            boolean matches = bCryptPasswordEncoder.matches(passWord, password);
            if(matches){
                return memberEntity;
            }else {
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //登录注册
        Map<String,String> header = new HashMap<>();
        Map<String,String> query = new HashMap<>();
        query.put("access_token","9d2af8d9e32ff3018a66329e46f57c1b");

        String access_token = socialUser.getAccess_token();
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", header, query);


        String json = EntityUtils.toString(response.getEntity());
        System.out.println(json);
        System.out.println(json.split(":")[1].split(",")[0]);
        String uid = json.split(":")[1].split(",")[0];
        String name = json.split(":")[2].split(",")[0].split("\"")[1];

        MemberEntity regist = new MemberEntity();

        MemberEntity memberEntities = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntities!=null){
            //更换令牌
            MemberEntity entity = new MemberEntity();
//            entity.setSocialUid(memberEntities.getId());
            entity.setAccessToken(socialUser.getAccess_token());
            entity.setExpiresIn(socialUser.getExpires_in());

            this.baseMapper.updateById(entity);

            memberEntities.setExpiresIn(socialUser.getExpires_in());
            System.out.println(memberEntities);
            System.out.println("///////////////////////////////////////////////////////////");
            return memberEntities;
        }else {
            //注册

            regist.setSocialUid(uid);
            regist.setGender(1);
            regist.setNickname(name);
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            this.baseMapper.insert(regist);
            System.out.println(regist);
            System.out.println("?????????????????????????????????????????????????????");
            return regist;
        }
    }

}