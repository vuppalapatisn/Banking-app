---
name: devops
description: Work on build, CI/CD, containerization, deployment, Kubernetes, and cloud infrastructure for the Banking-app services. Use when editing the GitHub Actions pipeline, Dockerfiles, docker-compose, environment configuration, or authoring Kubernetes/cloud deployment manifests.
---

# Skill: devops

## Description
Delivery and operations playbook for a Maven multi-module Spring Boot system shipped as one Docker
image per service, built/tested by GitHub Actions and run on Kafka + PostgreSQL infrastructure.

## Scope
- **In scope:** `pom.xml` reactor build, `.github/workflows/ci.yml`, per-service `Dockerfile`,
  `.dockerignore`, `docker-compose.yml`, environment/config wiring, Kubernetes manifests, cloud
  deployment, health/observability.
- **Out of scope:** application/business logic (use `feature-development`); vulnerability analysis
  (use `security`); code correctness (use `code-review`).

## Inputs
- The delivery task (pipeline change, new image, deploy target, scaling need).
- Current build: reactor `pom.xml`, CI workflow, Dockerfiles, `docker-compose.yml`, per-service
  ports and env vars (`SPRING_*`, `*_URI`, `ANTHROPIC_API_KEY`, datasource creds).

## Outputs
- Working pipeline / manifests / compose changes that keep `mvn clean verify` authoritative and
  each service independently deployable.
- Documentation updates (`run.md`, README) for any changed build/run/deploy step.
- Rollback and verification notes for deployment changes.

## Process
1. **Preserve the canonical build.** Any CI change MUST keep the `build` job running
   `mvn clean verify` on JDK 21 across all modules; images build from repo root context using
   `<service>/Dockerfile` (multi-stage: `mvn -pl <mod> -am -DskipTests package` → `eclipse-temurin:21-jre`).
2. **Configuration as environment.** Inject config/secrets via env vars or a secrets manager — never
   bake into images. Keep localhost defaults for local runs only.
3. **Per-service deployment.** Treat each service as an independently versioned, independently
   deployable unit with its own health probe (`/actuator/health`).
4. **Infra dependencies.** Kafka (KRaft) and PostgreSQL are external; wire bootstrap servers and
   datasource via env; ensure ordering/readiness (compose healthchecks; K8s readiness/liveness).
5. **Verify** locally with `docker compose up -d` + image run; verify CI is green; document steps.
6. **Rollout** with a rollback path (previous image tag) and post-deploy health/lag checks.

## Best Practices
- Pin base images and cache Maven deps in CI (`setup-java` cache) for speed and reproducibility.
- Externalize secrets (K8s Secrets / cloud secret manager); mount, don't embed.
- Readiness probe = app up AND dependencies reachable; liveness = process healthy.
- Keep images small (JRE runtime stage, `.dockerignore` excludes `target/`, `.git/`, docs).
- Make pipeline steps idempotent and fail-fast; surface dropped/skipped work in logs.

## Anti-Patterns
- Committing secrets/kubeconfig/real `application-*.yml` to the repo.
- Skipping tests in CI to make a build pass; `continue-on-error` on the build job.
- `latest`-only image tags with no immutable version; deploying untested images.
- Baking environment-specific hostnames/creds into images.
- Disabling TLS verification in build/deploy tooling.

## Examples
- *Push images to a registry.* → Extend the `docker` job with login + `docker/build-push-action`
  `push: true` and an immutable tag (git SHA); keep `build` as a prerequisite; store registry creds
  as encrypted secrets.
- *Add Kubernetes manifests.* → Deployment + Service + ConfigMap (non-secret) + Secret (creds) per
  service; env from ConfigMap/Secret; readiness/liveness on `/actuator/health`; document in `run.md`.
- *Speed up CI.* → Enable Maven cache in `setup-java`; parallelize the image matrix (already per-service).
```
