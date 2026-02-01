package org.yax.yapiplug.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(name = "YapiPluginConfig", storages = @Storage("yapiPlugin.xml"))
public final class PluginConfig implements PersistentStateComponent<PluginConfig.State> {

    public enum Provider {
        DEEPSEEK("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat", 
                new String[]{"deepseek-chat", "deepseek-coder"}),
        GLM("GLM (智谱AI)", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4",
                new String[]{"glm-4", "glm-4-plus", "glm-4-0520", "glm-4-long", "glm-4-airx", "glm-4-flash"}),
        QWEN("千问 (通义千问)", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus",
                new String[]{"qwen-plus", "qwen-turbo", "qwen-max", "qwen-max-longcontext"}),
        NVIDIA("英伟达 (NVIDIA)", "https://integrate.api.nvidia.com/v1/chat/completions", "nvidia/llama-3.1-nemotron-70b-instruct",
                new String[]{"nvidia/llama-3.1-nemotron-70b-instruct", "meta/llama-3.1-405b-instruct", "meta/llama-3.1-70b-instruct", 
                           "meta/llama-3.1-8b-instruct", "mistralai/mixtral-8x7b-instruct-v0.1", "microsoft/phi-3-medium-128k-instruct",
                           "kimi", "glm-4", "glm-4-plus", "glm-4-0520", "glm-4-long", "glm-4-airx", "glm-4-flash"});

        private final String displayName;
        private final String defaultApiUrl;
        private final String defaultModel;
        private final String[] availableModels;

        Provider(String displayName, String defaultApiUrl, String defaultModel, String[] availableModels) {
            this.displayName = displayName;
            this.defaultApiUrl = defaultApiUrl;
            this.defaultModel = defaultModel;
            this.availableModels = availableModels;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultApiUrl() {
            return defaultApiUrl;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public String[] getAvailableModels() {
            return availableModels;
        }
    }

    public static class State {
        public String provider = Provider.DEEPSEEK.name();
        public String apiKey = "";
        public String apiUrl = "https://api.deepseek.com/chat/completions";
        public String model = "deepseek-chat";
    }

    private State myState = new State();

    public static PluginConfig getInstance(com.intellij.openapi.project.Project project) {
        return project.getService(PluginConfig.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public String getApiKey() {
        return myState.apiKey;
    }

    public void setApiKey(String apiKey) {
        myState.apiKey = apiKey;
    }

    public String getApiUrl() {
        return myState.apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        myState.apiUrl = apiUrl;
    }

    public String getModel() {
        return myState.model;
    }

    public void setModel(String model) {
        myState.model = model;
    }

    public String getProvider() {
        return myState.provider;
    }

    public void setProvider(String provider) {
        myState.provider = provider;
    }

    public Provider getProviderEnum() {
        try {
            return Provider.valueOf(myState.provider);
        } catch (IllegalArgumentException e) {
            return Provider.DEEPSEEK;
        }
    }
}
