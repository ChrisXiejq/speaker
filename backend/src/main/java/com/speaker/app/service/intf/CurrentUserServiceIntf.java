package com.speaker.app.service.intf;

import com.speaker.app.model.entity.User;

public interface CurrentUserServiceIntf {

    /** 返回单用户模式的数据归属账号；数据库为空时自动创建。 */
    User requireCurrentUser();
}
