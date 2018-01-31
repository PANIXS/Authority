package com.mmall.service;

import com.google.common.base.Preconditions;
import com.mmall.common.RequestHolder;
import com.mmall.dao.SysDeptMapper;
import com.mmall.dao.SysUserMapper;
import com.mmall.exception.ParamException;
import com.mmall.model.SysDept;
import com.mmall.param.DeptParam;
import com.mmall.util.BeanValidator;
import com.mmall.util.IpUtil;
import com.mmall.util.LevelUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class SysDeptService {
    @Resource
    private SysDeptMapper sysDeptMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysLogService sysLogService;

    public void save(DeptParam param){
        BeanValidator.check(param);
        if (checkExist(param.getParentId(), param.getName(), param.getId())){
            throw new ParamException("同一层级下存在相同名称的部门");
        }
        SysDept dept = SysDept.builder().name(param.getName()).parentId(param.getParentId())
                .seq(param.getSeq()).remark(param.getRemark()).build();
        dept.setLevel(LevelUtil.calculateLevel(getLevel(param.getParentId()),param.getParentId()));
        dept.setOperator(RequestHolder.getCurrentUser().getUsername());
        dept.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        dept.setOperateTime(new Date());
        sysDeptMapper.insertSelective(dept);
        sysLogService.saveDeptLog(null,dept);
    }

    public void update(DeptParam param){
        BeanValidator.check(param);
        if (checkExist(param.getParentId(), param.getName(), param.getId())){
            throw new ParamException("同一层级下存在相同名称的部门");
        }
        SysDept before = sysDeptMapper.selectByPrimaryKey(param.getId());
        Preconditions.checkNotNull(before,"待更新的部门不存在");
        SysDept after = SysDept.builder().id(param.getId()).name(param.getName()).parentId(param.getParentId())
                .seq(param.getSeq()).remark(param.getRemark()).build();
        after.setLevel(LevelUtil.calculateLevel(getLevel(param.getParentId()),param.getParentId()));
        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        after.setOperateTime(new Date());

        updateWithChild(before,after);
        sysLogService.saveDeptLog(before,after);
    }

    @Transactional
    protected void updateWithChild(SysDept before ,SysDept after){
            String newLevelPrefix = after.getLevel();
            String oldLevelPrefix = before.getLevel();
            if(after.getLevel().contains(after.getId().toString())){//新的层级不能包含自己的id
                throw new ParamException("抱歉,在下觉得你的层级关系不对");
            }
            if (!after.getLevel().equals(before.getLevel())){
                List<SysDept> deptList = sysDeptMapper.getChildDeptListByLevel(before.getLevel()+"."+before.getId());
                if (CollectionUtils.isNotEmpty(deptList)){
                    for (SysDept dept:deptList){
                        String level = dept.getLevel();
                        if (level.indexOf(oldLevelPrefix)==0){
                            //1.不能包含自己id,2 不能有重复id ->不能包含自己id
                            //将原来查出来的level逐个更新 0.1 -> 0.1.8     0.1.8+''   0.1.8->0.1     0.1+s
                            level = newLevelPrefix+level.substring(oldLevelPrefix.length());
                            dept.setLevel(level);
                        }
                    }
                /*    for(SysDept dept:deptList){
                        sysDeptMapper.updateByPrimaryKey(dept);
                    }*/
                    sysDeptMapper.batchUpdateLevel(deptList);
                }
            }
            sysDeptMapper.updateByPrimaryKey(after);
    }

    private boolean checkExist(Integer parantId, String deptName, Integer deptId){
        return sysDeptMapper.countByNameAndParentId(parantId,deptName,deptId)>0;
    }

    private String getLevel(Integer deptId){
        SysDept dept = sysDeptMapper.selectByPrimaryKey(deptId);
        if (dept ==null){
            return null;
        }
        return dept.getLevel();
    }

    public void delete(int deptId){
        SysDept dept = sysDeptMapper.selectByPrimaryKey(deptId);
        Preconditions.checkNotNull(dept,"待删除的部门不存在,无法删除");
        if (sysDeptMapper.countByParentId(dept.getId())>0){
            throw new ParamException("当前部门下面有子部门,无法删除");
        }
        if (sysUserMapper.countByDeptId(dept.getId())>0){
            throw new ParamException("当前部门下面有用户,无法删除");
        }
        sysDeptMapper.deleteByPrimaryKey(deptId);
    }

}
