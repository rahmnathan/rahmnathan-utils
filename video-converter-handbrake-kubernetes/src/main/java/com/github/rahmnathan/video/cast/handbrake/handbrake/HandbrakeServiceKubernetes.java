package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class HandbrakeServiceKubernetes {

    public void convertMedia(SimpleConversionJob conversionJob) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().withHttpClientFactory(new JdkHttpClientFactory()).build()) {

            if (Files.exists(conversionJob.getOutputFile().toPath())) {
                Files.delete(conversionJob.getOutputFile().toPath());
            }

            Optional<Pod> localmoviesPodOptional = client.pods().list().getItems().stream()
                    .filter(pod -> "localmovies".equalsIgnoreCase(pod.getMetadata().getLabels().get("app")))
                    .findAny();

            if (localmoviesPodOptional.isEmpty()) {
                return;
            }

            String podName = "handbrake-" + UUID.randomUUID();

            log.info("Creating job {} to process media conversion.", podName);

            List<String> args = List.of("-Z", conversionJob.getHandbrakePreset(),
                    "-i", conversionJob.getInputFile().getAbsolutePath(),
                    "-o", conversionJob.getOutputFile().getAbsolutePath(),
                    "-v");

            List<Volume> volumes = localmoviesPodOptional.get().getSpec().getVolumes().stream()
                    .filter(volume -> volume.getName().startsWith("media"))
                    .toList();

            List<VolumeMount> volumeMounts = localmoviesPodOptional.get().getSpec().getContainers().stream()
                    .filter(container -> "localmovies".equalsIgnoreCase(container.getName()))
                    .findAny()
                    .get()
                    .getVolumeMounts().stream()
                    .filter(volumeMount -> volumeMount.getName().startsWith("media"))
                    .toList();

            String namespace = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));

            ResourceRequirements resources = new ResourceRequirements(
                    Map.of("cpu", Quantity.parse("6"),
                           "memory", Quantity.parse("6Gi")),
                    Map.of("cpu", Quantity.parse("2"),
                           "memory", Quantity.parse("2Gi"))
            );

            final Job job = new JobBuilder()
                    .withApiVersion("batch/v1")
                    .withNewMetadata()
                    .withName(podName)
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(podName)
                    .withImage("rahmnathan/handbrake:latest")
                    .withArgs(args)
                    .withVolumeMounts(volumeMounts)
                    .withResources(resources)
                    .endContainer()
                    .withVolumes(volumes)
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            Job launchedJob = client.batch().v1().jobs().inNamespace(namespace).resource(job).createOrReplace();

            log.info("Created job successfully.");

            client.batch().v1().jobs().inNamespace(namespace).withName(podName).waitUntilCondition(job1 ->
                    job1 != null &&
                            job1.getStatus() != null &&
                            job1.getStatus().getReady() != null &&
                            job1.getStatus().getReady() > 0, 5, TimeUnit.MINUTES);

            client.batch().v1().jobs().inNamespace(namespace).withName(podName).waitUntilCondition(job1 ->
                    job1 != null &&
                            job1.getStatus() != null &&
                            job1.getStatus().getSucceeded() != null &&
                            job1.getStatus().getSucceeded() > 0, 6, TimeUnit.HOURS);

            conversionJob.getInputFile().delete();
            log.info("Job completed.");
        }
    }
}
