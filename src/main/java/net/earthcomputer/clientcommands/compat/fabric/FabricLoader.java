package net.earthcomputer.clientcommands.compat.fabric;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Counterpart to Fabric Loader's {@code net.fabricmc.loader.api.FabricLoader}, providing only the
 * surface the ported code uses: {@link #getConfigDir()}, {@link #isDevelopmentEnvironment()} and
 * {@link #getModContainer(String)} with {@code findPath}.
 */
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    /** Fabric's config directory; NeoForge's equivalent is {@code FMLPaths.CONFIGDIR}. */
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Fabric's game directory. */
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /** Whether a mod with the given id is present. */
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    public Optional<ModContainer> getModContainer(String modId) {
        return ModList.get().getModContainerById(modId).map(ModContainer::new);
    }

    /** Wraps a NeoForge mod container to expose Fabric's {@code findPath}. */
    public static final class ModContainer {
        private final net.neoforged.fml.ModContainer delegate;

        ModContainer(net.neoforged.fml.ModContainer delegate) {
            this.delegate = delegate;
        }

        public IModInfo getMetadata() {
            return delegate.getModInfo();
        }

        /**
         * Resolves a classpath resource owned by this mod, mirroring Fabric's
         * {@code ModContainer#findPath}. {@code build_info.json} is emitted into the mod's own
         * resources by the {@code generateBuildInfo} task, so the mod file locator is the right root.
         */
        public Optional<Path> findPath(String path) {
            // Fall back to extracting from the classloader when running from a dev environment,
            // where the resource lives in build/resources rather than inside a jar.
            try (InputStream in = FabricLoader.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    return Optional.empty();
                }
                Path temp = Files.createTempFile("clientcommands-build-info", ".json");
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return Optional.of(temp);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }
}
