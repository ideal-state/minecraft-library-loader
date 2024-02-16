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
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>LibraryLoader</p>
 *
 * <p>创建于 2024/2/15 20:07</p>
 *
 * @author ketikai
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LibraryLoader extends JavaPlugin {

    private static final Logger logger = LogManager.getLogger(LibraryLoader.class);

    public LibraryLoader() {
        super();
        saveDefaultConfig();
        File localRepository = new File("./libraries/");
        if (!localRepository.exists()) {
            localRepository.mkdirs();
        }
        MavenResolver.initialize(localRepository,
                getConfig().getConfigurationSection("repositories"));
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager instanceof SimplePluginManager) {
            try {
                Field fileAssociations = SimplePluginManager.class.getDeclaredField("fileAssociations");
                fileAssociations.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Pattern, PluginLoader> fileAssociationsVal = (Map<Pattern, PluginLoader>) fileAssociations.get(pluginManager);
                for (Map.Entry<Pattern, PluginLoader> entry : fileAssociationsVal.entrySet()) {
                    PluginLoader pluginLoader = entry.getValue();
                    if (pluginLoader instanceof JavaPluginLoader) {
                        entry.setValue(new ProxyJavaPluginLoader((JavaPluginLoader) pluginLoader));
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.error("不支持的 PluginManager 类型实例，即将关闭服务器");
            Bukkit.shutdown();
        }
    }
}
