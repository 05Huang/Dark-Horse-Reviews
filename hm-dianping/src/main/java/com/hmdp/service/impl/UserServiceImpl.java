package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式有误!");
        }
        //2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.保存验证码到session
        session.setAttribute("code", code);
        //4.发送验证码
        log.debug("验证码为" + code);
        //5.返回成功
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.验证手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式有误!");
        }
        //2，验证验证码
        String cahceCode = (String) session.getAttribute("code");
        String code = loginForm.getCode();
        if (code == null || !cahceCode.equals(code)) {
            //3.验证码不一致
            return Result.fail("验证码不正确");
        }
        //4.验证用户是否存在
        User user = query().eq("phone", phone).one();
        //5.不存在创建
        if (user == null) {
            user = CreatUserWithPhone(phone);
        }
        //6.保存用户到session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        //7.返回ok
        return Result.ok();
    }

    private User CreatUserWithPhone(String phone) {
        User user = new User();
        user.setNickName("user_id" + RandomUtil.randomString(10));
        user.setPhone(phone);
        save(user);
        return user;
    }
}
