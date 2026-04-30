#!/usr/bin/env python3
"""
Nuclei YAML 模板 → 新 vul 表结构 批量导入脚本。

用法:
    python import_templates.py --db-host localhost --db-user root --db-pass root --db-name hawkeye
    python import_templates.py --dry-run                              # 只解析不写入
    python import_templates.py --category cves/2024                   # 只导入指定子目录
    python import_templates.py --stats-only                           # 只统计不导入

依赖: pip install pyyaml pymysql
"""

import argparse
import json
import os
import sys
import time
from collections import defaultdict
from pathlib import Path

import pymysql
import yaml

# ── 常量 ──────────────────────────────────────────────
BATCH_SIZE = 500
VALID_SEVERITIES = {"critical", "high", "medium", "low", "info", "unknown"}

SEVERITY_ALIASES = {
    "informational": "info",
    "informational ": "info",
    "none": "unknown",
}


def normalize_severity(raw: str) -> str:
    """将 YAML 中的 severity 值归一到标准 6 级别。"""
    if not raw:
        return "unknown"
    s = raw.strip().lower()
    if s in VALID_SEVERITIES:
        return s
    if s in SEVERITY_ALIASES:
        return SEVERITY_ALIASES[s]
    print(f"    [WARN] 未知 severity 值: '{raw}', 回退为 'unknown'")
    return "unknown"


def truncate(value, max_len):
    """截断字符串，超出时打印警告。"""
    if value and len(value) > max_len:
        return value[:max_len]
    return value


# ── 工具函数 ──────────────────────────────────────────

def safe_str(val, default=""):
    """从 YAML 取值，兼容 str / list / None → 返回字符串。"""
    if val is None:
        return default
    if isinstance(val, list):
        return ",".join(str(v).strip() for v in val if v)
    return str(val)


# ── YAML 解析 ──────────────────────────────────────────

