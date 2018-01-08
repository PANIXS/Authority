package com.mmall.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mmall.dao.SysDeptMapper;
import com.mmall.dto.DeptLevelDto;
import com.mmall.model.SysDept;
import com.mmall.util.LevelUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static java.util.Comparator.comparing;

@Service
public class SysTreeService {
    @Resource
    private SysDeptMapper sysDeptMapper;

    public List<DeptLevelDto> deptTree(){
        List<SysDept> deptList = sysDeptMapper.getAllDept();

        List<DeptLevelDto> dtoList = Lists.newArrayList();
        for (SysDept dept : deptList){
            DeptLevelDto dto = DeptLevelDto.adapt(dept);
            dtoList.add(dto);
        }
        return deptListToTree(dtoList);
    }
    public List<DeptLevelDto> deptListToTree(List<DeptLevelDto> deptLevelList){
        if (CollectionUtils.isEmpty(deptLevelList)){
            return Lists.newArrayList();
        }
        Multimap<String,DeptLevelDto> levelDeptMap = ArrayListMultimap.create();
        List<DeptLevelDto> rootList = Lists.newArrayList();

        for (DeptLevelDto dto : deptLevelList){
            levelDeptMap.put(dto.getLevel(),dto);
            if (LevelUtil.ROOT.equals(dto.getLevel())){
                rootList.add(dto);
            }
        }
        //按照seq从小到大排序
        Collections.sort(rootList, comparing(o -> o.getSeq()));
        //树形生成树
        transformDeptTree(rootList,LevelUtil.ROOT,levelDeptMap);
        return rootList;
    }
    public void transformDeptTree(List<DeptLevelDto> deptLevelList,String level,Multimap<String,DeptLevelDto> levelDeptMap){
        for (int i=0; i<deptLevelList.size();i++){
            //遍历该层的每个元素
            DeptLevelDto deptLevelDto = deptLevelList.get(i);
            //处理当前层级的数据
            String nextLevel = LevelUtil.calculateLevel(level,deptLevelDto.getId());
            //处理下一层
            List<DeptLevelDto> temDeptList = (List<DeptLevelDto>)levelDeptMap.get(nextLevel);
            if (CollectionUtils.isNotEmpty(temDeptList)){
                //排序
                Collections.sort(temDeptList,comparing(o->o.getSeq()));
                //设置下一层部门
                deptLevelDto.setDeptList(temDeptList);
                //进入到下一层处理
                transformDeptTree(temDeptList,nextLevel,levelDeptMap);
            }
        }
    }
}
