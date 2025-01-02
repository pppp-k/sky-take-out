package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量保存套餐和菜品的关联关系
     * @param setmealDishList
     */
    void insertBatch(List<SetmealDish> setmealDishList);

    /**
     * 根据套餐id批量删除套餐菜品关系表的数据
     * @param setmealIds
     */
    void deleteBySetmealIds(List<Long> setmealIds);

    /**
     * 根据套餐id查询对应的菜品
     *
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);
}
