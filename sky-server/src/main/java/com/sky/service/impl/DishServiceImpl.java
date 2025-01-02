package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 新增菜品和对应口味
     * @param dishDTO
     */
    @Transactional//多表操作，保持原子性
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入一条数据
        dishMapper.insert(dish);

        //获得插入后的菜品主键id
        Long id = dish.getId();


        //向口味表插入多条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(id);
            });
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     * @return
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断菜品是否能够删除--是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断菜品是否能够删除--是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        //删除菜品表的菜品数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //删除菜品关联的口味数据
//            dishFlavorMapper.delteByDishId(id);
//        }

        //批量删除菜品数据
        dishMapper.deleteByIds(ids);

        //批量删除口味
        dishFlavorMapper.delteByDishIds(ids);

    }

    /**
     * 根据id查菜品与口味
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //查菜品
        Dish dish = dishMapper.getById(id);

        //查口味
        List<DishFlavor> dishFlavors = dishFlavorMapper.getById(id);

        //封装
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品和口味
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //删除原有口味数据
        dishFlavorMapper.delteByDishId(dishDTO.getId());

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 起售或停售菜品
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        // 如果是停售操作，还需要将包含当前菜品的套餐也停售
        if (status == StatusConstant.DISABLE) {
            //只写了根据多个菜品id查套餐的接口，所以用List
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> seatMealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            for(Long seatMealId : seatMealIds) {
                Setmeal setmeal = Setmeal.builder()
                        .id(seatMealId)
                        .status(StatusConstant.DISABLE)
                        .build();
                setmealMapper.update(setmeal);
            }
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {

        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();



        return dishMapper.list(dish);
    }
}
