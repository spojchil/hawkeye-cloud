package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapstruct.VulMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 漏洞分类服务实现。
 * <p>
 * 分类采用树形结构（通过 {@code parentId} 指向父分类），参考 {@code AssetCategory}。
 * 支持分类的 CRUD、防环校验、父子约束，以及分类与模板的批量关联/移除操作。
 * <p>
 * <b>关键业务规则：</b>
 * <ul>
 *   <li><b>变更父分类防环：</b> 目标父节点不能是当前节点的子孙</li>
 *   <li><b>删除保护：</b> 有子分类或关联模板时拒绝删除</li>
 *   <li><b>批量关联去重：</b> 已存在的关联关系跳过，不重复插入</li>
 * </ul>
 *
 * @see VulCategoryService
 */
@Service
@RequiredArgsConstructor
public class VulCategoryServiceImpl extends ServiceImpl<VulCategoryMapper, VulCategory>
        implements VulCategoryService {

    private final VulMapstruct vulMapstruct;
    private final VulCategoryMappingMapper mappingMapper;

    @Override
    @LogExecutionTime
    public List<VulCategoryVO.Response> listCategories(Long parentId, String name) {
        LambdaQueryWrapper<VulCategory> wrapper = new LambdaQueryWrapper<VulCategory>()
                .eq(parentId != null, VulCategory::getParentId, parentId)
                .isNull(parentId == null, VulCategory::getParentId)
                .like(StrUtil.isNotBlank(name), VulCategory::getName, name)
                .orderByAsc(VulCategory::getName);

        return baseMapper.selectList(wrapper).stream()
                .map(vulMapstruct::toCategoryVO)
                .toList();
    }

    @Override
    @LogExecutionTime
    @Transactional
    public VulCategoryVO.Response create(VulCategoryVO.Request request) {
        VulCategory category = vulMapstruct.toCategoryEntity(request);
        baseMapper.insert(category);
        return vulMapstruct.toCategoryVO(category);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public VulCategoryVO.Response update(Long categoryId, VulCategoryVO.Request request) {
        VulCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                        "父分类不能是自己", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
            }
            if (isAncestor(categoryId, request.getParentId())) {
                throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                        "不能将分类挂到自己的子分类下，会形成循环",
                        HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
            }
        }

        LambdaUpdateWrapper<VulCategory> wrapper = new LambdaUpdateWrapper<VulCategory>()
                .eq(VulCategory::getCategoryId, categoryId)
                .set(request.getName() != null, VulCategory::getName, request.getName())
                .set(request.getParentId() != null, VulCategory::getParentId, request.getParentId())
                .set(request.getDescription() != null, VulCategory::getDescription, request.getDescription());

        baseMapper.update(null, wrapper);

        VulCategory updated = baseMapper.selectById(categoryId);
        return vulMapstruct.toCategoryVO(updated);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void delete(Long categoryId) {
        VulCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getParentId, categoryId));
        if (childCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，请先删除子分类",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在关联模板，请先移除所有模板",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        baseMapper.deleteById(categoryId);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public int addTemplates(Long categoryId, List<Long> templateIds) {
        if (baseMapper.selectById(categoryId) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        Set<Long> existing = new HashSet<>(mappingMapper.selectList(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId)
                        .in(VulCategoryMapping::getTemplateId, templateIds)
        ).stream().map(VulCategoryMapping::getTemplateId).toList());

        // ★ existing 只含 DB 已有记录，不含本轮输入中的重复项，需要用 seen 做输入级去重
        List<VulCategoryMapping> newMappings = new ArrayList<>();
        for (Long templateId : templateIds) {
            if (existing.add(templateId)) {
                VulCategoryMapping mapping = new VulCategoryMapping();
                mapping.setCategoryId(categoryId);
                mapping.setTemplateId(templateId);
                newMappings.add(mapping);
            }
        }

        if (!newMappings.isEmpty()) {
            // ★ saveBatch 一次 SQL 批量插入，避免逐条 insert 的 N 次网络往返
            //    MyBatis-Plus saveBatch 内部会自动设置 BaseEntity 字段（tenantId、createTime 等）
            mappingMapper.insert(newMappings);
        }
        return newMappings.size();
    }

    @Override
    @LogExecutionTime
    @Transactional
    public int removeTemplates(Long categoryId, List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return 0;
        }
        return mappingMapper.delete(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId)
                        .in(VulCategoryMapping::getTemplateId, templateIds));
    }

    /**
     * 判断 targetId 是否是 categoryId 的祖先节点。
     * 从 targetId 出发沿 parentId 链向上追溯，检查是否经过 categoryId。
     * <p>
     * 每层一次 selectById，时间复杂度 O(树深度)。分类树的层级一般在 3~5 层，
     * 不会超过 10 层，单次校验 ~5 次查询可接受。若树深度失控，可改为一次全表
     * selectList + 内存构建父链 Map。
     */
    private boolean isAncestor(Long categoryId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && !visited.contains(current)) {
            if (current.equals(categoryId)) {
                return true;
            }
            visited.add(current);
            VulCategory parent = baseMapper.selectById(current);
            if (parent == null) {
                break;
            }
            current = parent.getParentId();
        }
        return false;
    }
}
