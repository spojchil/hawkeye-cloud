package com.hawkeye.vul.business.service;

/**
 * 模板批量导入服务。
 * 读取 ddocs/http/ 目录下的 Nuclei YAML 模板，解析后批量写入数据库。
 */
public interface VulTemplateImportService {

    /**
     * 全量导入（默认跳过已存在的模板）。
     *
     * @return 导入统计：{ total: 总数, imported: 新导入数, skipped: 跳过数, failed: 失败数 }
     */
    ImportResult importAll();

    /**
     * 按目录导入指定分类下的模板。
     *
     * @param categoryDir 目录名，如 "cves" / "misconfiguration"
     */
    ImportResult importByCategory(String categoryDir);

    record ImportResult(int total, int imported, int skipped, int failed) {
    }
}
