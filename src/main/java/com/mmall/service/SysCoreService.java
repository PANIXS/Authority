package com.mmall.service;

import com.google.common.collect.Lists;
import com.mmall.beans.CacheKeyConstants;
import com.mmall.common.RequestHolder;
import com.mmall.dao.SysAclMapper;
import com.mmall.dao.SysRoleAclMapper;
import com.mmall.dao.SysRoleUserMapper;
import com.mmall.model.SysAcl;
import com.mmall.model.SysUser;
import com.mmall.util.JsonMapper;
import com.mmall.util.StringUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Service
public class SysCoreService {

    @Resource
    private SysAclMapper sysAclMapper;
    @Resource
    private SysRoleUserMapper sysRoleUserMapper;
    @Resource
    private SysRoleAclMapper sysRoleAclMapper;
    @Resource
    private SysCacheService sysCacheService;

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
        //自己定义的规则,可以是配置文件获取
        SysUser sysUser = RequestHolder.getCurrentUser();
        if (sysUser.getMail().contains("admin")){
            return true;
        }
        return false;
    }

    public boolean hasUrlAcl(String url){
        if (isSuperAdmin()){
            return true;
        }
        //权限点为空的时候,直接true,说明不关心这个url
        List<SysAcl> aclList = sysAclMapper.getByUrl(url);
        if (CollectionUtils.isEmpty(aclList)){
            return true;
        }

        List<SysAcl> userAclList = getCurrentUserAclListFromCache();
        Set<Integer> userAclIdSet = userAclList.stream().map(SysAcl::getId).collect(toSet());
        boolean hasValidAcl = false;
        // 规则: 只要有一个权限点有权限,那么我们就认为有访问权限
        for (SysAcl acl : aclList) {
            //判断一个用户是否具有某个权限点的访问权限
            if (acl == null || acl.getStatus() != 1) { //权限点无效
                continue;
            }
            hasValidAcl = true;
            if (userAclIdSet.contains(acl.getId())) {
                return true;
            }
        }
            if (!hasValidAcl){
                return true;
            }
            return false;
        }

        public  List<SysAcl>   getCurrentUserAclListFromCache(){
            int userId = RequestHolder.getCurrentUser().getId();
            String cacheValue = sysCacheService.getFromCache(CacheKeyConstants.USER_ACLS,String.valueOf(userId));
            if (StringUtils.isBlank(cacheValue)){
                List<SysAcl> aclList = getCurrentUserAclList();
                if (CollectionUtils.isNotEmpty(aclList)){
                    sysCacheService.saveCache(JsonMapper.obj2String(aclList),600,CacheKeyConstants.USER_ACLS,String.valueOf(userId));
                }
                return aclList;
            }
            return JsonMapper.string2Obj(cacheValue, new TypeReference<List<SysAcl>>() {
                @Override
                public Type getType() {
                    return super.getType();
                }
            });
        }
}

