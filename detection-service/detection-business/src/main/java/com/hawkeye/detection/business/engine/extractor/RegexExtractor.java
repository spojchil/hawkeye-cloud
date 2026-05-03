package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexExtractor implements Extractor {

    @Override public String type() { return "regex"; }

    @Override
    public String extract(HttpResponseContext ctx, ExtractorDef def) {
        List<String> patterns = def.getRegex();
        if (patterns == null || patterns.isEmpty()) return null;

        String target = "header".equals(def.getPart())
                ? (ctx.getHeaders() != null ? ctx.getHeaders().toString() : "")
                : (ctx.getBody() != null ? ctx.getBody() : "");
        if (target == null) return null;

        int group = def.getGroupNum() != null ? def.getGroupNum() : 0;
        for (String pattern : patterns) {
            Matcher m = Pattern.compile(pattern, Pattern.DOTALL).matcher(target);
            if (m.find()) {
                return group <= m.groupCount() ? m.group(group) : m.group();
            }
        }
        return null;
    }
}
