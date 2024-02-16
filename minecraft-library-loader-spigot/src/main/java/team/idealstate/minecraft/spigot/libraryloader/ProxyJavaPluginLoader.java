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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>ProxyJavaPluginLoader</p>
 *
 * <p>创建于 2024/2/15 20:40</p>
 *
 * @author ketikai
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProxyJavaPluginLoader implements PluginLoader {

    private static final Logger logger = LogManager.getLogger(ProxyJavaPluginLoader.class);
    private final PluginLoader pluginLoader;

    @SuppressWarnings({"deprecation"})
    public ProxyJavaPluginLoader(Server server) {
        this.pluginLoader = new JavaPluginLoader(server);
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        Plugin loadedPlugin = pluginLoader.loadPlugin(file);
        ClassLoader classLoader = loadedPlugin.getClass().getClassLoader();
        String pluginName = loadedPlugin.getName();
        if (!(classLoader instanceof URLClassLoader)) {
            logger.error("[{}] 使用了不受支持的类加载器 '{}'", pluginName, classLoader);
            return loadedPlugin;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(loadedPlugin.getResource("/plugin.yml")));
        List<String> libraries = config.getStringList("libraries");
        if (libraries.isEmpty()) {
            logger.info("[{}] 无依赖项，跳过依赖项加载", pluginName);
            return loadedPlugin;
        }

        logger.info("[{}] 预计加载 {} 项依赖", pluginName, libraries.size());
        int resolvedSize;
        try {
            URLClassLoader ucl = (URLClassLoader) classLoader;
            List<File> resolved = MavenResolver.resolve(libraries);
            resolvedSize = resolved.size();
            for (File dependencyFile : resolved) {
                logger.info("[{}] 正在加载 {}", pluginName, dependencyFile);
                UclUtils.addFile(ucl, dependencyFile);
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
