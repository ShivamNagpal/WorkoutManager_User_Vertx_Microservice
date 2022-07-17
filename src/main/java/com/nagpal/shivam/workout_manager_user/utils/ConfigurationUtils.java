package com.nagpal.shivam.workout_manager_user.utils;

import com.nagpal.shivam.workout_manager_user.enums.Configuration;
import com.nagpal.shivam.workout_manager_user.enums.ResponseMessage;
import com.nagpal.shivam.workout_manager_user.exceptions.AppException;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationUtils {
    private static final Logger logger = Logger.getLogger(ConfigurationUtils.class.getName());

    private ConfigurationUtils() {
    }

    public static Future<JsonObject> getConfiguration(Vertx vertx) {
        return getEnvConfig(vertx)
                .compose(envConfig -> getFileConfig(vertx).map(fileConfig -> envConfig.mergeIn(fileConfig, true)))
                .compose(mergedConfig -> getSysConfig(vertx).map(sysConfig -> mergedConfig.mergeIn(sysConfig, true)))
                .map(mergedConfig -> {
                    normalizeKeys(mergedConfig);
                    return mergedConfig;
                });
    }

    public static String[] validateMandatoryConfigs(JsonObject config) {
        LinkedList<String> keys = new LinkedList<>();
        for (Configuration configuration : Configuration.MANDATORY_CONFIGURATIONS) {
            if (!config.containsKey(configuration.getKey())) {
                keys.add(configuration.getKey());
            }
        }
        return keys.toArray(new String[0]);
    }

    private static void normalizeKeys(JsonObject config) {
        for (Configuration configuration : Configuration.values()) {
            Configuration.normalizeConfigurationKey(config, configuration);
        }
    }

    private static Future<JsonObject> getEnvConfig(Vertx vertx) {
        ConfigStoreOptions configEnvOptions = new ConfigStoreOptions()
                .setType(Constants.ENV);
        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(configEnvOptions);
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        return configRetriever.getConfig()
                .map(config -> {
                    correctCasingForConfig(config, String::toUpperCase);
                    return config;
                });
    }

    private static Future<JsonObject> getFileConfig(Vertx vertx) {
        ConfigStoreOptions configFileOptions = new ConfigStoreOptions()
                .setType(Constants.FILE)
                .setConfig(new JsonObject().put(Constants.PATH, Constants.CONFIG_JSON_PATH));

        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(configFileOptions);
        String[] profiles = checkForConfigurationProfiles();
        if (profiles.length != 0) {
            String message = ResponseMessage.VERTX_ACTIVE_PROFILES.getMessage(Arrays.toString(profiles));
            logger.log(Level.INFO, message);
            for (String profile : profiles) {
                String path = MessageFormat.format(Constants.CONFIG_PROFILE_JSON_PATH, profile);
                ConfigStoreOptions configProfileFileOptions = new ConfigStoreOptions()
                        .setType(Constants.FILE)
                        .setOptional(true)
                        .setConfig(new JsonObject().put(Constants.PATH, path));
                configRetrieverOptions.addStore(configProfileFileOptions);
            }
        }

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        return configRetriever.getConfig();
    }

    private static Future<JsonObject> getSysConfig(Vertx vertx) {
        ConfigStoreOptions configSysOptions = new ConfigStoreOptions()
                .setType(Constants.SYS);
        ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions().addStore(configSysOptions);

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, configRetrieverOptions);
        return configRetriever.getConfig().map(config -> {
            correctCasingForConfig(config, String::toLowerCase);
            return config;
        });
    }

    private static String[] checkForConfigurationProfiles() {
        Optional<String> sysProfileFlagOptional = System.getProperties().keySet().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(key -> key.equalsIgnoreCase(Configuration.VERTX_PROFILES_ACTIVE.getKey()))
                .findAny();
        String[] profiles = new String[0];
        if (sysProfileFlagOptional.isPresent()) {
            profiles = System.getProperty(sysProfileFlagOptional.get())
                    .split(Constants.PROFILES_SEPARATOR_REGEX_PATTERN);
        } else {
            Optional<Map.Entry<String, String>> envProfileFlagOptional = System.getenv().entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(Constants.ENV_KEY_VERTX_PROFILES_ACTIVE)).findAny();
            if (envProfileFlagOptional.isPresent()) {
                profiles = envProfileFlagOptional.get().getValue().split(Constants.PROFILES_SEPARATOR_REGEX_PATTERN);
            }
        }
        return profiles;
    }

    private static void correctCasingForConfig(JsonObject config, UnaryOperator<String> configMapper) {
        Set<String> duplicateKeys = new HashSet<>();
        new LinkedList<>(config.fieldNames()).forEach(key -> {
            Object value = config.getValue(key);
            String newKey = configMapper.apply(key);
            config.remove(key);
            if (config.containsKey(newKey)) {
                duplicateKeys.add(newKey);
            }
            config.put(newKey, value);
        });
        if (!duplicateKeys.isEmpty()) {
            ResponseMessage responseMessage = ResponseMessage.DUPLICATE_CONFIG_KEYS_PROVIDED;
            throw new AppException(responseMessage.getMessageCode(), responseMessage.getMessage(duplicateKeys));
        }
    }
}
