package com.atguigu.tingshu.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private UserInfoVo userInfoVo;
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo, Long userId) {
        //获取用户信息
        Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(userId);
        Assert.notNull(userInfoVoResult, "用户信息不存在");
        UserInfoVo userInfoVo = userInfoVoResult.getData();
        Assert.notNull(userInfoVo, "用户信息不存在");
        //  订单原始金额
        BigDecimal originalAmount = new BigDecimal("0.00");
        //  减免总金额
        BigDecimal derateAmount = new BigDecimal("0.00");
        //  订单总价
        BigDecimal orderAmount = new BigDecimal("0.00");
        //  订单明细集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //  订单减免明细列表
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //  1001 专辑
        if ("1001".equals(tradeVo.getItemType())) {
            //  判断用户是否购买过专辑
            Result<Boolean> isPaidAlbumResult = this.userInfoFeignClient.isPaidAlbum(tradeVo.getItemId());
            Assert.notNull(isPaidAlbumResult);
            Boolean isPaidAlbum = isPaidAlbumResult.getData();
            Assert.notNull(isPaidAlbum);
            if (isPaidAlbum) {
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            //判断用户是否购买过专辑 24.11.15
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(tradeVo.getItemId());
            Assert.notNull(albumInfoResult, "返回专辑结果集不能为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑对象不能为空");
            //  判断当前用户是否是vip
            if (userInfoVo.getIsVip().intValue() == 0) {
                //  非VIP 用户
                originalAmount = albumInfo.getPrice();

                //  判断是否打折 , 不等于-1 就是打折
                if (albumInfo.getDiscount().intValue() != -1) {
                    //100打8折，优惠20
                    // 原价*（10-折扣） / 10
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
                //最终价格
                orderAmount = originalAmount.subtract(derateAmount);

            } else if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().before(new Date())) {
                // 2024-10-01   2024-11-13  不是vip
                //原始价格
                originalAmount = albumInfo.getPrice();
                //优惠价格 derateAmount  -1表示没有折扣
                if (albumInfo.getVipDiscount().intValue() != -1) {
                    //100打8折，优惠20
                    // 原价*（10-折扣） / 10
                    derateAmount = originalAmount.multiply(new BigDecimal("10").subtract(albumInfo.getDiscount())).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                }
                //最终价格
                orderAmount = originalAmount.subtract(derateAmount);

            }
            //  订单明细集合 List<OrderDetailVo> orderDetailVoList
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(albumInfo.getPrice());
            orderDetailVoList.add(orderDetailVo);

            //订单减免明细列表  List<OrderDerateVo>  orderDerateVoList
            // 原始价格 - 最终价格 != 0 有优惠
            if (originalAmount.subtract(orderAmount).doubleValue() != 0) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(originalAmount.subtract(orderAmount));
                orderDerateVoList.add(orderDerateVo);
            }
            } else if ("1002".equals(tradeVo.getItemType())) { //TODO 5 购买声音
            }

                //6 把这些数据最终封装到 OrderInfoVo
                OrderInfoVo orderInfoVo = new OrderInfoVo();
                //购买类型 ：vip 专辑  声音
                orderInfoVo.setItemType(tradeVo.getItemType());
                //原始价格
                orderInfoVo.setOriginalAmount(originalAmount);
                //优惠价格
                orderInfoVo.setDerateAmount(derateAmount);
                //最终价格
                orderInfoVo.setOrderAmount(orderAmount);
                //订单明细
                orderInfoVo.setOrderDetailVoList(orderDetailVoList);
                //优惠明细
                orderInfoVo.setOrderDerateVoList(orderDerateVoList);

                //7 防止重复提交
                //防止重复提交，添加数据到redis里面
                String tradeNoKey = "user:trade:" + userId;
                String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
                redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
                orderInfoVo.setTradeNo(tradeNo);
                //8 数据安全校验
                //  生成签名
                Map<String, Object> parameterMap = JSON.parseObject(JSON.toJSONString(orderInfoVo), Map.class);
                String sign = SignHelper.getSign(parameterMap);
                orderInfoVo.setSign(sign);


                return orderInfoVo;



        }
    }
