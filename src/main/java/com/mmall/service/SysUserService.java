package com.mmall.service;

import com.mmall.dao.SysUserMapper;
import com.mmall.exception.ParamException;
import com.mmall.model.SysUser;
import com.mmall.param.UserParam;
import com.mmall.util.BeanValidator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class SysUserService {

        @Resource
        private SysUserMapper sysUserMapper;

        public void save(UserParam param){
            BeanValidator.check(param);
            if (checkTelephoneExist(param.getTelephone(),param.getId())){
                throw new ParamException("电话已被占用");
            }
            if (checkEmailExist(param.getMail(), param.getId())){
                throw new ParamException("邮箱已被占用");
            }
            String password = "123456";
            SysUser user = SysUser.builder().username(param.getUsername()).telephone(param.getTelephone()).mail(param.getMail())
                    .password(password).deptId(param.getDeptId()).status(param.getStatus()).remark(param.getRemark()).build();
            user.setOperator("system");
            user.setOperateIp("127.0.0.1");
            user.setOperateTime(new Date());

            //TODO:sendEmail

            sysUserMapper.insertSelective(user);

        }
        public boolean checkEmailExist(String mail, Integer userId){
            return false;
        }
        public boolean checkTelephoneExist(String mail, Integer userId){
            return false;
        }

}
