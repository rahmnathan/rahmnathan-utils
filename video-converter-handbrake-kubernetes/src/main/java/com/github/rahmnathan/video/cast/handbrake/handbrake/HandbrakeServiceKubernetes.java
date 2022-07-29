package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;
import io.micrometer.core.instrument.Metrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
public class HandbrakeServiceKubernetes {
    private static final AtomicInteger ACTIVE_CONVERSION_GAUGE = Metrics.gauge("handbrake.conversions.active", new AtomicInteger(0));

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
                           "memory", Quantity.parse("4Gi")),
                    Map.of("cpu", Quantity.parse("2"),
                           "memory", Quantity.parse("2Gi"))
            );

            Job job = new JobBuilder()
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

            client.batch().v1().jobs().inNamespace(namespace).resource(job).createOrReplace();

            log.info("Created job successfully.");

            client.batch().v1().jobs().inNamespace(namespace).withName(podName).waitUntilCondition(job1 ->
                    job1 != null &&
                            job1.getStatus() != null &&
                            job1.getStatus().getReady() != null &&
                            job1.getStatus().getReady() > 0, 12, TimeUnit.HOURS);

            ACTIVE_CONVERSION_GAUGE.getAndIncrement();

            try {
                while(true) {
                    Thread.sleep(60000);
                    job = client.batch().v1().jobs().inNamespace(namespace).withName(podName).get();
                    if(job != null &&
                            job.getStatus() != null &&
                            job.getStatus().getSucceeded() != null &&
                            job.getStatus().getSucceeded() > 0) {
                        break;
                    } else {
                        log.info("Waiting for job to complete.");
                        if(job != null) {
                            log.info("JobStatus: {}", job.getStatus());
                        }
                    }
                }

                conversionJob.getInputFile().delete();
            } catch (InterruptedException e){
                log.error("Interrupted.", e);
            } finally {
                ACTIVE_CONVERSION_GAUGE.getAndDecrement();
            }
            log.info("Job completed.");
        }
    }
}
