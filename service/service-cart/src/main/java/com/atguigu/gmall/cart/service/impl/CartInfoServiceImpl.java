package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    //TODO 存在事务问题
    @Override//添加到购物车
    @Transactional(rollbackFor = Exception.class)
    //”在@Transactional注解的服务方法会产生一个新的线程的情况下，事务不会从调用者线程传播到新建线程“
    //所以这里的Transactional注解是无效的
    public void addToCart(Long skuId, Long userId, Integer skuNum) {
        String cartKey = getCartKey(userId);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(cartKey)))//如果缓存中没有记录的话，先将MySQL旧数据全部更新到缓存，保持缓存和MySQL的一致性
            loadCartInfosCache(cartKey, getCartInfosFromMySql(userId.toString()));
        CartInfo cartInfo = (CartInfo) redisTemplate.boundHashOps(cartKey).get(skuId.toString());//因为事先会更新缓存，所以直接从缓存中取
        CartInfo originCartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());//opsForHash与boundHashOps作用相同
        Future<CartInfo> asyncRes;
        try {
            if (cartInfo == null) {
                SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                cartInfo = new CartInfo();
                cartInfo.setSkuId(skuId);
                cartInfo.setUserId(String.valueOf(userId));
                cartInfo.setSkuNum(skuNum);
                cartInfo.setSkuName(skuInfo.getSkuName());
                cartInfo.setCartPrice(skuInfo.getPrice());
                cartInfo.setSkuPrice(skuInfo.getPrice());
                cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
                asyncRes = cartAsyncService.saveCartInfo(cartInfo);
            } else {
                cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));
                asyncRes = cartAsyncService.updateCartInfo(cartInfo);
            }
            //更新后将cart加入缓存，优化查询
            //redis数据结构 每个user对应一个hashMap，key为skuId，value为cartInfo
            redisTemplate.opsForHash().put(cartKey, skuId.toString(), asyncRes.get());
            System.out.println("进入缓存！！");
            //为购物车设置过期时间
            setCartKeyExpire(cartKey);
            //asyncRes.get();
        } catch (Exception e) {//如果异步操作MySQL出现异常，则复原redis
            e.printStackTrace();
            System.out.println("复原！！");//恢复时异步操作
            cartAsyncService.resetCache(redisTemplate, cartKey, skuId.toString(), originCartInfo);
            throw new RuntimeException("添加购物车失败！");
        }
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (!StringUtils.isEmpty(userId)) { //查询登录的购物车,可能发生合并购物车
            List<CartInfo> tempCartList = getCartList(userTempId);
            cartInfoList = getCartList(userId);
            if (!CollectionUtils.isEmpty(tempCartList)) {//临时购物车非空，进行合并
                mergeCart(tempCartList, cartInfoList, userId);
                deleteCartList(userTempId);
            }
        } else if (!StringUtils.isEmpty(userTempId)) { //查询临时购物车
            cartInfoList = getCartList(userTempId);
        }
        if (!CollectionUtils.isEmpty(cartInfoList))//按时间排序后返回
            cartInfoList.sort((a, b) -> DateUtil.truncatedCompareTo(b.getUpdateTime(), a.getUpdateTime(), Calendar.SECOND));
        return cartInfoList;
    }

    //TODO 未考虑事务
    @Override//异步修改数据库，同步修改缓存
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        cartAsyncService.checkCart(userId, isChecked, skuId);
        String cartKey = getCartKey(Long.parseLong(userId));
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfo != null) {
            cartInfo.setIsChecked(isChecked);
            redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
        }
    }

    //TODO 未考虑事务
    @Override//类似checkCart，异步修改数据库，同步修改缓存
    public void deleteCart(Long skuId, String userId) {
        cartAsyncService.delCartInfoMySql(skuId, userId);
        String cartKey = getCartKey(Long.parseLong(userId));
        CartInfo cartInfo = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfo != null) {
            redisTemplate.opsForHash().delete(cartKey, skuId.toString());
            setCartKeyExpire(cartKey);
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartList = getCartList(userId);
        return cartList.stream().filter(cartInfo -> cartInfo.getIsChecked() != 0).collect(Collectors.toList());
    }

    //根据userId删除redis和MySQL,均采用异步进行
    //TODO 存在事务问题
    private void deleteCartList(String userId) {
        if (!StringUtils.isEmpty(userId)) {
            cartAsyncService.delCartMysql(userId);
            cartAsyncService.delCartRedis(redisTemplate, getCartKey(Long.parseLong(userId)));
        }
    }

    //合并购物车,对于temp中有而user中没有的，做insert，反之update
    //TODO 存在异步事务问题
    private void mergeCart(List<CartInfo> tempCartList, List<CartInfo> userCartList, String userId) {
        Map<String, CartInfo> map = new HashMap<>();
        userCartList.forEach(cartInfo -> map.put(cartInfo.getSkuId().toString(), cartInfo));
        tempCartList.forEach(tempCartInfo -> {
            CartInfo mapValue = map.getOrDefault(tempCartInfo.getSkuId().toString(), null);
            tempCartInfo.setUserId(userId);
            try {
                if (mapValue != null) {//异步更新各个数据库字段
                    mapValue.setSkuNum(mapValue.getSkuNum() + tempCartInfo.getSkuNum());
                    if (tempCartInfo.getIsChecked() == 1) mapValue.setIsChecked(1);
                    mapValue.setUpdateTime(new Timestamp(new Date().getTime()));//手动更新时间
                    cartAsyncService.updateCartInfo(mapValue);
                } else {
                    Future<CartInfo> future = cartAsyncService.saveCartInfo(tempCartInfo);
                    CartInfo addCartInfo = future.get();
                    map.put(tempCartInfo.getSkuId().toString(), addCartInfo);//等添加完再放到map,否则没有id
                    userCartList.add(addCartInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("合并购物车失败！");
            }
        });
        redisTemplate.opsForHash().putAll(getCartKey(Long.parseLong(userId)), map);
    }


    //先从缓存中获取，如果缓存为空则尝试从mysql获取，获取成功再放入缓存
    private List<CartInfo> getCartList(String userId) {
        if (StringUtils.isEmpty(userId)) return new ArrayList<>();
        String cartKey = getCartKey(Long.parseLong(userId));
        List<Object> values = redisTemplate.opsForHash().values(cartKey);
        List<CartInfo> cartInfos = new ArrayList<>(values.size());
        for (Object value : values) {
            cartInfos.add((CartInfo) value);
        }
        if (CollectionUtils.isEmpty(cartInfos)) {
            cartInfos = getCartInfosFromMySql(userId);
            if (!CollectionUtils.isEmpty(cartInfos))
                loadCartInfosCache(cartKey, cartInfos);
        }
        return cartInfos;
    }

    //从mysql中获取cartList
    private List<CartInfo> getCartInfosFromMySql(String userId) {
        List<CartInfo> cartInfos;
        QueryWrapper<CartInfo> cartInfoWrapper = new QueryWrapper<>();
        cartInfoWrapper.eq("user_id", userId);
        cartInfos = cartInfoMapper.selectList(cartInfoWrapper);
        return cartInfos;
    }


    @Override//对外开放的更新缓存中的最新价格，返回更新后的cartInfoList
    public void loadCartInfoCache(String userId) {
        List<CartInfo> cartInfoList = getCartInfosFromMySql(userId);
        String cartKey = getCartKey(Long.parseLong(userId));
        loadCartInfosCache(cartKey,cartInfoList);
    }

    //批量将数据库的购物车数据同步到缓存，同时获取最新的价格
    private void loadCartInfosCache(String cartKey, List<CartInfo> cartInfos) {
        if (!CollectionUtils.isEmpty(cartInfos)) {
            ConcurrentMap<String, CartInfo> map = new ConcurrentHashMap<>();
            ExecutorService executorService = Executors.newFixedThreadPool(cartInfos.size());
            cartInfos.forEach(cartInfo -> executorService.execute(() -> {
                BigDecimal latestPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
                cartInfo.setSkuPrice(latestPrice);
                System.out.println("SkuId = " + cartInfo.getSkuId() + "\nPrice=" + cartInfo.getSkuPrice());
                map.put(cartInfo.getSkuId().toString(), cartInfo);
            }));//异步进行远程调用
            executorService.shutdown();//空闲线程停止接收task,等待正在运行的所有task完成，完成一个关闭一条线程
            //noinspection StatementWithEmptyBody
            while (!executorService.isTerminated());//确认所有线程结束
            redisTemplate.opsForHash().putAll(cartKey, map);
            setCartKeyExpire(cartKey);
        }
    }

    //设置指定购物车过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    //获取用户对应购物车缓存
    private String getCartKey(Long userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}


    /*@Test
    public void x() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        list.add(7);
        list.add(8);
        list.add(9);
        list.add(10);
        list.add(11);
        list.add(12);
        list.add(13);
        list.add(14);
        list.add(15);
        list.add(16);
        list.add(17);
        list.add(18);
        list.add(19);
        list.add(20);
        list.forEach(i -> executorService.execute(() -> {
            System.out.println("i = " + i);
            try {
                Thread.sleep((long) (Math.random() * 500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        executorService.shutdown();
        while (true) {
            if (executorService.isTerminated()) break;
        }
        System.out.println("Done!");
    }*/