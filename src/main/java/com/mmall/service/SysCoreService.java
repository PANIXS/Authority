package com.mmall.service;

import com.google.common.collect.Lists;
import com.mmall.common.RequestHolder;
import com.mmall.dao.SysAclMapper;
import com.mmall.dao.SysRoleAclMapper;
import com.mmall.dao.SysRoleUserMapper;
import com.mmall.model.SysAcl;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SysCoreService {

    @Resource
    private SysAclMapper sysAclMapper;
    @Resource
    private SysRoleUserMapper sysRoleUserMapper;
    @Resource
    private SysRoleAclMapper sysRoleAclMapper;

    //获取当前用户所有的权限点
    public List<SysAcl> getCurrentUserAclList(){
        int userId = RequestHolder.getCurrentUser().getId();
        return getUserAclList(userId);
    }
    //获取指定角色已分配的权限点
    public List<SysAcl> getRoleAclList(int roleId){
              List<Integer> aclIdList = sysRoleAclMapper.getAclIdListByRoleIdList(Lists.<Integer>newArrayList(roleId));
              if (CollectionUtils.isEmpty(aclIdList)){
                  return Lists.newArrayList();
              }
              return sysAclMapper.getByIdList(aclIdList);
    }
    public List<SysAcl> getUserAclList(int userId){
        if (isSuperAdmin()){
            return sysAclMapper.getAll();
        }
        //根据userID查出角色列表
        List<Integer> userRoleIdList = sysRoleUserMapper.getRoleIdListByUserId(userId);
        if (CollectionUtils.isEmpty(userRoleIdList)){
            return Lists.newArrayList();
        }
        //根据角色列表查出所有权限的id列表
        List<Integer> userAclIdList = sysRoleAclMapper.getAclIdListByRoleIdList(userRoleIdList);
        if (CollectionUtils.isEmpty(userAclIdList)){
            return Lists.newArrayList();
        }
        //返回根据id列表查出来的权限对象
        return sysAclMapper.getByIdList(userAclIdList);
    }
    public boolean isSuperAdmin(){
        return true;
    }
}
