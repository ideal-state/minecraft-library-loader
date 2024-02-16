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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * <p>UclHelper</p>
 *
 * <p>创建于 2024/2/15 21:52</p>
 *
 * @author ketikai
 * @version 1.0.0
 * @since 1.0.0
 */
abstract class UclUtils {
    private static final Method ADD_URL;

    static {
        try {
            ADD_URL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean addFile(URLClassLoader urlClassLoader, File file) {
        if (file != null && file.exists()) {
            try {
                ADD_URL.invoke(urlClassLoader, file.toURI().toURL());
                return true;
            } catch (IllegalAccessException | InvocationTargetException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
