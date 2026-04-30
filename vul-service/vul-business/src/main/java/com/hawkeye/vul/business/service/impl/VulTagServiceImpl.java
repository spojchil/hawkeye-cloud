package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.common.utils.annotation.LogExecutionTime;
import com.hawkeye.vul.business.mapper.VulTagMapper;
import com.hawkeye.vul.business.mapper.VulTemplateTagMapper;
import com.hawkeye.vul.business.service.VulTagService;
import com.hawkeye.vul.common.pojo.entity.VulTag;
import com.hawkeye.vul.common.pojo.entity.VulTemplateTag;
import com.hawkeye.vul.common.pojo.vo.vul.VulTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VulTagServiceImpl implements VulTagService {

    private final VulTagMapper tagMapper;
    private final VulTemplateTagMapper templateTagMapper;

    @Override
    @LogExecutionTime
    public List<VulTagVO> list(String keyword) {
        LambdaQueryWrapper<VulTag> wrapper = new LambdaQueryWrapper<VulTag>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(VulTag::getName, keyword.toLowerCase());
        }
        wrapper.orderByAsc(VulTag::getName);

        return tagMapper.selectList(wrapper).stream().map(tag -> {
            VulTagVO vo = new VulTagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getName());
            vo.setTemplateCount(templateTagMapper.selectCount(
                    new LambdaQueryWrapper<VulTemplateTag>()
                            .eq(VulTemplateTag::getTagId, tag.getId())));
            return vo;
        }).toList();
    }
}
