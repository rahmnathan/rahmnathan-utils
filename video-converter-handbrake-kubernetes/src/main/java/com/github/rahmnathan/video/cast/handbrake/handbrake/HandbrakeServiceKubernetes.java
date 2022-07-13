package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class HandbrakeServiceKubernetes {

    public void convertMedia(SimpleConversionJob conversionJob) throws IOException, ApiException {
        ApiClient client = Config.defaultClient();
        CoreV1Api api = new CoreV1Api(client);

        Configuration.setDefaultApiClient(client);

        V1Pod v1Pod = new V1Pod();

        V1PodSpec podSpec = new V1PodSpec();
        String podName = "handbrake-" + UUID.randomUUID();
        V1ObjectMeta objectMeta = new V1ObjectMeta();
        objectMeta.name(podName);
        v1Pod.metadata(objectMeta);

        V1Container ffmpegContainer = new V1Container();
        ffmpegContainer.name("handbrake");
        ffmpegContainer.image("rahmnathan/handbrake:latest");

        String namespace = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));

        V1Pod localmoviesPod = api.listNamespacedPod(namespace, null, null, null, null, "app=localmovies", 1, null, null, null, null)
                .getItems()
                .get(0);

        V1Container localmoviesContainer = localmoviesPod.getSpec().getContainers().stream()
                .filter(container -> "localmovies".equalsIgnoreCase(container.getName()))
                .toList().get(0);

        List<V1VolumeMount> volumeMounts = localmoviesContainer.getVolumeMounts().stream()
                .filter(v1VolumeMount -> v1VolumeMount.getName().startsWith("media"))
                .collect(Collectors.toList());

        List<V1VolumeDevice> devices = localmoviesContainer.getVolumeDevices().stream()
                .filter(v1VolumeDevice -> v1VolumeDevice.getName().startsWith("media"))
                .collect(Collectors.toList());

        ffmpegContainer.setVolumeMounts(volumeMounts);
        ffmpegContainer.setVolumeDevices(devices);

        List<String> args = List.of("-Z", conversionJob.getHandbrakePreset(),
                "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-o", conversionJob.getOutputFile().getAbsolutePath());

        ffmpegContainer.command(args);

        podSpec.setContainers(List.of(ffmpegContainer));

        V1Pod v1Pod1 = api.createNamespacedPod(namespace, v1Pod, podName, null, null, null);

        while(true) {
            String logs = api.readNamespacedPodLog(podName, namespace, null, null, Boolean.TRUE, null, null, null, null, null, null);

            log.info(logs);

            v1Pod1 = api.readNamespacedPodStatus(podName, namespace, null);
            log.info(Objects.requireNonNull(v1Pod1.getStatus()).toString());
        }
    }
}
