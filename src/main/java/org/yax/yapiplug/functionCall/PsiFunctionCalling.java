package org.yax.yapiplug.functionCall;

import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;

public class PsiFunctionCalling implements FunctionCalling{
    @Override
    public String getName() {
        return "read java code";
    }

    @Override
    public String getDescription() {
        return "通过 idea PSI 读取java code";
    }

    @Override
    public JSONObject getInput_schema() {
        return JSONObject.parseObject(
                "{\"type\": \"object\", \"properties\": {\"className\": {\"type\": \"string\"}, \"methodName\": {\"type\": \"String\"}, \"argsNames\": {\"type\": \"array\",\"description\": \"参数名称数组\",\"items\":{\"type\":\"String\"}}}, \"required\": [\"className\"]}"
        );
    }

    @Override
    public String executeFunction(JSONObject input) {
        String className = input.getString("className");
        String methodName = input.getString("methodName");
        String[] argsNames = null;
        
        if (input.getJSONArray("argsNames") != null) {
            argsNames = input.getJSONArray("argsNames").toArray(new String[input.getJSONArray("argsNames").size()]);
        }
        
        try {
            // 获取当前项目
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 0) {
                return buildErrorResponse("没有打开的项目");
            }

            Project project = projects[0];
            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);

            // 根据类名查找类
            PsiClass psiClass = javaPsiFacade.findClass(className, GlobalSearchScope.projectScope(project));
            if (psiClass == null) {
                return buildErrorResponse("找不到类: " + className);
            }

            // 如果 methodName 为空，返回整个类的代码
            if (methodName == null || methodName.trim().isEmpty()) {
                String classCode = getClassCode(psiClass);
                return buildSuccessResponse(classCode);
            }

            // 根据方法名和参数名查找方法
            PsiMethod method = findMethodByNameAndParameters(psiClass, methodName, argsNames);
            if (method == null) {
                return buildErrorResponse("找不到方法: " + methodName + 
                        (argsNames != null ? " 参数: " + java.util.Arrays.toString(argsNames) : ""));
            }

            String methodCode = getMethodCode(method);
            return buildSuccessResponse(methodCode);

        } catch (Exception e) {
            return buildErrorResponse("执行出错: " + e.getMessage());
        }
    }

    /**
     * 根据方法名和参数名查找方法
     * @param psiClass 类
     * @param methodName 方法名
     * @param parameterNames 参数名数组（可为空）
     * @return 匹配的方法，如果没有找到则返回 null
     */
    private PsiMethod findMethodByNameAndParameters(PsiClass psiClass, String methodName, String[] parameterNames) {
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        
        if (methods.length == 0) {
            return null;
        }

        // 如果没有指定参数名，返回第一个匹配的方法
        if (parameterNames == null || parameterNames.length == 0) {
            return methods[0];
        }

        // 根据参数名匹配方法
        for (PsiMethod method : methods) {
            if (matchMethodParameters(method, parameterNames)) {
                return method;
            }
        }

        // 如果没有完全匹配的，返回第一个方法
        return methods[0];
    }

    /**
     * 检查方法的参数是否与给定的参数名数组匹配
     * @param method 方法
     * @param parameterNames 参数名数组
     * @return 是否匹配
     */
    private boolean matchMethodParameters(PsiMethod method, String[] parameterNames) {
        PsiParameter[] parameters = method.getParameterList().getParameters();

        // 参数个数不匹配
        if (parameters.length != parameterNames.length) {
            return false;
        }

        // 检查每个参数名是否匹配
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].getName().equals(parameterNames[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取类的完整代码（包括注释）
     */
    private String getClassCode(PsiClass psiClass) {
        StringBuilder sb = new StringBuilder();

        // 获取类的文档注释
        PsiDocComment docComment = psiClass.getDocComment();
        if (docComment != null) {
            sb.append(docComment.getText()).append("\n");
        }

        // 获取类的完整代码
        sb.append(psiClass.getText());

        return sb.toString();
    }

    /**
     * 获取方法的代码（包括注释）
     */
    private String getMethodCode(PsiMethod method) {
        StringBuilder sb = new StringBuilder();

        // 获取方法的文档注释
        PsiDocComment docComment = method.getDocComment();
        if (docComment != null) {
            sb.append(docComment.getText()).append("\n");
        }

        // 获取方法的完整代码
        sb.append(method.getText());

        return sb.toString();
    }

    /**
     * 构建成功响应
     */
    private String buildSuccessResponse(String code) {
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("code", code);
        return result.toJSONString();
    }

    /**
     * 构建错误响应
     */
    private String buildErrorResponse(String error) {
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("error", error);
        return result.toJSONString();
    }
}
