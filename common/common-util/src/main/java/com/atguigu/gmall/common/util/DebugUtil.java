package com.atguigu.gmall.common.util;

import java.util.List;

public class DebugUtil<T> {
    public static <T> void printOneLayerList(List<T> list) {
        System.out.println(">>>>>>>>>printOneLayerList>>>>>>>>>>");
        list.forEach(System.out::println);
        System.out.println("<<<<<<<<<printOneLayerList<<<<<<<<<<");
    }
}
