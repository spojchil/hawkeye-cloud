package com.hawkeye.vul.business.service;

import com.hawkeye.vul.common.pojo.vo.vul.VulTagVO;

import java.util.List;

public interface VulTagService {

    List<VulTagVO> list(String keyword);
}
