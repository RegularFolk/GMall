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

    @Override//???????????????????????????????????????????????????es???,????????????????????????????????????????????????good??????es
    public void upperGoods(Long skuId) {//?????????????????????
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

    @Override//?????????????????????es????????????????????????
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    @Override//?????????????????????????????????????????????IO
    public void incrHotScore(Long skuId) {
        String hotKey = "hotScore";//ZSet?????????TreeMap?????????hotKey???treeMap????????????value???key???value????????????????????????
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (hotScore != null && hotScore % 10 == 0) {
            Goods goods = goodsRepository.findById(skuId).get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }

    //1.?????????????????????????????????DSL??????
    //2.??????DSL??????????????????
    //3.????????????????????????????????????
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        try {
            SearchRequest searchRequest = buildQueryDsl(searchParam);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo responseVo = parseSearchResult(searchResponse, searchParam);
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("???????????????????????????");
        }
    }

    //?????????????????????
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse, SearchParam searchParam) {
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        responseVo.setTotal(hits.totalHits);//??????total??????hits?????????
        //??????goodList
        packGoodList(responseVo, hits);
        Map<String, Aggregation> aggMap = searchResponse.getAggregations().asMap();
        //??????trademarkList
        packTrademark(responseVo, aggMap);
        //??????attrsList,???????????????trademark
        packAttrList(responseVo, aggMap);

        responseVo.setPageNo(searchParam.getPageNo());
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setTotalPages((responseVo.getTotal() + responseVo.getPageSize() - 1) / responseVo.getPageSize());//???????????????,????????????
        return responseVo;
    }

    private void packAttrList(SearchResponseVo responseVo, Map<String, Aggregation> aggMap) {
        ParsedNested attrAgg = (ParsedNested) aggMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrVoList = attrIdAgg.getBuckets().stream().map((bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            String attrIdString = bucket.getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrIdString));//????????????id
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrNameString = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrNameString);//??????????????????
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(attrValueList);//?????????????????????
            return searchResponseAttrVo;
        })).collect(Collectors.toList());
        responseVo.setAttrsList(attrVoList);
    }

    private void packTrademark(SearchResponseVo responseVo, Map<String, Aggregation> aggMap) {
        //??????????????????????????????????????????
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggMap.get("tmIdAgg");
        List<SearchResponseTmVo> tmVoList = tmIdAgg.getBuckets().stream().map((bucket) -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            String keyAsString = bucket.getKeyAsString();//???????????????id
            searchResponseTmVo.setTmId(Long.parseLong(keyAsString));
            Aggregations bucketAggregations = bucket.getAggregations();
            ParsedStringTerms tmNameAgg = bucketAggregations.get("tmNameAgg");//??????????????????
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            ParsedStringTerms tmLogoUrlAgg = bucketAggregations.get("tmLogoUrlAgg");//????????????logoUrl??????????????????????????????
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
                //????????????????????????????????????????????????????????????
                HighlightField highlightField = subHit.getHighlightFields().get("title");
                if (highlightField != null) {
                    //???????????????????????????
                    Text text = highlightField.getFragments()[0];
                    goods.setTitle(text.toString());
                }
                goodsList.add(goods);
            }
        }
        responseVo.setGoodsList(goodsList);
    }

    //??????DSL????????????????????????DSL??????????????????
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //??????????????????????????????id????????????
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        //????????????????????????id????????????
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split("[:]");
            if (split.length == 2) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }
        //???????????????????????????????????????????????????
        //props=23:8G:????????????&props=107:??????:????????????
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split("[:]");
                if (split.length == 3) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    //??????????????????id
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    //?????????????????????
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        //????????????????????????keyword??????
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND));
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //??????
        searchSourceBuilder.from((searchParam.getPageNo() - 1) * searchParam.getPageSize());
        searchSourceBuilder.size(searchParam.getPageSize());
        //??????  order=1:asc  1:desc  1??????????????????hotScore??????,2????????????????????????
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
        } else {//???????????????????????????????????????
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }
        //??????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");//?????????????????????
        highlightBuilder.postTags("</span>");//?????????????????????
        highlightBuilder.preTags("<span style=color:red>");//?????????????????????
        searchSourceBuilder.highlighter(highlightBuilder);
        //??????
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("tmIdAgg").field("tmId")
                        .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName").size(10))
                        .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl").size(10))
        );//????????????
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg", "attrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").size(10)
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName").size(10))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue").size(10)))
        );//??????????????????
        searchSourceBuilder.fetchSource(new String[]{"id", "title", "defaultImg", "price"}, null);//???????????????????????????????????????
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        String dsl = searchSourceBuilder.toString();
        System.out.println("dsl = " + dsl);
        return searchRequest;
    }
}
