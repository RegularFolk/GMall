package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override//商品上架，就是把数据库中的数据存入es中,从数据库中查询出多个数据，打包成good存入es
    public void upperGoods(Long skuId) {//优化成并行实现
        Goods goods = new Goods();
        CompletableFuture<SkuInfo> skuInfoCF = CompletableFuture.supplyAsync(() -> productFeignClient.getSkuInfo(skuId));
        CompletableFuture<Void> trademarkCF = skuInfoCF.thenAcceptAsync((skuInfo) -> {
            packSkuInfo(skuId, goods, skuInfo);
            BaseTrademark trademark = productFeignClient.getTradeMark(skuInfo.getTmId()).getData();
            packTrademark(goods, trademark);
        }, threadPoolExecutor);
        CompletableFuture<Void> categoryCF = skuInfoCF.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            packCategory(goods, categoryView);
        }, threadPoolExecutor);
        CompletableFuture<Void> attrCF = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId).getData();
            packAttrs(goods, attrList);
        }, threadPoolExecutor);
        CompletableFuture.allOf(
                skuInfoCF,
                trademarkCF,
                categoryCF,
                attrCF).join();
        goodsRepository.save(goods);
    }

    private void packAttrs(Goods goods, List<BaseAttrInfo> attrList) {
        List<SearchAttr> baseAttrList = attrList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());
        goods.setAttrs(baseAttrList);
    }

    private void packCategory(Goods goods, BaseCategoryView categoryView) {
        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Id(categoryView.getCategory3Id());
        goods.setCategory3Name(categoryView.getCategory3Name());
    }

    private void packTrademark(Goods goods, BaseTrademark tradeMark) {
        goods.setTmId(tradeMark.getId());
        goods.setTmName(tradeMark.getTmName());
        goods.setTmLogoUrl(tradeMark.getLogoUrl());
    }

    private void packSkuInfo(Long skuId, Goods goods, SkuInfo skuInfo) {
        goods.setId(skuId);
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setCreateTime(new Date());
    }

    @Override//商品下架就是把es中的指定商品删除
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    @Override//将热度信息缓存以避免大量的磁盘IO
    public void incrHotScore(Long skuId) {
        String hotKey = "hotScore";//ZSet类似于TreeMap，此处hotKey为treeMap的名称，value为key，value映射真正存储的值
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (hotScore != null && hotScore % 10 == 0) {
            Goods goods = goodsRepository.findById(skuId).get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    //1.根据用户的检索条件生成DSL语句
    //2.执行DSL，获取结果集
    //3.将查询结果集封装返回对象
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        try {
            SearchRequest searchRequest = buildQueryDsl(searchParam);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo responseVo = parseSearchResult(searchResponse, searchParam);
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("检索商品发生错误！");
        }
    }

    //封装结果集对象
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse, SearchParam searchParam) {
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        responseVo.setTotal(hits.totalHits);//设置total，从hits中获取
        //赋值goodList
        packGoodList(responseVo, hits);
        Map<String, Aggregation> aggMap = searchResponse.getAggregations().asMap();
        //赋值trademarkList
        packTrademark(responseVo, aggMap);
        //赋值attrsList,类似于赋值trademark
        packAttrList(responseVo, aggMap);

        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setTotalPages((responseVo.getTotal() + responseVo.getPageSize() - 1) / responseVo.getPageSize());//一共有几页,固定公式
        return responseVo;
    }

    private void packAttrList(SearchResponseVo responseVo, Map<String, Aggregation> aggMap) {
        ParsedNested attrAgg = (ParsedNested) aggMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrVoList = attrIdAgg.getBuckets().stream().map((bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            String attrIdString = bucket.getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrIdString));//获取属性id
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrNameString = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrNameString);//获取属性名称
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(attrValueList);//获取属性值集合
            return searchResponseAttrVo;
        })).collect(Collectors.toList());
        responseVo.setAttrsList(attrVoList);
    }

    private void packTrademark(SearchResponseVo responseVo, Map<String, Aggregation> aggMap) {
        //数据类型转换，获取到桶的集合
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggMap.get("tmIdAgg");
        List<SearchResponseTmVo> tmVoList = tmIdAgg.getBuckets().stream().map((bucket) -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            String keyAsString = bucket.getKeyAsString();//获取到品牌id
            searchResponseTmVo.setTmId(Long.parseLong(keyAsString));
            Aggregations bucketAggregations = bucket.getAggregations();
            ParsedStringTerms tmNameAgg = bucketAggregations.get("tmNameAgg");//获取品牌名称
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            ParsedStringTerms tmLogoUrlAgg = bucketAggregations.get("tmLogoUrlAgg");//获取品牌logoUrl，与名称获取方式相同
            String url = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(url);
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        responseVo.setTrademarkList(tmVoList);
    }

    private void packGoodList(SearchResponseVo responseVo, SearchHits hits) {
        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        if (subHits != null && subHits.length > 0) {
            for (SearchHit subHit : subHits) {
                String sourceAsString = subHit.getSourceAsString();
                Goods goods = JSONObject.parseObject(sourceAsString,Goods.class);
                //如果高亮的数据不为空，应该获取高亮的名称
                HighlightField highlightField = subHit.getHighlightFields().get("title");
                if (highlightField != null) {
                    //获取到高亮中的数据
                    Text text = highlightField.getFragments()[0];
                    goods.setTitle(text.toString());
                }
                goodsList.add(goods);
            }
        }
        responseVo.setGoodsList(goodsList);
    }

    //生成DSL语句，依照可能的DSL语句逐步分析
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断用户是否根据分类id进行查询
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        //判断是否根据品牌id进行过滤
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split("[:]");
            if (split.length == 2) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }
        //判断用户是否通过平台属性值进行过滤
        //props=23:8G:运行内存&props=107:华为:二级手机
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split("[:]");
                if (split.length == 3) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //设置平台属性id
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    //设置平台属性值
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //判断用户是否根据keyword检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND));
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //分页
        searchSourceBuilder.from((searchParam.getPageNo() - 1) * searchParam.getPageSize());
        searchSourceBuilder.size(searchParam.getPageSize());
        //排序  order=1:asc  1:desc  1表示按照热度hotScore排序,2表示按照价格排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split("[:]");
            if (split.length == 2) {
                String field = "";
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        } else {//默认情况下按照热度降序排序
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//高亮标记的字段
        highlightBuilder.postTags("</span>");//高亮标记的后缀
        highlightBuilder.preTags("<span style=color:red>");//高亮标记的前缀
        searchSourceBuilder.highlighter(highlightBuilder);
        //聚合
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("tmIdAgg").field("tmId")
                        .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName").size(10))
                        .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl").size(10))
        );//品牌聚合
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg", "attrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").size(10)
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName").size(10))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue").size(10)))
        );//平台属性聚合
        searchSourceBuilder.fetchSource(new String[]{"id", "title", "defaultImg", "price"}, null);//设置那些字段显示那些不显示
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        String dsl = searchSourceBuilder.toString();
        System.out.println("dsl = " + dsl);
        return searchRequest;
    }
}
