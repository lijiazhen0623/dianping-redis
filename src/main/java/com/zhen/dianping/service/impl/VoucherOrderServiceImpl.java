package com.zhen.dianping.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhen.dianping.dto.Result;
import com.zhen.dianping.entity.SeckillVoucher;
import com.zhen.dianping.entity.VoucherOrder;
import com.zhen.dianping.mapper.VoucherOrderMapper;
import com.zhen.dianping.service.ISeckillVoucherService;
import com.zhen.dianping.service.IVoucherOrderService;
import com.zhen.dianping.utils.RedisIdWorker;
import com.zhen.dianping.utils.RedisLock;
import com.zhen.dianping.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1、查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2、判断秒杀是否开始(结束)
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动还没有开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动结束了");
        }
        //3、判断库存是否充足
        Integer stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
//        synchronized (userId.toString().intern())
        RedisLock redisLock = new RedisLock("order:" + userId, stringRedisTemplate);
        //获取锁
        boolean lock = redisLock.tryLock(30L);
        if (!lock) {
            return Result.fail("已获取过该优惠券");
        }
        try {
            //用代理对象调用，@Transactional才能发挥作用
            IVoucherOrderService currentProxy = (IVoucherOrderService) AopContext.currentProxy();
            return currentProxy.createVoucherOrder(voucherId);
        } catch (Exception e) {
            System.out.println(e);
            return Result.fail("获取失败");
        } finally {
            //释放锁
            redisLock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //实现一人一单
        //根据用户id和优惠券id查找优惠券订单是否存在
        Long id = UserHolder.getUser().getId();
        int count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("不可重复获取优惠券");
        }
        //4、扣减库存
        boolean success = seckillVoucherService
                .update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                //乐观锁
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }
        //5、创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //5.1 订单号id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //5.2 用户id
        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        //5.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //6、返回订单号
        return Result.ok(orderId);
    }
}
