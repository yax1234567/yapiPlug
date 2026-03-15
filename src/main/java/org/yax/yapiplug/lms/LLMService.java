package org.yax.yapiplug.lms;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.yax.yapiplug.config.PluginConfig;
import org.yax.yapiplug.util.YapiUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
public class LLMService {

    public static void sendToAI(Project project, String context) {
        System.out.println(context);
        PluginConfig config = PluginConfig.getInstance(project);
        String providerName = config.getProviderEnum().getDisplayName();
        String modelName=config.getModel();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, providerName+"_"+modelName + " 正在深入分析中...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String response = callLLMAPI(context, project);

                    // 调用大模型后，检查是否有 YAPI cookie，没有则登录
                    String yapiCookie = checkAndLoginYapi(project);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (yapiCookie != null && !yapiCookie.isEmpty()) {
                            Messages.showInfoMessage(project, response + "\n\n[YAPI 登录成功]", providerName + " 代码分析报告");
                        } else {
                            Messages.showInfoMessage(project, response, providerName + " 代码分析报告");
                        }
                    });
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, providerName + " 调用失败: " + e.getMessage(), "网络请求错误");
                    });
                }
            }
        });
    }

    private static String callLLMAPI(String promptContent, Project project) throws Exception {
        PluginConfig config = PluginConfig.getInstance(project);
        PluginConfig.Provider provider = config.getProviderEnum();
        String apiKey = config.getApiKey();
        String apiUrl = config.getApiUrl();
        String model = config.getModel();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("API Key 未配置，请在 Settings > Tools > Yapi Plugin 中配置");
        }

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        // 构建请求体
        JsonObject root = new JsonObject();

        root.addProperty("model", model != null && !model.trim().isEmpty() ? model : provider.getDefaultModel());

        JsonArray messages = new JsonArray();

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "你是一个 Spring Boot 专家。请分析 Controller 代码，" +
                "提取：1. 请求 URL 2. HTTP 方法 3. 入参字段(含 DTO 内部字段) 4. 返回值字段。请用 Markdown 表格展示。 5.接口方法简介尽量简短");
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", promptContent);
        messages.add(userMsg);

        root.add("messages", messages);
        root.addProperty("stream", false);
        root.addProperty("temperature", 0.7); // 采样温度

        // 发送数据
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = new Gson().toJson(root).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 解析响应
        int code = conn.getResponseCode();
        if (code == 200) {
            Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
            String responseStr = scanner.useDelimiter("\\A").next();
            scanner.close();

            JsonObject responseJson = new Gson().fromJson(responseStr, JsonObject.class);
            return responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        } else {
            // 获取错误流信息
            Scanner errorScanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8);
            String errorMsg = errorScanner.hasNext() ? errorScanner.useDelimiter("\\A").next() : "Unknown Error";
            throw new RuntimeException("HTTP " + code + ": " + errorMsg);
        }
    }

    /**
     * 检查是否有 YAPI cookie，没有则调用 loginYapi 登录
     */
    private static String checkAndLoginYapi(Project project) {
        PluginConfig config = PluginConfig.getInstance(project);
        String yapiCookie = config.getYapiCookie();

        // 如果已经有 cookie，直接返回
        if (yapiCookie != null && !yapiCookie.isEmpty()) {
            System.out.println("使用已存储的 YAPI Cookie");
            return yapiCookie;
        }

        String yapiUsername = config.getYapiUsername();
        String yapiPassword = config.getYapiPassword();

        // 检查是否配置了 YAPI 账号和密码
        if (yapiUsername == null || yapiUsername.trim().isEmpty() || 
            yapiPassword == null || yapiPassword.trim().isEmpty()) {
            System.out.println("YAPI 账号或密码未配置");
            return null;
        }

        try {
            // 调用 loginYapi 获取 cookie
            Map<String, String> loginResult = YapiUtil.loginYapi(yapiUsername, yapiPassword);
            String newCookie = loginResult.get("Cookie");
            
            if (newCookie != null && !newCookie.isEmpty()) {
                // 将 cookie 存储到配置中
                config.setYapiCookie(newCookie);
                System.out.println("YAPI 登录成功，获取到 Cookie: " + newCookie);
                return newCookie;
            } else {
                System.out.println("YAPI 登录失败，未获取到 Cookie");
                return null;
            }
        } catch (Exception e) {
            System.out.println("YAPI 登录异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
