package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageAsyncService;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCategory1Mapper category1Mapper;

    @Autowired
    private BaseCategory2Mapper category2Mapper;

    @Autowired
    private BaseCategory3Mapper category3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ManageAsyncService manageAsyncService;

    @Autowired
    private RabbitService rabbitService;

    @GmallCache(prefix = "category1:")
    @Override
    public List<BaseCategory1> getCategory1() {
        return category1Mapper.selectList(null);
    }

    @GmallCache(prefix = "category2:")
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);
        return category2Mapper.selectList(wrapper);
    }

    @GmallCache(prefix = "category3:")
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        QueryWrapper<BaseCategory3> wrapper = new QueryWrapper<>();
        wrapper.eq("category2_id", category2Id);
        return category3Mapper.selectList(wrapper);
    }

    /*
      根据分类Id 获取平台属性数据
      接口说明：
      1，平台属性可以挂在一级分类、二级分类和三级分类
      2，查询一级分类下面的平台属性，传：category1Id，0，0；   取出该分类的平台属性
      3，查询二级分类下面的平台属性，传：category1Id，category2Id，0；
      取出对应一级分类下面的平台属性与二级分类对应的平台属性
      4，查询三级分类下面的平台属性，传：category1Id，category2Id，category3Id；
      取出对应一级分类、二级分类与三级分类对应的平台属性
     */
    @GmallCache(prefix = "attrInfoList:")
    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

    /*
     * 更新数据要判断是新增的还是修改
     * 通过id属性判断
     * 对Info的修改同时涉及到value表，先将value表先前数据全部删除，再重新添加完成更新
     * */
    @Override
    @Transactional//涉及多步操作，加上事务
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId() != null) baseAttrInfoMapper.updateById(baseAttrInfo);
        else baseAttrInfoMapper.insert(baseAttrInfo);
        QueryWrapper<BaseAttrValue> valueWrapper = new QueryWrapper<>();
        valueWrapper.eq("attr_id", baseAttrInfo.getId());
        baseAttrValueMapper.delete(valueWrapper);
        List<BaseAttrValue> valueList = baseAttrInfo.getAttrValueList();
        if (valueList != null && valueList.size() > 0) {
            for (BaseAttrValue value : valueList) {
                baseAttrValueMapper.insert(value);
            }
        }
    }

    @GmallCache(prefix = "attrValueList:")
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> valueWrapper = new QueryWrapper<>();
        valueWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(valueWrapper);
    }

    @GmallCache(prefix = "attrInfo")
    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            List<BaseAttrValue> valueList = getAttrValueList(attrId);
            baseAttrInfo.setAttrValueList(valueList);
        }
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {
        QueryWrapper<SpuInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id", spuInfo.getCategory3Id());
        wrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(pageParam, wrapper);
    }

    @GmallCache(prefix = "baseSaleAttrList:")
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional//同时添加所有对应的SpuSaleAttr和SpuImage
    public void saveSpuInfo(SpuInfo spuInfo) {
        spuInfoMapper.insert(spuInfo);//插入后会把新的id回存入spuInfo对象中
        List<SpuImage> images = spuInfo.getSpuImageList();
        if (images != null && images.size() > 0) {
            for (SpuImage image : images) {
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insert(image);
            }
        }
        List<SpuSaleAttr> saleAttrs = spuInfo.getSpuSaleAttrList();
        if (saleAttrs != null && saleAttrs.size() > 0) {
            for (SpuSaleAttr saleAttr : saleAttrs) {
                saleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(saleAttr);
                List<SpuSaleAttrValue> attrValues = saleAttr.getSpuSaleAttrValueList();
                if (attrValues != null && attrValues.size() > 0) {
                    for (SpuSaleAttrValue attrValue : attrValues) {
                        attrValue.setSpuId(spuInfo.getId());
                        attrValue.setBaseSaleAttrId(saleAttr.getId());
                        spuSaleAttrValueMapper.insert(attrValue);
                    }
                }
            }
        }
    }

    @GmallCache(prefix = "spuImageList")
    @Override
    public List<SpuImage> spuImageList(Long spuId) {
        QueryWrapper<SpuImage> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id", spuId);
        return spuImageMapper.selectList(wrapper);
    }

    /*
     * 每个attr中包含了attrValue集合，需要进行联查
     * */
    @GmallCache(prefix = "spuSaleAttrList:")
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {
        return spuSaleAttrMapper.selectSupSaleAttrList(spuId);
    }

    /*
     * skuInfo中包含image,attr,attrValue
     * 三个集合，应该分别再进行插入
     * */
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        skuInfoMapper.insert(skuInfo);
        List<SkuImage> images = skuInfo.getSkuImageList();
        if (images != null && images.size() > 0) {
            for (SkuImage image : images) {
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insert(image);
            }
        }
        List<SkuAttrValue> attrValues = skuInfo.getSkuAttrValueList();
        if (attrValues != null && attrValues.size() > 0) {
            for (SkuAttrValue attrValue : attrValues) {
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(attrValue);
            }
        }
        List<SkuSaleAttrValue> values = skuInfo.getSkuSaleAttrValueList();
        if (values != null && values.size() > 0) {
            for (SkuSaleAttrValue value : values) {
                value.setSkuId(skuInfo.getId());
                value.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(value);
            }
        }
    }

    @Override
    public IPage<SkuInfo> getPage(Page<SkuInfo> page) {
        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(page, wrapper);
    }

    @Override//异步发送消息到service-list接收
    public void onSale(Long skuId) {
        manageAsyncService.onSaleAsync(skuId);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_UPPER, skuId);
    }

    @Override//异步发送消息到service-list接收
    public void cancelSale(Long skuId) {
        manageAsyncService.cancelSaleAsync(skuId);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS, MqConst.ROUTING_GOODS_LOWER, skuId);
    }

    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX)
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return getSkuInfoDB(skuId);
    }

    //集群redis，使用redisson实现缓存，redisson只做了提供lock
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo;
        try {
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {
                String skuLock = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(skuLock);
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (res) {//加锁成功
                    try {//这个try用于解锁
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo == null) {//数据库中不存在查找数据
                            SkuInfo skuInfo1 = new SkuInfo();
                            redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        lock.unlock();
                    }
                } else {//加锁失败，0.1秒后重试
                    Thread.sleep(100);
                    return getSkuInfo(skuId);
                }
            }
            return skuInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }//缓存宕机,兜底
        return getSkuInfoDB(skuId);
    }


    //单体redis锁,使用lua脚本解锁
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo;
        try {
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            if (skuInfo == null) {//缓存中不存在，从数据库获取,加锁，防止击穿
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                String uuid = UUID.randomUUID().toString().replace("-", "");
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (isExist != null && isExist) {//锁添加成功
                    System.out.println("获取到分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {//如果从数据库里获取的数据是空的,往缓存中加入空对象！！注意！！必须得重新new，否则存入的是null，再次访问还是判断成缓存不存在
                        SkuInfo skuInfo1 = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey, skuInfo1, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        return skuInfo1;
                    }
                    redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setScriptText(script);
                    redisScript.setResultType(Long.class);
                    redisTemplate.execute(redisScript, Collections.singletonList(lockKey), uuid);
                    return skuInfo;
                } else {//锁获取失败，0.1秒后重试
                    Thread.sleep(100);
                    return getSkuInfo(skuId);
                }
            }
            return skuInfo;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }//防止缓存宕机
        return getSkuInfoDB(skuId);
    }

    //根据skuId查询数据库
    private SkuInfo getSkuInfoDB(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            QueryWrapper<SkuImage> skuImageWrapper = new QueryWrapper<>();
            skuImageWrapper.eq("sku_id", skuId);
            List<SkuImage> skuImages = skuImageMapper.selectList(skuImageWrapper);
            skuInfo.setSkuImageList(skuImages);
        }
        return skuInfo;
    }

    @GmallCache(prefix = "categoryViewByCategory3Id:")
    @Override
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @GmallCache(prefix = "skuPrice:")
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) return skuInfo.getPrice();
        return new BigDecimal(0);
    }

    @GmallCache(prefix = "getSpuSaleAttrListCheckBySku:")
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId,
                                                          @Param("spuId") Long spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    @GmallCache(prefix = "skuValueIdsMap:")
    @Override
    public Map<Object, Object> getSkuValueIdsMap(Long spuId) {
        List<Map<Object, Object>> mapList = skuSaleAttrValueMapper.selectSaleAttrValueBySpu(spuId);
        Map<Object, Object> map = null;
        if (mapList != null && mapList.size() > 0) {
            map = new HashMap<>();
            for (Map<Object, Object> stringMap : mapList) {
                map.put(stringMap.get("value_ids"), stringMap.get("sku_id"));
            }
        }
        return map;
    }

    @GmallCache(prefix = "category:")
    @Override//获取全部分类信息,组织成要求的JSON对象返回，对象具有多层结构
    public List<JSONObject> getBaseCategoryList() {
        //返回的JSON集合
        List<JSONObject> out = new ArrayList<>();
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        //将查询到的所有分类按照一级分类id分组 key:category1Id, value:baseCategoryView
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = category1Map.entrySet().iterator();
        int index = 1;
        while (iterator1.hasNext()) {
            Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
            Long category1Id = entry1.getKey();
            List<BaseCategoryView> category2List = entry1.getValue();
            //获取一级分类名称
            String category1Name = category2List.get(0).getCategory1Name();
            JSONObject category1 = new JSONObject();
            category1.put("index", index);
            category1.put("categoryId", category1Id);
            category1.put("categoryName", category1Name);
            //对二级分类操作类似，以二级分类id分组
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator2 = category2Map.entrySet().iterator();
            List<JSONObject> category2Children = new ArrayList<>();//二级分类
            while (iterator2.hasNext()) {//key:category2Id value:baseCategoryView
                Map.Entry<Long, List<BaseCategoryView>> entry2 = iterator2.next();
                Long category2Id = entry2.getKey();
                List<BaseCategoryView> category3List = entry2.getValue();
                String category2Name = category3List.get(0).getCategory2Name();
                JSONObject category2 = new JSONObject();
                category2.put("categoryName", category2Name);
                category2.put("categoryId", category2Id);
                List<JSONObject> category3Children = new ArrayList<>();
                //处理三级分类,同一二级
                Map<Long, List<BaseCategoryView>> category3Map = category3List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                for (Map.Entry<Long, List<BaseCategoryView>> entry3 : category3Map.entrySet()) {
                    JSONObject category3 = new JSONObject();
                    Long category3Id = entry3.getKey();
                    List<BaseCategoryView> category3s = entry3.getValue();
                    String category3Name = category3s.get(0).getCategory3Name();
                    category3.put("categoryName", category3Name);
                    category3.put("categoryId", category3Id);
                    category3Children.add(category3);
                }
                category2.put("categoryChild", category3Children);
                category2Children.add(category2);
            }
            category1.put("categoryChild", category2Children);
            out.add(category1);
            index++;
        }
        return out;
    }

    @GmallCache(prefix = "baseTradeMark:")
    @Override//根据商标id查询商标
    public BaseTrademark getTradeMarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    @GmallCache(prefix = "attrListBySkuId:")
    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.getAttrListBySkuId(skuId);
    }


}