def parse_template(yaml_path: Path, base_dir: Path) -> dict | None:
    """
    解析单个 Nuclei YAML，返回待入库的字段字典。
    跳过不含 http: 或无法解析的模板，返回 None。
    """
    try:
        with open(yaml_path, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
    except yaml.YAMLError as e:
        print(f"  [FAIL] YAML 解析失败: {yaml_path} — {e}")
        return None
    except Exception as e:
        print(f"  [FAIL] 文件读取失败: {yaml_path} — {e}")
        return None

    if not isinstance(data, dict) or "http" not in data:
        return None

    info = data.get("info", {}) or {}
    classification = info.get("classification", {}) or {}

    # ── 基础元数据 ──
    template_id = (data.get("id") or "").strip().lower()
    if not template_id:
        print(f"  [WARN] 缺少 id 字段: {yaml_path}")
        return None

    name = truncate((info.get("name") or "").strip(), 512) or template_id
    description = truncate((info.get("description") or "").strip(), 2048) or None
    author = truncate((info.get("author") or "").strip(), 512) or None
    severity = normalize_severity(info.get("severity"))

    cve_id = safe_str(classification.get("cve-id")).strip()[:50] or None
    cwe_id = safe_str(classification.get("cwe-id")).strip()[:50] or None

    cvss_raw = classification.get("cvss-score")
    cvss_score = round(float(cvss_raw), 2) if cvss_raw is not None else None

    epss_raw = classification.get("epss-score")
    epss_score = round(float(epss_raw), 4) if epss_raw is not None else None

    flow = (data.get("flow") or "").strip()[:2000] or None

    # ── 模板级变量 ──
    variables = data.get("variables")
    variables_json = json.dumps(variables, ensure_ascii=False) if isinstance(variables, dict) and variables else None

    # ── 标签 ──
    tags_raw = info.get("tags")
    tags = []
    if isinstance(tags_raw, str):
        tags = [t.strip() for t in tags_raw.split(",") if t.strip()]
    elif isinstance(tags_raw, list):
        tags = [str(t).strip() for t in tags_raw if str(t).strip()]

    # ── 参考链接 ──
    references = []
    ref_raw = info.get("reference", info.get("references"))
    if isinstance(ref_raw, list):
        for r in ref_raw:
            if isinstance(r, str):
                references.append({"url": r.strip(), "title": None})
            elif isinstance(r, dict):
                references.append({
                    "url": ((r.get("url") or r.get("link") or "").strip()),
                    "title": ((r.get("title") or "").strip()[:512] or None),
                })

    # ── HTTP 步骤 ──
    http_items = data["http"]
    if not isinstance(http_items, list):
        http_items = [http_items]

    http_steps = []
    all_matchers = []       # (step_order | None, matcher_dict)
    all_extractors = []     # (step_order | None, extractor_dict)

    # 顶层 matchers / extractors（不属于任何步骤）
    top_matchers = data.get("matchers")
    if isinstance(top_matchers, list):
        all_matchers.extend((None, m) for m in top_matchers)
    top_extractors = data.get("extractors")
    if isinstance(top_extractors, list):
        all_extractors.extend((None, e) for e in top_extractors)

    for idx, item in enumerate(http_items):
        step_order = idx + 1
        method = ((item.get("method") or "").strip().upper()[:10]) or None
        path = item.get("path")
        headers = item.get("headers")
        body = ((item.get("body") or "").strip()) or None
        raw = item.get("raw")
        attack = ((item.get("attack") or "").strip()[:20]) or None
        matchers_condition = ((item.get("matchers-condition") or "or").strip()[:3])
        stop_at_first_match = 1 if item.get("stop-at-first-match") else 0

        # path 在 YAML 中可能是字符串或列表
        if isinstance(path, str):
            path = [path]
        path_json = json.dumps(path, ensure_ascii=False) if path else None

        # headers JSON
        headers_json = json.dumps(headers, ensure_ascii=False) if isinstance(headers, dict) else None

        # raw HTTP 文本：可能是字符串或列表（多行拼接）
        raw_text = None
        if raw:
            if isinstance(raw, list):
                raw_text = "\n".join(str(r) for r in raw)
            else:
                raw_text = str(raw)
            raw_text = raw_text.strip()

        http_steps.append({
            "step_order": step_order,
            "method": method,
            "path": path_json,
            "headers": headers_json,
            "body": body,
            "raw": raw_text,
            "attack": attack,
            "matchers_condition": matchers_condition,
            "stop_at_first_match": stop_at_first_match,
        })

        # ── 步骤内 matchers ──
        step_matchers = item.get("matchers")
        if isinstance(step_matchers, list):
            all_matchers.extend((step_order, m) for m in step_matchers)

        # ── 步骤内 extractors ──
        step_extractors = item.get("extractors")
        if isinstance(step_extractors, list):
            all_extractors.extend((step_order, e) for e in step_extractors)

    # ── 构建 matcher 记录 ──
    matchers = []
    for step_order, m in (all_matchers or []):
        mtype = ((m.get("type") or "word").strip()[:20])
        condition = ((m.get("condition") or "or").strip()[:3])
        name = ((m.get("name") or "").strip()[:255]) or None

        # 构建 config JSON：提取 type-specific 字段
        config = {}
        for key in ("words", "status", "dsl", "regex", "size", "xpath", "binary", "all"):
            val = m.get(key)
            if val is not None:
                config[key] = val
        # encoding 字段（hex）
        if m.get("encoding"):
            config["encoding"] = m["encoding"]

        matchers.append({
            "step_order": step_order,
            "type": mtype,
            "part": ((m.get("part") or "").strip()[:20]) or None,
            "condition": condition,
            "negative": 1 if m.get("negative") else 0,
            "case_insensitive": 1 if m.get("case-insensitive", m.get("caseInsensitive")) else 0,
            "internal": 1 if m.get("internal") else 0,
            "name": name,
            "config": json.dumps(config, ensure_ascii=False),
        })

    # ── 构建 extractor 记录 ──
    extractors = []
    for step_order, e in (all_extractors or []):
        etype = ((e.get("type") or "regex").strip()[:20])
        ename = ((e.get("name") or "").strip()[:255]) or None

        config = {}
        for key in ("regex", "json", "kval", "dsl"):
            val = e.get(key)
            if val is not None:
                config[key] = val

        group = e.get("group")
        group_num = int(group) if group is not None else 1

        extractors.append({
            "step_order": step_order,
            "type": etype,
            "part": ((e.get("part") or "").strip()[:20]) or None,
            "name": ename,
            "config": json.dumps(config, ensure_ascii=False),
            "internal": 1 if e.get("internal") else 0,
            "group_num": group_num,
        })

    # ── 分类目录（从文件路径提取） ──
    try:
        rel = yaml_path.relative_to(base_dir)
        category_dir = str(rel.parts[0]) if rel.parts else None
    except ValueError:
        category_dir = None

    return {
        "template_id": template_id,
        "name": name,
        "description": description,
        "author": author,
        "severity": severity,
        "cve_id": cve_id,
        "cwe_id": cwe_id,
        "cvss_score": cvss_score,
        "epss_score": epss_score,
        "flow": flow,
        "variables": variables_json,
        "tags": tags,
        "references": references,
        "http_steps": http_steps,
        "matchers": matchers,
        "extractors": extractors,
        "category_dir": category_dir,
        "file_path": str(yaml_path),
    }


# ── 数据库操作 ─────────────────────────────────────────

def build_insert_sql(table, columns):
    """拼接 INSERT INTO table (cols) VALUES (%s, %s, ...)"""
    cols = ", ".join(f"`{c}`" for c in columns)
    placeholders = ", ".join(["%s"] * len(columns))
    return f"INSERT INTO {table} ({cols}) VALUES ({placeholders})"


TEMPLATE_COLS = [
    "template_id", "name", "description", "author", "severity",
    "cve_id", "cwe_id", "cvss_score", "epss_score", "flow", "variables",
]
HTTP_STEP_COLS = [
    "template_id", "step_order", "method", "path", "headers",
    "body", "raw", "attack", "matchers_condition", "stop_at_first_match",
]
MATCHER_COLS = [
    "template_id", "step_order", "type", "part", "condition",
    "negative", "case_insensitive", "internal", "name", "config",
]
EXTRACTOR_COLS = [
    "template_id", "step_order", "type", "part", "name", "config",
    "internal", "group_num",
]


def flush_batch(cur, batch, tag_cache, category_cache, dry_run: bool, stats: dict):
    """
    将一批模板写入数据库。
    batch: list of parsed template dicts (from parse_template)
    tag_cache: {name: id}   — 已存在的标签
    category_cache: {name: id} — 已存在的分类
    """
    if not batch:
        return

    template_ids_in_batch = [t["template_id"] for t in batch]

    # ── 1. 查重：跳过已存在的 template_id ──
    if not dry_run:
        placeholders = ",".join(["%s"] * len(template_ids_in_batch))
        cur.execute(
            f"SELECT template_id FROM vul_template WHERE template_id IN ({placeholders})",
            template_ids_in_batch,
        )
        existing_ids = {row[0] for row in cur.fetchall()}
    else:
        existing_ids = set()

    new_batch = [t for t in batch if t["template_id"] not in existing_ids]
    skipped = len(batch) - len(new_batch)
    stats["skipped"] += skipped

    if not new_batch:
        return

    # ── 2. 插入模板 ──
    # 批内去重（同一 batch 中可能有 YAML 模板 id 重复）
    seen_tids = set()
    deduped_batch = []
    for t in new_batch:
        if t["template_id"] not in seen_tids:
            seen_tids.add(t["template_id"])
            deduped_batch.append(t)
    batch_dup_count = len(new_batch) - len(deduped_batch)
    stats["skipped"] += batch_dup_count
    new_batch = deduped_batch

    template_rows = []
    for t in new_batch:
        name_val = t["name"] or t["template_id"]
        sev_val = t["severity"] or "unknown"
        if not name_val:
            print(f"  [WARN] 空名称模板: {t['file_path']}, template_id={t['template_id']}")
            name_val = t["template_id"]
        template_rows.append((
            t["template_id"], name_val, t["description"], t["author"],
            sev_val, t["cve_id"], t["cwe_id"], t["cvss_score"],
            t["epss_score"], t["flow"], t["variables"],
        ))

    sql = build_insert_sql("vul_template", TEMPLATE_COLS).replace(
        "INSERT INTO", "INSERT IGNORE INTO"
    )
    if not dry_run:
        cur.executemany(sql, template_rows)

        # 回查自增 ID
        new_ids = [t["template_id"] for t in new_batch]
        placeholders = ",".join(["%s"] * len(new_ids))
        cur.execute(
            f"SELECT id, template_id FROM vul_template WHERE template_id IN ({placeholders})",
            new_ids,
        )
        tid_to_db_id = {row[1]: row[0] for row in cur.fetchall()}
    else:
        # dry-run: 模拟 ID
        tid_to_db_id = {t["template_id"]: i + 1000000 for i, t in enumerate(new_batch)}

    stats["imported"] += len(new_batch)

    # ── 3. 预处理标签 ──
    all_tags = set()
    for t in new_batch:
        for tag in t["tags"]:
            all_tags.add(tag)

    new_tags = all_tags - set(tag_cache.keys())
    if new_tags and not dry_run:
        cur.executemany(
            "INSERT IGNORE INTO vul_tag (name) VALUES (%s)",
            [(tag,) for tag in new_tags],
        )
        # 回查新增标签的 ID
        placeholders = ",".join(["%s"] * len(new_tags))
        cur.execute(
            f"SELECT id, name FROM vul_tag WHERE name IN ({placeholders})",
            list(new_tags),
        )
        for row in cur.fetchall():
            tag_cache[row[1]] = row[0]
    elif new_tags and dry_run:
        for i, tag in enumerate(new_tags, 1):
            tag_cache[tag] = i + 2000000

    # 插入标签关联
    template_tag_rows = []
    for t in new_batch:
        db_id = tid_to_db_id[t["template_id"]]
        for tag in t["tags"]:
            tag_id = tag_cache.get(tag)
            if tag_id:
                template_tag_rows.append((db_id, tag_id))

    if template_tag_rows and not dry_run:
        cur.executemany(
            "INSERT IGNORE INTO vul_template_tag (template_id, tag_id) VALUES (%s, %s)",
            template_tag_rows,
        )
    stats["tags"] += len(template_tag_rows)

    # ── 4. 参考链接 ──
    reference_rows = []
    for t in new_batch:
        db_id = tid_to_db_id[t["template_id"]]
        for ref in t["references"]:
            reference_rows.append((db_id, ref["url"], ref["title"]))

    if reference_rows and not dry_run:
        cur.executemany(
            "INSERT INTO vul_reference (template_id, url, title) VALUES (%s, %s, %s)",
            reference_rows,
        )
    stats["references"] += len(reference_rows)

    # ── 5. HTTP 步骤 ──
    step_rows = []
    for t in new_batch:
        db_id = tid_to_db_id[t["template_id"]]
        for step in t["http_steps"]:
            step_rows.append((
                db_id, step["step_order"], step["method"], step["path"],
                step["headers"], step["body"], step["raw"], step["attack"],
                step["matchers_condition"], step["stop_at_first_match"],
            ))

    if step_rows and not dry_run:
        cur.executemany(build_insert_sql("vul_http_step", HTTP_STEP_COLS), step_rows)
    stats["http_steps"] += len(step_rows)

    # ── 6. Matchers ──
    matcher_rows = []
    for t in new_batch:
        db_id = tid_to_db_id[t["template_id"]]
        for m in t["matchers"]:
            matcher_rows.append((
                db_id, m["step_order"], m["type"], m["part"], m["condition"],
                m["negative"], m["case_insensitive"], m["internal"], m["name"], m["config"],
            ))

    if matcher_rows and not dry_run:
        cur.executemany(build_insert_sql("vul_matcher", MATCHER_COLS), matcher_rows)
    stats["matchers"] += len(matcher_rows)

    # ── 7. Extractors ──
    extractor_rows = []
    for t in new_batch:
        db_id = tid_to_db_id[t["template_id"]]
        for e in t["extractors"]:
            extractor_rows.append((
                db_id, e["step_order"], e["type"], e["part"], e["name"],
                e["config"], e["internal"], e["group_num"],
            ))

    if extractor_rows and not dry_run:
        cur.executemany(build_insert_sql("vul_extractor", EXTRACTOR_COLS), extractor_rows)
    stats["extractors"] += len(extractor_rows)

    # ── 8. 分类 ──
    category_name_counts = defaultdict(int)
    for t in new_batch:
        if t["category_dir"]:
            category_name_counts[t["category_dir"]] += 1

    # 确保分类存在
    for cat_name in category_name_counts:
        if cat_name not in category_cache:
            if not dry_run:
                cur.execute(
                    "INSERT IGNORE INTO vul_category (name) VALUES (%s)", (cat_name,)
                )
                cur.execute("SELECT category_id FROM vul_category WHERE name = %s", (cat_name,))
                row = cur.fetchone()
                if row:
                    category_cache[cat_name] = row[0]
            else:
                category_cache[cat_name] = len(category_cache) + 3000000

    template_cat_rows = []
    for t in new_batch:
        if t["category_dir"]:
            db_id = tid_to_db_id[t["template_id"]]
            cat_id = category_cache.get(t["category_dir"])
            if cat_id:
                template_cat_rows.append((db_id, cat_id))

    if template_cat_rows and not dry_run:
        cur.executemany(
            "INSERT IGNORE INTO vul_template_category (template_id, category_id) VALUES (%s, %s)",
            template_cat_rows,
        )
    stats["categories"] += len(template_cat_rows)


# ── 主流程 ─────────────────────────────────────────────

def collect_yaml_files(template_dir: Path, subdir: str | None) -> list[Path]:
    """收集所有 .yaml 文件。"""
    base = template_dir
    if subdir:
        base = template_dir / subdir
        if not base.exists():
            print(f"[FAIL] 目录不存在: {base}")
            sys.exit(1)

    yaml_files = sorted(base.rglob("*.yaml"))
    # 排除非模板文件
    yaml_files = [f for f in yaml_files
                  if not f.name.startswith(".") and "wappalyzer-mapping" not in f.name]
    return yaml_files


def import_templates(
    template_dir: Path,
    db_config: dict,
    subdir: str | None = None,
    dry_run: bool = False,
    stats_only: bool = False,
):
    """主导入逻辑。"""
    yaml_files = collect_yaml_files(template_dir, subdir)
    total = len(yaml_files)
    print(f"[*] 扫描目录: {template_dir}")
    if subdir:
        print(f"  子目录过滤: {subdir}")
    print(f"  发现 {total} 个 YAML 文件")
    print(f"  模式: {'DRY-RUN (不写库)' if dry_run else '正式导入'}")
    print()

    if stats_only:
        # 只做解析统计，不碰数据库
        stats = {"total": total, "http": 0, "skipped_protocol": 0, "parse_error": 0}
        for f in yaml_files:
            result = parse_template(f, template_dir)
            if result:
                stats["http"] += 1
            else:
                try:
                    with open(f, "r") as fh:
                        data = yaml.safe_load(fh)
                    if isinstance(data, dict) and "http" not in data:
                        stats["skipped_protocol"] += 1
                    else:
                        stats["parse_error"] += 1
                except Exception:
                    stats["parse_error"] += 1

        print(f"  总计: {stats['total']}")
        print(f"  含 http 协议: {stats['http']}")
        print(f"  非 http 协议: {stats['skipped_protocol']}")
        print(f"  解析失败: {stats['parse_error']}")
        return

    # ── 连接数据库 ──
    if not dry_run:
        conn = pymysql.connect(
            host=db_config["host"],
            port=db_config.get("port", 3306),
            user=db_config["user"],
            password=db_config["password"],
            database=db_config["database"],
            charset="utf8mb4",
            autocommit=False,
        )
        cur = conn.cursor()
    else:
        conn = None
        cur = None

    # 预热缓存
    tag_cache = {}
    category_cache = {}
    if not dry_run:
        cur.execute("SELECT id, name FROM vul_tag")
        for row in cur.fetchall():
            tag_cache[row[1]] = row[0]
        cur.execute("SELECT category_id, name FROM vul_category WHERE deleted = 0")
        for row in cur.fetchall():
            category_cache[row[1]] = row[0]

    stats = {
        "total": 0, "imported": 0, "skipped": 0, "failed": 0, "non_http": 0,
        "tags": 0, "references": 0, "http_steps": 0,
        "matchers": 0, "extractors": 0, "categories": 0,
    }

    batch = []
    start_time = time.time()

    for i, yaml_path in enumerate(yaml_files):
        # 先用轻量检测过滤非 http 模板，避免计入 failed
        try:
            with open(yaml_path, "r", encoding="utf-8") as f:
                if "http:" not in f.read():
                    stats["non_http"] += 1
                    continue
        except Exception:
            pass

        result = parse_template(yaml_path, template_dir)
        if result is None:
            stats["failed"] += 1
        else:
            batch.append(result)
            stats["total"] += 1

        # 分批写入
        if len(batch) >= BATCH_SIZE:
            flush_batch(cur, batch, tag_cache, category_cache, dry_run, stats)
            if not dry_run and conn:
                conn.commit()
            elapsed = time.time() - start_time
            print(f"  [{stats['total']}/{total}] "
                  f"导入 {stats['imported']} · 跳过 {stats['skipped']} · "
                  f"失败 {stats['failed']} · 非http {stats['non_http']} · {elapsed:.1f}s")
            batch.clear()

    # 收尾
    if batch:
        flush_batch(cur, batch, tag_cache, category_cache, dry_run, stats)
        if not dry_run and conn:
            conn.commit()

    elapsed = time.time() - start_time

    # ── 输出统计 ──
    print()
    print("=" * 60)
    print("导入完成" if not dry_run else "DRY-RUN 完成（未写入数据库）")
    print(f"  耗时: {elapsed:.1f}s")
    print(f"  模板总计: {stats['total']}")
    print(f"  成功导入: {stats['imported']}")
    print(f"  跳过(已存在): {stats['skipped']}")
    print(f"  非http协议: {stats['non_http']}")
    print(f"  解析失败: {stats['failed']}")
    print(f"  ---")
    print(f"  vul_tag 关联: {stats['tags']}")
    print(f"  vul_reference: {stats['references']}")
    print(f"  vul_http_step: {stats['http_steps']}")
    print(f"  vul_matcher: {stats['matchers']}")
    print(f"  vul_extractor: {stats['extractors']}")
    print(f"  vul_template_category: {stats['categories']}")
    print(f"  标签字典: {len(tag_cache)}")
    print(f"  分类目录: {len(category_cache)}")
    print("=" * 60)

    if not dry_run and conn:
        cur.close()
        conn.close()


# ── 入口 ────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Nuclei YAML 模板 → 新 vul 表结构 批量导入"
    )
    parser.add_argument("--db-host", default=os.environ.get("DB_HOST", "localhost"))
    parser.add_argument("--db-port", type=int, default=int(os.environ.get("DB_PORT", 3306)))
    parser.add_argument("--db-user", default=os.environ.get("DB_USER", "root"))
    parser.add_argument("--db-pass", default=os.environ.get("DB_PASS", ""))
    parser.add_argument("--db-name", default=os.environ.get("DB_NAME", "hawkeye_cloud"))
    parser.add_argument(
        "--template-dir",
        default=os.environ.get(
            "TEMPLATE_DIR",
            str(Path(__file__).resolve().parent.parent / "ddocs" / "http"),
        ),
        help="Nuclei YAML 模板目录",
    )
    parser.add_argument("--category", default=None, help="只导入指定子目录，如 cves/2024")
    parser.add_argument("--dry-run", action="store_true", help="只解析不写入数据库")
    parser.add_argument("--stats-only", action="store_true", help="只统计不导入")
    args = parser.parse_args()

    template_dir = Path(args.template_dir)
    if not template_dir.exists():
        print(f"[FAIL] 模板目录不存在: {template_dir}")
        sys.exit(1)

    db_config = {
        "host": args.db_host,
        "port": args.db_port,
        "user": args.db_user,
        "password": args.db_pass,
        "database": args.db_name,
    }

    import_templates(
        template_dir=template_dir,
        db_config=db_config,
        subdir=args.category,
        dry_run=args.dry_run,
        stats_only=args.stats_only,
    )


if __name__ == "__main__":
    main()
