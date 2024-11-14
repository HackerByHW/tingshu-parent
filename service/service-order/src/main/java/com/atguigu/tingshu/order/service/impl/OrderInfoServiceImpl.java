package com.atguigu.tingshu.order.service.impl;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        List<OrderDetailVo> orderDerateVoList = new ArrayList<>();
        //  1001 专辑
        if ("1001".equals(tradeVo.getItemType())){
            //  判断用户是否购买过专辑
            Result<Boolean> isPaidAlbumResult = this.userInfoFeignClient.isPaidAlbum(tradeVo.getItemId());
            Assert.notNull(isPaidAlbumResult);
            Boolean isPaidAlbum = isPaidAlbumResult.getData();
            Assert.notNull(isPaidAlbum);
            if (isPaidAlbum){
                throw new GuiguException(ResultCodeEnum.REPEAT_BUY_ERROR);
            }
            //
        }
        return null;
    }
}
