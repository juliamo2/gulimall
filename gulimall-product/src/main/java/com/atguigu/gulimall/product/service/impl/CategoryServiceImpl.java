package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@EnableCaching
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1?????????????????????
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2?????????????????????????????????

        //2.1?????????????????????????????????
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1????????????????????????????????????????????????????????????

        //????????????
        baseMapper.deleteBatchIds(asList);
    }

    //[2,25,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * ?????????????????????????????????
     * @param category
     */
    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)//??????????????????????????????????????????????????????
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
//        List<CategoryEntity> categoryEntities = listWithTree();
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

//    @Override
//    public List<CategoryEntity> getLevel1Categorys() {
//
//        List<CategoryEntity> categoryEntities = getParent_cid(sele);
//        return categoryEntities;
//    }

    //?????????
//    public Map<String,List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock(){
//        synchronized (this){
//            return getCatalogJsonFromDb();
//        }
//    }


    //?????????????????????????????????
    //
//    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
//            //?????????????????? ????????????
//        RLock lock = redisson.getLock("catalogJson-lock");
//        lock.lock();
//
//        Map<String,List<Catelog2Vo>> catalogJsonFromDb = null;
//        try{
//            catalogJsonFromDb = getCatalogJsonFromDb();
//        }finally {
//            lock.unlock();
//        }
//        return catalogJsonFromDb;
//    }

//    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
//        //        ???????????????
////        ??????????????????
////        ??????
//
//        //????????????
//        //???????????????JSON????????????????????????????????????
//
//        String uuid = UUID.randomUUID().toString();
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,3000,TimeUnit.SECONDS);
//        //???????????????????????????
//        if(lock){
//            //???????????????????????????
//            Map<String,List<Catelog2Vo>> catalogJsonFromDb = null;
//            try{
//                catalogJsonFromDb = getCatalogJsonFromDb();
//            }finally {
//                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//                //????????????????????????????????????nx??????????????????
//                redisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class),Arrays.asList("lock"),uuid);
//            }
//            //???????????????????????????,????????????????????????,??????????????????????????????????????????
//            //?????????
//            return catalogJsonFromDb;
//        }else {
//            try {
//                Thread.sleep(200);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            return getCatalogJsonFromDbWithRedisLock();
//        }
//    }


    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
                k -> k.getCatId().toString(),
                v -> {
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());

                    //
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                            List<CategoryEntity> level3Catelogs = getParent_cid(selectList,l2.getCatId());
                            if(level3Catelogs!=null){
                                List<Catelog2Vo.Category3Vo> category3Vos = level3Catelogs.stream().map(l3 -> {
                                    Catelog2Vo.Category3Vo catelog3 = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catelog3;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(category3Vos);

                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }
        ));

//        String s = JSON.toJSONString(parent_cid);
//        redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
//        System.out.println(parent_cid);
        return parent_cid;
    }

//    public Map<String, List<Catelog2Vo>> getCatalogJson(){
//
//        //1.??????????????????
////        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
////        if(StringUtils.isEmpty(catalogJSON)){
//            //????????????????????????????????????
////            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
//            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
//            //??????????????????????????????,????????????????????????JSON?????????,????????????????????????
//
////            return catalogJsonFromDb;
////        }
//
////        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
//        return catalogJsonFromDb;
//    }

    //????????????????????????

//    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
//
////        ????????????????????????????????????
//        List<CategoryEntity> selectList = baseMapper.selectList(null);
//
//
//        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
//        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(
//                k -> k.getCatId().toString(),
//                v -> {
//                    List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
//
//                    //
//                    List<Catelog2Vo> catelog2Vos = null;
//                    if (categoryEntities != null) {
//                        catelog2Vos = categoryEntities.stream().map(l2 -> {
//                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
//
//                            List<CategoryEntity> level3Catelogs = getParent_cid(selectList,v.getCatId());
//                            if(level3Catelogs!=null){
//                                List<Catelog2Vo.Category3Vo> collect = level3Catelogs.stream().map(l3 -> {
//                                    Catelog2Vo.Category3Vo catelog3 = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
//                                    return catelog3;
//                                }).collect(Collectors.toList());
//                                catelog2Vo.setCatalog3List(collect);
//
//                            }
//                            return catelog2Vo;
//                        }).collect(Collectors.toList());
//                    }
//                    return catelog2Vos;
//                }
//        ));
//
////        String s = JSON.toJSONString(parent_cid);
////        redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
////        System.out.println(parent_cid);
//        return parent_cid;
//    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid().equals(parent_cid);
        }).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<>().eq("parent_cid",));
        return collect;
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1?????????????????????id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;

    }

    //????????????????????????????????????
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1??????????????????
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2??????????????????
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }



}