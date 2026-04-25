package com.hawkeye.auth.business.mapper;

import com.hawkeye.auth.common.pojo.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthMapper {

    @Select("select * from account where username = #{username}")
    Account selectByUsername(String username);
}
