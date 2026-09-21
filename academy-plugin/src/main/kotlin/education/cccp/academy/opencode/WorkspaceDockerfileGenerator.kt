package education.cccp.academy.opencode

/**
 * Pure workspace image definition renderer (ACADEMY-5-1) — produces the
 * `Dockerfile` of the `cccp-education/academy-workspace` image, built locally
 * by `docker compose build` (no registry, no publication).
 *
 * **Base = `gradle:9.7.1-jdk25`** (verified S-009): Ubuntu 26.04 already
 * carrying Java 25 + Gradle 9.7.1 + git + curl + apt-get — exactly the
 * toolchain D-ACADEMY-6-10 pins to the workspace image. The official
 * `ghcr.io/anomalyco/opencode` image is Alpine-minimal (no zsh, no git, no
 * Java/Gradle) and cannot run the ecosystem tasks, so a generated Dockerfile
 * is required; the `opencode` binary is installed from the official script.
 *
 * The image ships `zsh` (the session shell, D-ACADEMY-1) and runs as the
 * non-root `learner` user with the project mounted at `/workspace`.
 */
object WorkspaceDockerfileGenerator {

    /** Renders the `Dockerfile` of the learner workspace image. */
    fun render(config: OpenCodeConfig): String = listOf(
        "# Academy workspace image (ACADEMY-5) - generated, built locally by docker compose",
        "# Base carries the pinned toolchain (Java 25 + Gradle 9.7.1) per D-ACADEMY-6-10:",
        "# the host never provisions Java/Gradle, the workspace image does.",
        "FROM gradle:9.7.1-jdk25",
        "",
        "USER root",
        "RUN apt-get update \\",
        "    && apt-get install -y --no-install-recommends zsh git curl ca-certificates \\",
        "    && rm -rf /var/lib/apt/lists/*",
        "",
        "# Non-root learner user owning the mounted project and the agent install",
        "RUN useradd --create-home --shell /bin/zsh learner",
        "",
        "# Install the opencode agent AS the learner (never a bundled blob): a root",
        "# install would land in /root/.opencode and be invisible to the learner.",
        "USER learner",
        "ENV HOME=/home/learner",
        "ENV PATH=\"/home/learner/.opencode/bin:\${PATH}\"",
        "RUN curl -fsSL https://opencode.ai/install | bash",
        "",
        "WORKDIR /workspace",
        "",
        "# Interactive zsh session - the learner drives the agent from here",
        "CMD [\"/bin/zsh\"]",
    ).joinToString("\n", postfix = "\n")
}
