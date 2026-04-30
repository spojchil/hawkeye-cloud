package com.hawkeye.vul.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VulTemplateMapper extends BaseMapper<VulTemplate> {

    IPage<VulTemplate> pageQuery(
            Page<VulTemplate> page,
            @Param("name") String name,
            @Param("severity") String severity,
            @Param("tag") String tag,
            @Param("categoryId") Long categoryId,
            @Param("enabled") Boolean enabled,
            @Param("sort") String sort
    );
}
