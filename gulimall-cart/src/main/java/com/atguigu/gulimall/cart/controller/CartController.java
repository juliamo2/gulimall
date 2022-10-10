package com.atguigu.gulimall.cart.controller;

import com.atguigu.common.constant.AuthServiceConstant;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.to.UserInfoTo;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {

        CartVo cart = cartService.getCart();

        model.addAttribute("cart",cart);

        return "cartList";
    }

    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            Model model) throws ExecutionException, InterruptedException {

        cartService.addToCart(skuId,num);

        model.addAttribute("skuId",skuId);

        return "redirect:http://cart.gulimall.com/addToCartSuessful.html?skuId="+skuId;
    }

    @GetMapping("/addToCartSuessful.html")
    public String addToCartSussessful(@RequestParam("skuId") Long skuId,Model model){

        CartItemVo item = cartService.getVartItem(skuId);

        model.addAttribute("item",item);

        return "success";
    }
}
