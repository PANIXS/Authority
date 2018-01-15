package com.mmall.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mmall.dao.SysAclMapper;
import com.mmall.dao.SysAclModuleMapper;
import com.mmall.dao.SysDeptMapper;
import com.mmall.dto.AclDto;
import com.mmall.dto.AclModuleLevelDto;
import com.mmall.dto.DeptLevelDto;
import com.mmall.model.SysAcl;
import com.mmall.model.SysAclModule;
import com.mmall.model.SysDept;
import com.mmall.util.LevelUtil;
import com.sun.tools.classfile.StackMap_attribute;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

@Service
public class SysTreeService {
    @Resource
    private SysDeptMapper sysDeptMapper;
    @Resource
    private SysAclModuleMapper sysAclModuleMapper;
    @Resource
    private SysCoreService sysCoreService;
    @Resource
    private SysAclMapper sysAclMapper;

    public List<AclModuleLevelDto> roleTree(int roleId){
        //1 当前用户已分配的权限点
        List<SysAcl> userAclList = sysCoreService.getCurrentUserAclList();
        //2 当前角色分配的权限点
        List<SysAcl> roleAclList = sysCoreService.getRoleAclList(roleId);
        //3 当前系统所有权限点
        List<AclDto> aclDtoList = Lists.newArrayList();

        Set<Integer> userAclIdSet = userAclList.stream()
                                                                    .map(a->a.getId())
                                                                    .collect(toSet());
        Set<Integer> roleAclIdSet = roleAclList.stream()
                                                                    .map(sysAcl -> sysAcl.getId())
                                                                    .collect(toSet());
        //取得所有权限
        List<SysAcl> allAclList = sysAclMapper.getAll();

        for (SysAcl acl:allAclList){
            AclDto dto = AclDto.adapt(acl);
            if (userAclIdSet.contains(acl.getId())){
                dto.setHasAcl(true);
            }
            if (roleAclIdSet.contains(acl.getId())){
                dto.setChecked(true);
            }
            aclDtoList.add(dto);
        }
        return aclListToTree(aclDtoList);
    }
    public List<AclModuleLevelDto> aclListToTree(List<AclDto> aclDtoList){
        if (CollectionUtils.isEmpty(aclDtoList)){
            return Lists.newArrayList();
        }
        //拿到模块树
        List<AclModuleLevelDto> aclModuleLevelList = aclModuleTree();

        Multimap<Integer,AclDto> moduleIdAclMap = ArrayListMultimap.create();
        for (AclDto acl:aclDtoList){
            if (acl.getStatus()==1){
                moduleIdAclMap.put(acl.getAclModuleId(),acl);
            }
        }
        bindAclsWithOrder(aclModuleLevelList,moduleIdAclMap);
        return aclModuleLevelList;
    }

    public void bindAclsWithOrder(List<AclModuleLevelDto> aclModuleLevelList,Multimap<Integer,AclDto> moduleIdAclMap){
        if (CollectionUtils.isEmpty(aclModuleLevelList)){
            return;
        }
        for (AclModuleLevelDto dto:aclModuleLevelList){
            //根据模块id选出当前存储的权限点列表
            List<AclDto> aclDtoList =(List<AclDto>)moduleIdAclMap.get(dto.getId());
            if (CollectionUtils.isNotEmpty(aclDtoList)){
                Collections.sort(aclDtoList,comparing(SysAcl::getSeq));
                dto.setAclList(aclDtoList);
            }
            bindAclsWithOrder(dto.getAclModuleList(),moduleIdAclMap);
        }
    }

    public List<AclModuleLevelDto> aclModuleTree(){
        List<SysAclModule> aclModuleList = sysAclModuleMapper.getAllAclModule();

     /*   List<AclModuleLevelDto> dtoList = Lists.newArrayList();
        for (SysAclModule aclModule:aclModuleList){
            dtoList.add(AclModuleLevelDto.adapt(aclModule));
        }*/
        List<AclModuleLevelDto> dtoList = aclModuleList.stream()
                .map(AclModuleLevelDto::adapt)
                .collect(toList());

        return aclModuleListToTree(dtoList);
    }
    public List<AclModuleLevelDto> aclModuleListToTree(List<AclModuleLevelDto> dtoList){
        if (CollectionUtils.isEmpty(dtoList)){
            return Lists.newArrayList();
        }
        Multimap<String,AclModuleLevelDto> levelAclModuleMap = ArrayListMultimap.create();
        List<AclModuleLevelDto> rootList = Lists.newArrayList();
       dtoList.stream().forEach(
               a-> {
                   levelAclModuleMap.put(a.getLevel(),a);
                   if (LevelUtil.ROOT.equals(a.getLevel())) {
                       rootList.add(a);
                    }
           });
        /*for (AclModuleLevelDto dto:dtoList){
            levelAclModuleMap.put(dto.getLevel(),dto);
            if (LevelUtil.ROOT.equals(dto.getLevel())){
                //取出首层元素
                rootList.add(dto);
            }
        }*/

        Collections.sort(rootList, comparing(SysAclModule::getSeq));
        transformAclModuleTree(rootList,LevelUtil.ROOT,levelAclModuleMap);
        return rootList;
    }
    //就是递归的设置rootList里面的List(以前是空的),最后成为树
    public void transformAclModuleTree(List<AclModuleLevelDto> dtoList,String level,Multimap<String,AclModuleLevelDto> levelAclModuleMap){
        for (int i = 0; i < dtoList.size(); i++){
            AclModuleLevelDto dto = dtoList.get(i);
            String nextLevel = LevelUtil.calculateLevel(level,dto.getId());
            List<AclModuleLevelDto> tempList = (List<AclModuleLevelDto>)levelAclModuleMap.get(nextLevel);
            if (CollectionUtils.isNotEmpty(tempList)){
                Collections.sort(tempList,comparing(SysAclModule::getSeq));
                dto.setAclModuleList(tempList);
                transformAclModuleTree(tempList,nextLevel,levelAclModuleMap);
            }
        }
    }

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
