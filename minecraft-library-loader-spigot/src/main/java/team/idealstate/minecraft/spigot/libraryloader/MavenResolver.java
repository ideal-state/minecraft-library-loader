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
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>MavenResolver</p>
 *
 * <p>创建于 2024/2/15 20:50</p>
 *
 * @author ketikai
 * @version 1.0.0
 * @since 1.0.0
 */
abstract class MavenResolver {

    private static final Logger logger = LogManager.getLogger(MavenResolver.class);
    private static final RepositorySystem SYSTEM;
    private static final DefaultRepositorySystemSession SESSION;
    private static volatile List<RemoteRepository> repositories = null;

    static {
        RepositorySystemSupplier supplier = new RepositorySystemSupplier();
        SYSTEM = supplier.get();
        SESSION = MavenRepositorySystemUtils.newSession();
    }

    static void initialize(File localRepositories, ConfigurationSection config) {
        SESSION.setChecksumPolicy("fail");
        SESSION.setLocalRepositoryManager(SYSTEM.newLocalRepositoryManager(SESSION,
                new LocalRepository(localRepositories)));
        SESSION.setTransferListener(new TransferLog());
        SESSION.setReadOnly();
        Set<RemoteRepository> remoteRepositories;
        if (config != null) {
            remoteRepositories = new LinkedHashSet<>(config.getKeys(false).size() + 1);
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String url = section.getString("url");
                if (url == null) {
                    continue;
                }
                remoteRepositories.add(new RemoteRepository.Builder(key, "default", url).build());
            }
        } else {
            remoteRepositories = new LinkedHashSet<>(1);
        }
        remoteRepositories.add(new RemoteRepository.Builder(
                "central", "default", "https://repo1.maven.org/maven2/").build());
        repositories = SYSTEM.newResolutionRepositories(SESSION, new ArrayList<>(remoteRepositories));
    }

    public static List<File> resolve(Collection<String> dependencyIds) {
        if (!(dependencyIds instanceof Set)) {
            dependencyIds = new LinkedHashSet<>(dependencyIds);
        }
        List<Dependency> dependencies = new ArrayList<>();
        for (String dependencyId : dependencyIds) {
            DefaultArtifact defaultArtifact = new DefaultArtifact(dependencyId);
            Dependency dependency = new Dependency(defaultArtifact, JavaScopes.RUNTIME);
            dependencies.add(dependency);
        }

        DependencyResult dependencyResult;
        try {
            dependencyResult = SYSTEM.resolveDependencies(
                    SESSION,
                    new DependencyRequest(
                            new CollectRequest((Dependency) null, dependencies, repositories),
                            null
                    )
            );
        } catch (DependencyResolutionException e) {
            throw new RuntimeException("无法解析依赖项", e);
        }
        return dependencyResult.getArtifactResults()
                .stream()
                .map(artifactResult -> artifactResult.getArtifact().getFile())
                .collect(Collectors.toList());
    }

    private static class TransferLog extends AbstractTransferListener {
        @Override
        public void transferStarted(TransferEvent event) throws TransferCancelledException {
            TransferResource resource = event.getResource();
            logger.info("开始下载 {}",
                    resource.getRepositoryUrl() + resource.getResourceName());
        }

        @Override
        public void transferSucceeded(TransferEvent event) {
            TransferResource resource = event.getResource();
            logger.info("下载完成 {}",
                    resource.getRepositoryUrl() + resource.getResourceName());
        }

        @Override
        public void transferFailed(TransferEvent event) {
            TransferResource resource = event.getResource();
            logger.error("下载失败 {}",
                    resource.getRepositoryUrl() + resource.getResourceName());
        }
    }
}
