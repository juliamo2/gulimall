package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.FeignProductService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.to.UserInfoTo;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    FeignProductService feignProductService;

    @Autowired
    ThreadPoolExecutor executor;
    private final String CART_PREFIX = "gulimall-cart";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String result = (String) cartOps.get(skuId.toString());


        //添加新商品



        if (StringUtils.isEmpty(result)){
            CartItemVo cartItemVo = new CartItemVo();
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //1、远程查询添加的商品信息

                R info = feignProductService.info(skuId);

                SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                // 商品添加到购物车
                cartItemVo.setCheck(true);
                cartItemVo.setCount(num);
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setSkuId(skuId);
                cartItemVo.setPrice(skuInfo.getPrice());
            },executor);

            //组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> values = feignProductService.getSkuSaleValues(skuId);
                cartItemVo.setSkuAttrValues(values);
            }, executor);

            //.get() 阻塞等待
            CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();

            String s = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(),s);
            return cartItemVo;
        }
        else {
            //购物车有这个商品,修改数量

            CartItemVo cartItemVo = JSON.parseObject(result, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount()+num);

            //放入缓存
            cartOps.put(skuId.toString(),JSON.toJSONString(cartItemVo));

            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getVartItem(Long skuId) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String result = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(result, CartItemVo.class);
        return  cartItemVo;
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {

        CartVo cart = new CartVo();

        //区分登录状态
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId()!=null){
            //登陆
            String cartkey = CART_PREFIX + userInfoTo.getUserId();

            List<CartItemVo> tempCartItems = getTempCartItem(CART_PREFIX + userInfoTo.getUserKey());
            if (tempCartItems!=null){
                //临时购物车有数据
                //合并购物车
                for (CartItemVo itemVo:tempCartItems){
                    addToCart(itemVo.getSkuId(),itemVo.getCount());
                }
                //清除临时购物车
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }

            //获得登录的购物车（合并后的）
            List<CartItemVo> cartItem = getTempCartItem(cartkey);
            cart.setItems(cartItem);

            return cart;


        }else {
            //未登录
            String cartkey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> tempCartItem = getTempCartItem(cartkey);
            cart.setItems(tempCartItem);
        }
        return cart;
    }

    //获取到需要操作的购物车
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //购物车是离线的还是非离线的
        //1、
        String cartkey = "";
        if(userInfoTo.getUserId()!=null){
            //用户已登录
            cartkey = CART_PREFIX+userInfoTo.getUserId();
        }else {
            //未登录
            cartkey = CART_PREFIX+userInfoTo.getUserKey();
        }

//        redisTemplate.opsForHash().get(cartkey,"1");

        return redisTemplate.boundHashOps(cartkey);
    }

    private List<CartItemVo> getTempCartItem(String cartkey){

        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartkey);
        List<Object> values = hashOps.values();
        if(values!=null &&values.size()>0){
            List<CartItemVo> collect = values.stream().map((obj) -> {

                String str = (String) obj;
                CartItemVo cartItemVo = JSON.parseObject(str, CartItemVo.class);

                return cartItemVo;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public void clearCart(String cartkey) {

        redisTemplate.delete(cartkey);
    }
}
