/*
 *     <one line to give the program's name and a brief idea of what it does.>
 *     Copyright (C) 2024  ideal-state
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package team.idealstate.minecraft.spigot.libraryloader;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.eclipse.aether.artifact.Artifact;
import team.idealstate.minecraft.spigot.libraryloader.annotation.DirectDependency;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>ProxyJavaPluginLoader</p>
 *
 * <p>创建于 2024/2/15 20:40</p>
 *
 * @author ketikai
 * @version 1.0.1
 * @since 1.0.0
 */
final class ProxyJavaPluginLoader implements PluginLoader {

    private static final Logger logger = LogManager.getLogger(ProxyJavaPluginLoader.class);
    private static final Class<?> PCL_CLASS;
    private static final Field PCL_CLASSES_FIELD;

    static {
        try {
            PCL_CLASS = Class.forName("org.bukkit.plugin.java.PluginClassLoader",
                    false, ProxyJavaPluginLoader.class.getClassLoader());
            PCL_CLASSES_FIELD = PCL_CLASS.getDeclaredField("classes");
            Field modifiersField = Field.class.getDeclaredField("modifiers"); //①
            modifiersField.setAccessible(true);
            modifiersField.setInt(PCL_CLASSES_FIELD,
                    PCL_CLASSES_FIELD.getModifiers() & ~Modifier.FINAL);
            PCL_CLASSES_FIELD.setAccessible(true);
        } catch (ClassNotFoundException e) {
            logger.error("不支持的环境（不存在 PluginClassLoader 类）");
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            logger.error("不支持的环境（未找到 PluginClassLoader#classes 字段）");
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.error("不支持的环境（无法将 PluginClassLoader#classes 字段设为非 final）");
            throw new RuntimeException(e);
        }
    }

    private final PluginLoader pluginLoader;

    @SuppressWarnings({"deprecation"})
    public ProxyJavaPluginLoader(Server server) {
        this.pluginLoader = new JavaPluginLoader(server);
    }

    public ProxyJavaPluginLoader(JavaPluginLoader javaPluginLoader) {
        this.pluginLoader = javaPluginLoader;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        Plugin loadedPlugin = pluginLoader.loadPlugin(file);
        Class<? extends Plugin> pluginClass = loadedPlugin.getClass();
        ClassLoader classLoader = pluginClass.getClassLoader();
        String pluginName = loadedPlugin.getName();
        if (!(classLoader instanceof URLClassLoader)) {
            logger.error("[{}] 使用了不受支持的类加载器 '{}'", pluginName, classLoader);
            return loadedPlugin;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(loadedPlugin.getResource("plugin.yml")));
        List<String> libraries = config.getStringList("libraries");
        if (libraries.isEmpty()) {
            logger.info("[{}] 无依赖项，跳过依赖项加载", pluginName);
            return loadedPlugin;
        }

        logger.info("[{}] 预计加载 {} 项依赖", pluginName, libraries.size());

        int resolvedSize;
        try {
            URLClassLoader ucl = (URLClassLoader) classLoader;
            List<Artifact> resolved = MavenResolver.resolve(libraries);
            Set<String> directDependencyIds =
                    Arrays.stream(pluginClass
                                    .getDeclaredAnnotationsByType(DirectDependency.class))
                            .map(DirectDependency::value)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toSet());
            resolvedSize = resolved.size();
            Iterator<Artifact> artifactIterator = resolved.iterator();
            while (artifactIterator.hasNext()) {
                Artifact artifact = artifactIterator.next();
                String dependencyId = artifact.getGroupId() + ":" +
                        artifact.getArtifactId() + ":" +
                        artifact.getVersion();
                if (directDependencyIds.contains(dependencyId)) {
                    continue;
                }
                logger.info("[{}] 加载依赖 {}", pluginName, dependencyId);
                UclUtils.addFile(ucl, artifact.getFile());
                artifactIterator.remove();
            }
            if (!resolved.isEmpty()) {
                URLClassLoader urlClassLoader = new URLClassLoader(new URL[0], ucl);
                for (Artifact artifact : resolved) {
                    logger.info("[{}] 加载直接依赖 {}:{}:{}", pluginName,
                            artifact.getGroupId(), artifact.getArtifactId(),
                            artifact.getVersion()
                    );
                    UclUtils.addFile(urlClassLoader, artifact.getFile());
                }
                DirectDependencyCache cache = new DirectDependencyCache(urlClassLoader);
                cache.putAll((Map<String, Class<?>>) PCL_CLASSES_FIELD.get(ucl));
                PCL_CLASSES_FIELD.set(ucl, cache);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("[{}] 已加载共计 {} 项依赖", pluginName, resolvedSize);
        return loadedPlugin;
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        return pluginLoader.getPluginDescription(file);
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return pluginLoader.getPluginFileFilters();
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return pluginLoader.createRegisteredListeners(listener, plugin);
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        pluginLoader.enablePlugin(plugin);
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        pluginLoader.disablePlugin(plugin);
    }
}
