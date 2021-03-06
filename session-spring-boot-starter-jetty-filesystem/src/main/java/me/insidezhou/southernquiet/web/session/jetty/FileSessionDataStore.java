package me.insidezhou.southernquiet.web.session.jetty;

import me.insidezhou.southernquiet.filesystem.FileSystem;
import me.insidezhou.southernquiet.filesystem.InvalidFileException;
import me.insidezhou.southernquiet.filesystem.PathMeta;
import me.insidezhou.southernquiet.filesystem.PathNotFoundException;
import me.insidezhou.southernquiet.util.SerializationUtils;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于{@link FileSystem}的Jetty Session持久化.
 */
@SuppressWarnings("WeakerAccess")
public class FileSessionDataStore extends AbstractSessionDataStore {
    private FileSystem fileSystem;
    private String workingRoot; //Session持久化在FileSystem中的路径

    public FileSessionDataStore(FileSystem fileSystem, JettyAutoConfiguration.FileSessionProperties properties) {
        this.workingRoot = properties.getWorkingRoot();
        this.fileSystem = fileSystem;

        fileSystem.createDirectory(this.workingRoot);
    }

    @Override
    public void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        fileSystem.put(getFilePath(id), serialize(data));
    }

    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        long now = System.currentTimeMillis();

        return candidates.stream()
            .map(id -> {
                try {
                    return fileSystem.files(workingRoot, id);
                }
                catch (PathNotFoundException e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(Function.identity())
            .filter(meta -> getByMeta(meta).getExpiry() <= now)
            .map(PathMeta::getName)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        long now = System.currentTimeMillis();

        return fileSystem.files(workingRoot, id).anyMatch(meta -> getByMeta(meta).getExpiry() > now);
    }

    @Override
    public SessionData doLoad(String id) throws Exception {
        Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, id).findFirst();
        if (opt.isPresent()) {
            try (InputStream inputStream = fileSystem.openReadStream(opt.get().getPath())) {
                return deserialize(inputStream);
            }
        }

        return null;
    }

    @Override
    public boolean delete(String id) throws Exception {
        Optional<? extends PathMeta> opt = fileSystem.files(workingRoot, id).findFirst();
        opt.ifPresent(meta -> fileSystem.delete(meta.getPath()));

        return true;
    }

    private String getFilePath(String sessionId) {
        return workingRoot + FileSystem.PATH_SEPARATOR + sessionId;
    }

    private SessionData getByMeta(PathMeta meta) {
        try (InputStream inputStream = fileSystem.openReadStream(meta.getPath())) {
            return deserialize(inputStream);
        }
        catch (InvalidFileException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private InputStream serialize(SessionData data) {
        return new ByteArrayInputStream(SerializationUtils.serialize(new SessionJSON(data)));
    }

    private SessionData deserialize(InputStream stream) {
        try {
            return ((SessionJSON) SerializationUtils.deserialize(StreamUtils.copyToByteArray(stream))).toData();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
