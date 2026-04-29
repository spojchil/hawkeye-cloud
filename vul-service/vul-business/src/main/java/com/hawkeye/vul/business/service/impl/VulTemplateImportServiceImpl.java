package com.hawkeye.vul.business.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.business.mapper.VulTemplateMapper;
import com.hawkeye.vul.business.service.VulTemplateImportService;
import com.hawkeye.vul.common.enums.VulSeverityEnum;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 模板批量导入服务。
 * <p>
 * 从 ddocs/http/ 目录读取 Nuclei YAML 模板，解析为 Java 实体后批量入库。
 * 仅保留含 http 协议的模板；已存在的 templateId 跳过（支持增量导入）。
 * 同时按模板所属目录自动创建分类和关联关系。
 * <p>
 * <b>性能优化：</b> 查重使用批量 IN 查询（而非逐条 selectOne），
 * 10,688 个模板仅需约 22 次数据库查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VulTemplateImportServiceImpl implements VulTemplateImportService {

    private static final int BATCH_SIZE = 500;

    private final VulTemplateMapper vulTemplateMapper;
    private final VulCategoryMapper vulCategoryMapper;
    private final VulCategoryMappingMapper mappingMapper;

    @Override
    @Transactional
    public ImportResult importAll() {
        Path basePath = resolveBasePath();
        if (!Files.isDirectory(basePath)) {
            log.error("模板目录不存在: {}", basePath.toAbsolutePath());
            return new ImportResult(0, 0, 0, 0);
        }

        int total = 0, imported = 0, skipped = 0, failed = 0;
        ConcurrentHashMap<String, Long> categoryCache = new ConcurrentHashMap<>();

        try (var dirStream = Files.list(basePath)) {
            for (Path categoryDir : dirStream.filter(Files::isDirectory).toList()) {
                String categoryName = categoryDir.getFileName().toString();
                int[] result = importFromDirectory(categoryDir, categoryName, categoryCache);
                total += result[0];
                imported += result[1];
                skipped += result[2];
                failed += result[3];
            }
        } catch (IOException e) {
            log.error("遍历模板目录失败", e);
        }

        log.info("导入完成: 总计={}, 新增={}, 跳过={}, 失败={}", total, imported, skipped, failed);
        return new ImportResult(total, imported, skipped, failed);
    }

    @Override
    @Transactional
    public ImportResult importByCategory(String categoryDir) {
        Path basePath = resolveBasePath().resolve(categoryDir);
        if (!Files.isDirectory(basePath)) {
            log.error("分类目录不存在: {}", basePath.toAbsolutePath());
            return new ImportResult(0, 0, 0, 0);
        }

        ConcurrentHashMap<String, Long> categoryCache = new ConcurrentHashMap<>();
        int[] result = importFromDirectory(basePath, categoryDir, categoryCache);

        log.info("分类 [{}] 导入完成: 总计={}, 新增={}, 跳过={}, 失败={}",
                categoryDir, result[0], result[1], result[2], result[3]);
        return new ImportResult(result[0], result[1], result[2], result[3]);
    }

    /**
     * 从指定目录导入所有 YAML 模板，返回 [total, imported, skipped, failed]。
     */
    private int[] importFromDirectory(Path dir, String categoryName,
                                       ConcurrentHashMap<String, Long> categoryCache) {
        Long categoryId = getOrCreateCategory(categoryName, categoryCache);
        int total = 0, imported = 0, skipped = 0, failed = 0;
        List<VulTemplate> batch = new ArrayList<>();
        List<VulCategoryMapping> mappingBatch = new ArrayList<>();

        try (var yamlStream = Files.walk(dir)) {
            for (Path yamlFile : yamlStream.filter(p -> p.toString().endsWith(".yaml")).toList()) {
                total++;
                try {
                    String yamlContent = Files.readString(yamlFile);
                    Map<String, Object> yamlMap = new Yaml().load(yamlContent);

                    if (yamlMap == null || !yamlMap.containsKey("http")) {
                        skipped++;
                        continue;
                    }

                    String templateId = (String) yamlMap.get("id");
                    if (templateId == null) {
                        log.warn("YAML 缺少 id 字段: {}", yamlFile);
                        failed++;
                        continue;
                    }

                    VulTemplate template = parseTemplate(templateId, yamlMap);
                    batch.add(template);
                    mappingBatch.add(createEmptyMapping(categoryId));

                    if (batch.size() >= BATCH_SIZE) {
                        int[] batchResult = flushBatch(batch, mappingBatch, categoryId);
                        imported += batchResult[0];
                        skipped += batchResult[1];
                        batch.clear();
                        mappingBatch.clear();
                    }
                } catch (Exception e) {
                    failed++;
                    log.warn("解析模板失败: {} — {}", yamlFile.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("遍历目录失败: {}", dir, e);
        }

        if (!batch.isEmpty()) {
            int[] batchResult = flushBatch(batch, mappingBatch, categoryId);
            imported += batchResult[0];
            skipped += batchResult[1];
        }

        return new int[]{total, imported, skipped, failed};
    }

    /**
     * 批量入库。查重使用批量 IN 查询，避免 N+1 问题。
     */
    private int[] flushBatch(List<VulTemplate> templates, List<VulCategoryMapping> mappings, Long categoryId) {
        int imported = 0;
        int skipped = 0;

        // 批量查重：一次 IN 查询查出 batch 中所有已存在的 templateId
        Set<String> templateIds = templates.stream()
                .map(VulTemplate::getTemplateId)
                .collect(Collectors.toSet());
        Set<String> existingIds = vulTemplateMapper.selectList(
                        new LambdaQueryWrapper<VulTemplate>()
                                .in(VulTemplate::getTemplateId, templateIds)
                                .select(VulTemplate::getTemplateId))
                .stream()
                .map(VulTemplate::getTemplateId)
                .collect(Collectors.toSet());

        for (VulTemplate template : templates) {
            if (existingIds.contains(template.getTemplateId())) {
                skipped++;
            } else {
                vulTemplateMapper.insert(template);
                imported++;
            }
        }

        // 批量查重映射表
        List<Long> dbIds = templates.stream()
                .filter(t -> !existingIds.contains(t.getTemplateId()))
                .filter(t -> t.getId() != null)
                .map(VulTemplate::getId)
                .toList();

        if (!dbIds.isEmpty()) {
            Set<Long> existingMappings = mappingMapper.selectList(
                            new LambdaQueryWrapper<VulCategoryMapping>()
                                    .eq(VulCategoryMapping::getCategoryId, categoryId)
                                    .in(VulCategoryMapping::getTemplateId, dbIds))
                    .stream()
                    .map(VulCategoryMapping::getTemplateId)
                    .collect(Collectors.toSet());

            for (VulCategoryMapping mapping : mappings) {
                int index = mappings.indexOf(mapping);
                if (index >= 0 && index < templates.size()) {
                    VulTemplate template = templates.get(index);
                    if (template.getId() != null && !existingMappings.contains(template.getId())) {
                        mapping.setTemplateId(template.getId());
                        mappingMapper.insert(mapping);
                    }
                }
            }
        }

        return new int[]{imported, skipped};
    }

    private VulCategoryMapping createEmptyMapping(Long categoryId) {
        VulCategoryMapping mapping = new VulCategoryMapping();
        mapping.setCategoryId(categoryId);
        return mapping;
    }

    private VulTemplate parseTemplate(String templateId, Map<String, Object> yamlMap) {
        VulTemplate template = new VulTemplate();
        template.setTemplateId(templateId);
        template.setEnabled(true);
        template.setVersion(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) yamlMap.get("info");
        if (info != null) {
            template.setName((String) info.get("name"));
            template.setDescription((String) info.get("description"));
            template.setSeverity(extractSeverity(info));
            template.setTags(extractTags(info));
            template.setAuthor(extractAuthor(info));
            template.setReference(toJson(info.get("reference")));
            template.setClassification(toJson(info.get("classification")));
            template.setMetadata(toJson(info.get("metadata")));
        }

        template.setFlow((String) yamlMap.get("flow"));
        template.setVariables(toJson(yamlMap.get("variables")));
        template.setHttpRequests(toJson(yamlMap.get("http")));
        template.setMatchers(toJson(extractMatchers(yamlMap)));
        template.setExtractors(toJson(extractExtractorsFromHttp(yamlMap)));

        return template;
    }

    private String extractSeverity(Map<String, Object> info) {
        Object severity = info.get("severity");
        if (severity instanceof String s) {
            return VulSeverityEnum.fromNucleiSeverity(s).name().toLowerCase();
        }
        return VulSeverityEnum.UNKNOWN.name().toLowerCase();
    }

    private String extractTags(Map<String, Object> info) {
        Object tags = info.get("tags");
        return tags instanceof String s ? s : null;
    }

    private String extractAuthor(Map<String, Object> info) {
        Object author = info.get("author");
        if (author instanceof String s) {
            return s;
        }
        if (author instanceof List<?> list) {
            return String.join(",", list.stream().map(Object::toString).toList());
        }
        return null;
    }

    /**
     * 从 YAML 中提取 matchers。
     * matchers 可能定义在 http 段内的第一个步骤中，也可能在模板顶层。
     */
    @SuppressWarnings("unchecked")
    private Object extractMatchers(Map<String, Object> yamlMap) {
        Object fromHttp = extractFromFirstHttpStep(yamlMap, "matchers");
        if (fromHttp != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            Object first = yamlMap.get("http");
            if (first instanceof List<?> httpList && !httpList.isEmpty()
                    && httpList.getFirst() instanceof Map<?, ?> firstHttp) {
                result.put("matchers-condition",
                        ((Map<String, Object>) firstHttp).getOrDefault("matchers-condition", "or"));
            }
            result.put("matchers", fromHttp);
            return result;
        }
        if (yamlMap.containsKey("matchers")) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matchers-condition", yamlMap.getOrDefault("matchers-condition", "or"));
            result.put("matchers", yamlMap.get("matchers"));
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object extractExtractorsFromHttp(Map<String, Object> yamlMap) {
        return extractFromFirstHttpStep(yamlMap, "extractors");
    }

    /**
     * 从 http 数组的第一个元素中提取指定字段。
     */
    @SuppressWarnings("unchecked")
    private Object extractFromFirstHttpStep(Map<String, Object> yamlMap, String fieldName) {
        Object httpObj = yamlMap.get("http");
        if (httpObj instanceof List<?> httpList && !httpList.isEmpty()
                && httpList.getFirst() instanceof Map<?, ?> firstHttp) {
            return ((Map<String, Object>) firstHttp).get(fieldName);
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private Long getOrCreateCategory(String name, ConcurrentHashMap<String, Long> cache) {
        return cache.computeIfAbsent(name, n -> {
            VulCategory existing = vulCategoryMapper.selectOne(
                    new LambdaQueryWrapper<VulCategory>()
                            .eq(VulCategory::getName, n));
            if (existing != null) {
                return existing.getCategoryId();
            }
            VulCategory category = new VulCategory();
            category.setName(n);
            category.setDescription("Nuclei 分类: " + n);
            vulCategoryMapper.insert(category);
            return category.getCategoryId();
        });
    }

    private Path resolveBasePath() {
        String customPath = System.getProperty("vul.template.path");
        return customPath != null ? Paths.get(customPath) : Paths.get("vul-service", "ddocs", "http");
    }
}
