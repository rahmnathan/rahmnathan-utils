package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@AllArgsConstructor
public class HandbrakeServiceKubernetes {

    public void convertMedia(SimpleConversionJob conversionJob) throws IOException {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {

            if (Files.exists(conversionJob.getOutputFile().toPath())) {
                Files.delete(conversionJob.getOutputFile().toPath());
            }

            Optional<Pod> localmoviesPodOptional = client.pods().list().getItems().stream()
                    .filter(pod -> pod.getMetadata().getLabels().get("app").equalsIgnoreCase("localmovies"))
                    .findAny();

            if (localmoviesPodOptional.isEmpty()) {
                return;
            }

            String podName = "handbrake-" + UUID.randomUUID();

            log.info("Creating job {} to process media conversion.", podName);

            List<String> args = List.of("-Z", conversionJob.getHandbrakePreset(),
                    "-i", conversionJob.getInputFile().getAbsolutePath(),
                    "-o", conversionJob.getOutputFile().getAbsolutePath());

            List<Volume> volumes = localmoviesPodOptional.get().getSpec().getVolumes().stream()
                    .filter(volume -> volume.getName().startsWith("media"))
                    .toList();

            String namespace = Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/namespace"));

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
                    .endContainer()
                    .withVolumes(volumes)
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            Job runningJob = client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);

            log.info("Created job successfully.");

            while (runningJob.getStatus().getSucceeded() < 1) {
                log.info("Waiting for conversion to complete.");
                Thread.sleep(30000);
                runningJob = client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);
            }
        } catch (InterruptedException e) {
            log.error("Error in job library.", e);
        }
    }
}
