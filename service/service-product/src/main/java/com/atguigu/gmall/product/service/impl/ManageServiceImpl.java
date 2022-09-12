package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Override
    public List<BaseCategory1> getCategory1() {
        return category1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        QueryWrapper<BaseCategory2> wrapper = new QueryWrapper<>();
        wrapper.eq("category1_id", category1Id);
        return category2Mapper.selectList(wrapper);
    }

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

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        QueryWrapper<BaseAttrValue> valueWrapper = new QueryWrapper<>();
        valueWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(valueWrapper);
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            List<BaseAttrValue> valueList = getAttrValueList(attrId);
            baseAttrInfo.setAttrValueList(valueList);
        }
        return baseAttrInfo;
    }
}
