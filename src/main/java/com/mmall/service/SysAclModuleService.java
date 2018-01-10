package com.mmall.service;

import com.mmall.common.RequestHolder;
import com.mmall.dao.SysAclModuleMapper;
import com.mmall.exception.ParamException;
import com.mmall.model.SysAclModule;
import com.mmall.param.AclModuleParam;
import com.mmall.util.BeanValidator;
import com.mmall.util.IpUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class SysAclModuleService {

    @Resource
    private SysAclModuleMapper sysAclModuleMapper;

    public void save(AclModuleParam param){
        BeanValidator.check(param);
        if (checkExist(param.getParentId(), param.getName(), param.getId())){
            throw new ParamException("同一层级下存在相同名称的权限模块");
        }
        SysAclModule aclModule = SysAclModule.builder().name(param.getName()).parentId(param.getParentId()).seq(param.getSeq())
                .status(param.getStatus()).remark(param.getRemark()).build();
        aclModule.setOperator(RequestHolder.getCurrentUser().getUsername());
        aclModule.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        aclModule.setOperateTime(new Date());
        sysAclModuleMapper.insertSelective(aclModule);
    }

    public void update(AclModuleParam param){

    }

    private void updateWithChild(AclModuleParam before,SysAclModule after){

    }

    private boolean checkExist(Integer parantId, String aclModuleName, Integer deptId){
        return sysAclModuleMapper.countByNameAndParentId(parantId,aclModuleName,deptId)>0;
    }

    private String getLevel(Integer aclModuleId){
        SysAclModule sysAclModule = sysAclModuleMapper.selectByPrimaryKey(aclModuleId);
        if (sysAclModule ==null){
            return null;
        }
        return sysAclModule.getLevel();
    }

}
