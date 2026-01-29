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
        DEEPSEEK("DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat"),
        GLM("GLM (智谱AI)", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4"),
        QWEN("千问 (通义千问)", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-plus");

        private final String displayName;
        private final String defaultApiUrl;
        private final String defaultModel;

        Provider(String displayName, String defaultApiUrl, String defaultModel) {
            this.displayName = displayName;
            this.defaultApiUrl = defaultApiUrl;
            this.defaultModel = defaultModel;
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
