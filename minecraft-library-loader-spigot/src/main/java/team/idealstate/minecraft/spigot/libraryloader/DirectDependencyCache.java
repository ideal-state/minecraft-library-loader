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

import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>DirectDependencyCache</p>
 *
 * <p>创建于 2024/2/19 15:57</p>
 *
 * @author ketikai
 * @version 1.0.1
 * @since 1.0.1
 */
final class DirectDependencyCache extends ConcurrentHashMap<String, Class<?>> {

    private static final Logger logger = LogManager.getLogger(DirectDependencyCache.class);
    private final URLClassLoader ucl;
    private final ThreadLocal<Set<String>> inGet = ThreadLocal.withInitial(HashSet::new);

    DirectDependencyCache(URLClassLoader ucl) {
        this.ucl = ucl;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            ucl.close();
        } catch (Throwable e) {
            logger.error("在关闭 URLClassLoader 时抛出异常", e);
        } finally {
            super.finalize();
        }
    }

    @Override
    public Class<?> get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        Set<String> loadingClassNames = inGet.get();
        String className = (String) key;
        if (loadingClassNames.contains(className)) {
            return null;
        }
        Class<?> cls = super.get(className);
        if (cls == null) {
            try {
                loadingClassNames.add(className);
                logger.trace("尝试从直接依赖内加载类 {}", className);
                cls = Class.forName(className, true, ucl);
                logger.debug("已从直接依赖内加载类 {}", className);
                put(className, cls);
            } catch (Throwable e) {
                logger.trace("从直接依赖内加载类时抛出异常", e);
            } finally {
                loadingClassNames.remove(className);
            }
        }
        return cls;
    }
}
