package org.yax.yapiplug.action;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.yax.yapiplug.lms.LLMService;

import java.util.HashSet;
import java.util.Set;

public class AnalyzeAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || editor == null || psiFile == null) return;

        // 获取光标处的方法
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);

        if (method == null) {
            System.out.println("请将光标置于 Controller 方法内");
            return;
        }

        // 构建发送给大模型的上下文
        String context = buildContext(method);

        // 异步调用大模型（避免界面卡顿）
        LLMService.sendToAI(project, context);
    }

    private String buildContext(PsiMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 分析请求：Spring Boot Controller\n");
        // 1. 获取所属类
        PsiClass psiClass = method.getContainingClass();

        String baseUrl = getMappingPath(psiClass); // 获取类上的 RequestMapping
        sb.append("控制器类: ").append(psiClass.getQualifiedName()).append("\n");


        sb.append("方法名: ").append(method.getName()).append("\n");

        // 2. 获取方法上的 Mapping
        String methodUrl = getMappingPath(method);

        // 3. 拼接完整 URL
        String fullUrl = (baseUrl + "/" + methodUrl);
        sb.append("完整请求路径: ").append(fullUrl.isEmpty() ? "/" : fullUrl).append("\n");

        // 4. 获取 HTTP 请求方法 (GET/POST 等)
        sb.append("请求方式: ").append(getHttpMethod(method)).append("\n");

        // 1. 提取注释
        if (method.getDocComment() != null) {
            sb.append("方法注释: ").append(method.getDocComment().getText()).append("\n");
        }

        // 2. 提取方法源码
        sb.append("源代码: \n").append(method.getText()).append("\n\n");

        // 3. 递归解析入参字段 (实现“精确到字段”)
        sb.append("入参详细结构:\n");
        for (PsiParameter param : method.getParameterList().getParameters()) {
            sb.append(" - 参数名: ").append(param.getName())
                    .append(", 类型: ").append(param.getType().getPresentableText()).append("\n");
            appendClassFields(param.getType(), sb);
        }

        // 4. 解析返回值字段
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            sb.append("返回值详细结构 (").append(returnType.getPresentableText()).append("):\n");
            appendClassFields(returnType, sb);
        }
        sb.append("\n=== 调用的下一层 (Service) 方法源码 ===\n");
        appendCalledMethodsSource(method, sb);

        return sb.toString();
    }

    private void appendClassFields(PsiType type, StringBuilder sb) {
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            // 过滤掉 Java 核心类，只分析自定义 DTO/Entity
            if (psiClass != null && !psiClass.getQualifiedName().startsWith("java.")) {
                for (PsiField field : psiClass.getAllFields()) {
                    sb.append("    * 字段: ").append(field.getName())
                            .append(", 类型: ").append(field.getType().getPresentableText());
                    if(field.getDocComment() != null){
                        sb.append(", 注释: ").append(field.getDocComment().getText());
                    }
                    sb.append("\n");
                }
            }
        }
    }

    /**
     * 获取类或方法上的 Mapping 路径
     */
    private String getMappingPath(PsiModifierListOwner element) {
        // 常见的 Spring 映射注解
        String[] annotations = {
                "org.springframework.web.bind.annotation.RequestMapping",
                "org.springframework.web.bind.annotation.GetMapping",
                "org.springframework.web.bind.annotation.PostMapping",
                "org.springframework.web.bind.annotation.PutMapping",
                "org.springframework.web.bind.annotation.DeleteMapping"
        };

        for (String annotationName : annotations) {
            PsiAnnotation annotation = element.getAnnotation(annotationName);
            if (annotation != null) {
                // 获取 value 或 path 属性
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (value == null) value = annotation.findAttributeValue("path");

                if (value != null) {
                    String text = value.getText().replace("\"", "");
                    // 如果有多个路径，简单取第一个，或者返回数组字符串
                    return text.startsWith("{") ? text : text;
                }
            }
        }
        return "";
    }

    /**
     * 识别 HTTP 方法
     */
    private String getHttpMethod(PsiMethod method) {
        if (method.hasAnnotation("org.springframework.web.bind.annotation.GetMapping")) return "GET";
        if (method.hasAnnotation("org.springframework.web.bind.annotation.PostMapping")) return "POST";
        if (method.hasAnnotation("org.springframework.web.bind.annotation.PutMapping")) return "PUT";
        if (method.hasAnnotation("org.springframework.web.bind.annotation.DeleteMapping")) return "DELETE";
        return "ALL";
    }

    /**
     * 获取方法内调用的其他方法（如 service.doSomething()）的实现源码
     */
    private void appendCalledMethodsSource(PsiMethod method, StringBuilder sb) {
        Set<String> processedMethods = new HashSet<>();
        // 遍历方法体内的所有元素
        method.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                // 找到调用的具体方法定义
                PsiMethod callee = expression.resolveMethod();

                if (callee != null && isProjectSource(callee)) {
                    String methodKey = callee.getContainingClass().getQualifiedName() + "#" + callee.getName();
                    if (!processedMethods.contains(methodKey)) {
                        sb.append("/* 来源类: ").append(callee.getContainingClass().getName()).append(" */\n");
                        sb.append(callee.getText()).append("\n\n");
                        processedMethods.add(methodKey);
                    }
                }
            }
        });
    }

    private boolean isProjectSource(PsiMethod method) {
        return method.getManager().isInProject(method);
    }
}
