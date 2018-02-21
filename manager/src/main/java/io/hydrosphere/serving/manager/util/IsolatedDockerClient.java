package io.hydrosphere.serving.manager.util;


import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentSkipListSet;

public class IsolatedDockerClient extends DefaultDockerClient {

    public static IsolatedDockerClient createFromEnv() throws DockerCertificateException {
        return new IsolatedDockerClient(DefaultDockerClient.fromEnv());
    }

    private ConcurrentSkipListSet<String> imageStorage = new ConcurrentSkipListSet<>();
    private ConcurrentSkipListSet<String> containerStorage = new ConcurrentSkipListSet<>();

    private IsolatedDockerClient(Builder builder) {
        super(builder);
    }

    @Override
    public String build(Path directory, String name, String dockerfile, ProgressHandler handler, BuildParam... params)
            throws DockerException, InterruptedException, IOException {
        String image = super.build(directory, name, dockerfile, handler, params);
        imageStorage.add(image);
        return image;
    }

    @Override
    public ContainerCreation createContainer(final ContainerConfig config, final String name)
            throws DockerException, InterruptedException {
        ContainerCreation creation = super.createContainer(config, name);
        containerStorage.add(creation.id());
        return creation;
    }

    public void clear() {
        for (String container : containerStorage) {
            try {
                super.removeContainer(container, RemoveContainerParam.forceKill());
                containerStorage.remove(container);
            } catch (DockerException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (String image : imageStorage) {
            try {
                super.removeImage(image, true, true);
                imageStorage.remove(image);
            } catch (DockerException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        clear();
        super.close();
    }
}
