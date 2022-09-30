package com.atguigu.gmall.common.handler;

import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Date;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        updateTimeField(metaObject, "createTime");
        updateTimeField(metaObject, "updateTime");
        updateTimeField(metaObject, "operateTime");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        updateTimeField(metaObject, "updateTime");
        updateTimeField(metaObject, "operateTime");
    }

    //自动填充时间字段，根据Date和TimeStamp的类型选择
    private void updateTimeField(MetaObject metaObject, String fieldName) {
        if (metaObject.hasSetter(fieldName))
            setFieldValByName(fieldName, new Timestamp(new Date().getTime()), metaObject);
    }

//    @Test
//    public void x() {
//        Date date = new Timestamp(new Date().getTime());
//        System.out.println(date);
//    }
}
