package com.divination.liuyao.util;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.util.Map;

public class FreemarkerUtil {

    private static final Configuration cfg;

    static {
        cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setClassForTemplateLoading(FreemarkerUtil.class, "/templates");
    }

    public static String render(String templateName, Map<String, Object> params) {
        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(params, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("渲染 FreeMarker 模板失败: " + templateName, e);
        }
    }
}
