package com.yky.blog.common.satoken;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpLogic;

/**
 * 前台读者的 Sa-Token 多账号体系。
 * 与管理员的默认 StpUtil 隔离：loginType=user，token 名为 user-token，
 * 这样同一浏览器里读者登录态和后台登录态互不冲突。
 */
public class StpUserUtil {

    public static final StpLogic stpLogic = new StpLogic("user") {
        @Override
        public String getTokenName() {
            return "user-token";
        }
    };

    public static void login(Object id) {
        stpLogic.login(id);
    }

    public static void login(Object id, SaLoginModel model) {
        stpLogic.login(id, model);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    public static Object getLoginId() {
        return stpLogic.getLoginId();
    }

    public static Object getLoginIdDefaultNull() {
        return stpLogic.getLoginIdDefaultNull();
    }

    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    public static SaTokenInfo getTokenInfo() {
        return stpLogic.getTokenInfo();
    }
}
