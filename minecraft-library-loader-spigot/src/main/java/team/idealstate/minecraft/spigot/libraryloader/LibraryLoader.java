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

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.MalformedURLException;

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

    @Override
    public void onLoad() {
        saveDefaultConfig();
        File localRepository = new File("./libraries/");
        if (!localRepository.exists()) {
            localRepository.mkdirs();
        }
        try {
            MavenResolver.initialize(localRepository,
                    getConfig().getConfigurationSection("repositories"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Bukkit.getPluginManager().registerInterface(ProxyJavaPluginLoader.class);
    }
}
