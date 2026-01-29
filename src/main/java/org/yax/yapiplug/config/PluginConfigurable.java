package org.yax.yapiplug.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class PluginConfigurable implements Configurable {

    private final Project project;
    private JComboBox<String> providerComboBox;
    private JBTextField apiKeyField;
    private JBTextField apiUrlField;
    private JBTextField modelField;
    private JPanel mainPanel;

    public PluginConfigurable(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Yapi Plugin";
    }

    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.openapi.ui.VerticalFlowLayout(0, 10));

        String[] providers = new String[PluginConfig.Provider.values().length];
        for (int i = 0; i < PluginConfig.Provider.values().length; i++) {
            providers[i] = PluginConfig.Provider.values()[i].getDisplayName();
        }

        providerComboBox = new JComboBox<>(providers);
        apiKeyField = new JBTextField();
        apiUrlField = new JBTextField();
        modelField = new JBTextField();

        com.intellij.openapi.ui.LabeledComponent<JComboBox<String>> providerPanel = new com.intellij.openapi.ui.LabeledComponent<>();
        providerPanel.setText("模型提供商:");
        providerPanel.setComponent(providerComboBox);

        com.intellij.openapi.ui.LabeledComponent<JBTextField> apiKeyPanel = new com.intellij.openapi.ui.LabeledComponent<>();
        apiKeyPanel.setText("API Key:");
        apiKeyPanel.setComponent(apiKeyField);

        com.intellij.openapi.ui.LabeledComponent<JBTextField> apiUrlPanel = new com.intellij.openapi.ui.LabeledComponent<>();
        apiUrlPanel.setText("API URL:");
        apiUrlPanel.setComponent(apiUrlField);

        com.intellij.openapi.ui.LabeledComponent<JBTextField> modelPanel = new com.intellij.openapi.ui.LabeledComponent<>();
        modelPanel.setText("Model:");
        modelPanel.setComponent(modelField);

        mainPanel.add(providerPanel);
        mainPanel.add(apiKeyPanel);
        mainPanel.add(apiUrlPanel);
        mainPanel.add(modelPanel);

        providerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                int selectedIndex = providerComboBox.getSelectedIndex();
                PluginConfig.Provider provider = PluginConfig.Provider.values()[selectedIndex];
                apiUrlField.setText(provider.getDefaultApiUrl());
                modelField.setText(provider.getDefaultModel());
            }
        });

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        PluginConfig config = PluginConfig.getInstance(project);
        int selectedIndex = providerComboBox.getSelectedIndex();
        PluginConfig.Provider selectedProvider = PluginConfig.Provider.values()[selectedIndex];
        return !selectedProvider.name().equals(config.getProvider()) ||
               !apiKeyField.getText().equals(config.getApiKey()) ||
               !apiUrlField.getText().equals(config.getApiUrl()) ||
               !modelField.getText().equals(config.getModel());
    }

    @Override
    public void apply() throws ConfigurationException {
        PluginConfig config = PluginConfig.getInstance(project);
        int selectedIndex = providerComboBox.getSelectedIndex();
        PluginConfig.Provider selectedProvider = PluginConfig.Provider.values()[selectedIndex];
        config.setProvider(selectedProvider.name());
        config.setApiKey(apiKeyField.getText());
        config.setApiUrl(apiUrlField.getText());
        config.setModel(modelField.getText());
    }

    @Override
    public void reset() {
        PluginConfig config = PluginConfig.getInstance(project);
        PluginConfig.Provider provider = config.getProviderEnum();
        for (int i = 0; i < PluginConfig.Provider.values().length; i++) {
            if (PluginConfig.Provider.values()[i] == provider) {
                providerComboBox.setSelectedIndex(i);
                break;
            }
        }
        apiKeyField.setText(config.getApiKey());
        apiUrlField.setText(config.getApiUrl());
        modelField.setText(config.getModel());
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return apiKeyField;
    }
}
