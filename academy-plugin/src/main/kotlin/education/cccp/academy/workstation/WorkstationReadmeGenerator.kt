package education.cccp.academy.workstation

import education.cccp.academy.opencode.OpenCodePlugin
import education.cccp.academy.opencode.OpenCodeToolGenerator

/**
 * Renders the **learner-facing** README of the workstation (ACADEMY-16,
 * D-LWA-7).
 *
 * The audience is the FPA candidate, not a developer: prerequisites, three
 * steps, the certification workflow and the declared limits. Technical
 * identifiers stay in English (code rule); the prose is French because the
 * formation and its learners are French.
 *
 * This is the v1 README (US-3 will enrich it); it is already enough for a
 * learner to start and for support to diagnose the common blockers.
 */
object WorkstationReadmeGenerator {
    fun render(
        applicationName: String,
        moduleId: String,
        moduleVersion: String,
        profile: WorkstationProfile,
        plugins: List<OpenCodePlugin>,
    ): String {
        require(applicationName.isNotBlank()) { "applicationName must not be blank" }
        require(moduleId.isNotBlank()) { "moduleId must not be blank" }
        require(moduleVersion.isNotBlank()) { "moduleVersion must not be blank" }
        val enabled = plugins.filter { it.version.isNotBlank() }
        val tools =
            if (enabled.isEmpty()) {
                "- (aucun outil activé)"
            } else {
                enabled.joinToString("\n") { plugin ->
                    val export = OpenCodeToolGenerator.exportName(plugin.id)
                    "- `${toolName(export)}` — ${plugin.role}"
                }
            }
        val profileLabel =
            when (profile) {
                WorkstationProfile.CLOUD_BYOK -> "Cloud BYOK (clé fournie par l'apprenant, pas de LLM local)"
                WorkstationProfile.LOCAL_OLLAMA -> "Ollama local (runtime embarqué, machine plus puissante requise)"
            }
        return """
            |# $applicationName — poste de travail apprenant
            |
            |Ce dossier contient le poste de travail qui vous permet de **produire** les
            |livrables de votre certification FPA : scénario pédagogique global (SPG),
            |scénario détaillé (SPD), storyboard, capsule vidéo et dossier technique.
            |
            |Module : **$moduleId** (version `$moduleVersion`)
            |Profil LLM : **$profileLabel**
            |
            |## Prérequis
            |
            |- **Docker** (seul prérequis hôte : ni Java, ni Gradle à installer).
            |- Une clé LLM si vous utilisez le profil cloud (fournie par vous, jamais dans ce dossier).
            |
            |## 1. Installer
            |
            |Linux / macOS :
            |
            |```sh
            |./installers/linux/install.sh
            |```
            |
            |Windows :
            |
            |```bat
            |installers\windows\install.bat
            |```
            |
            |L'installateur est idempotent : le relancer ne casse rien.
            |
            |## 2. Démarrer la formation
            |
            |```sh
            |docker compose up -d
            |```
            |
            |Moodle : http://localhost:8080 — le module est déjà injecté.
            |
            |## 3. Ouvrir l'agent
            |
            |```sh
            |opencode
            |```
            |
            |L'agent lit `AGENTS.md` et `opencode.json`. Il expose les boroughs publics
            |comme des outils. Demandez-lui par exemple : « produis mon SPG », puis
            |« génère mes slides », « génère ma capsule », « assemble mon dossier PDF ».
            |
            |## Outils exposés (boroughs publics)
            |
            |$tools
            |
            |Les boroughs privés (`training`, `edster`, `workspace`, `waiter`, `quizz`) ne
            |sont jamais exposés à l'apprenant.
            |
            |## Limites assumées
            |
            |- L'agent `opencode` est une interface terminal (TUI) ; il n'y a pas d'interface web.
            |- Windows est supporté par un installeur dédié ; le premier dogfooding réel est en cours.
            |- Le profil Ollama local demande plus de mémoire que le profil cloud.
            |- Ce poste est **mono-utilisateur** : il n'est pas conçu pour un hébergement multi-apprenants.
            |
            |## Vérifier l'intégrité
            |
            |Le fichier `workstation-manifest.json` contient le SHA-256 de chaque fichier.
            |Il permet de vérifier que le dossier reçu est intact.
            |
            |## Secrets
            |
            |
            |Ne mettez **jamais** de clé dans ce dossier. Elle se fournit par variable
            |d'environnement ; l'agent la lit à l'exécution, jamais depuis un fichier.
            |
            """.trimMargin()
    }

    /** The opencode tool name: `<file>_<export>` (opencode naming rule). */
    private fun toolName(export: String): String = "cccp_$export"
}
