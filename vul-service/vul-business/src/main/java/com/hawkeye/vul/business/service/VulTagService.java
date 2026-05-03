package com.hawkeye.vul.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.vul.common.pojo.entity.VulTag;
import com.hawkeye.vul.common.pojo.vo.vul.VulTagVO;

import java.util.List;

public interface VulTagService extends IService<VulTag> {

    List<VulTagVO> list(String keyword);
}
