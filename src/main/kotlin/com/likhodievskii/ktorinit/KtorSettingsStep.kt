package com.likhodievskii.ktorinit

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.likhodievskii.ktorinit.api.KtorApiService
import com.likhodievskii.ktorinit.model.KtorFeature
import com.likhodievskii.ktorinit.model.KtorProjectSettings
import com.likhodievskii.ktorinit.model.Option
import javax.swing.JDialog

class KtorSettingsStep(override val context: WizardContext) : NewProjectWizardStep {
    override val propertyGraph: PropertyGraph = PropertyGraph()
    override val keywords: NewProjectWizardStep.Keywords = NewProjectWizardStep.Keywords()
    override val data: UserDataHolder = UserDataHolderBase()

    private val api = KtorApiService()

    private val versionCatalogProperty = propertyGraph.property(true)
    private val kotlinVersionProperty = propertyGraph.property("")
    private val groupIdProperty = propertyGraph.property("")
    private val artifactIdProperty = propertyGraph.property("")
    private val packageNameProperty = propertyGraph.property("")
    private val ktorVersionProperty = propertyGraph.property("")
    private val buildSystemProperty = propertyGraph.property(Option("", ""))
    private val engineProperty = propertyGraph.property(Option("", ""))
    private val configurationInProperty = propertyGraph.property(Option("", ""))

    private var buildSystemOptions = mutableListOf<Option>()
    private var engineOptions = mutableListOf<Option>()
    private var ktorVersionOptions = mutableListOf<Option>()
    private var configurationOptions = mutableListOf<Option>()

    private var updatingPackage = false

    val selectedPluginIds = mutableSetOf<String>()
    var featuresCache: List<KtorFeature> = emptyList()
    val selectedPluginsLabel = javax.swing.JLabel("No plugins selected")

    // Запускаем запрос сразу при создании шага
    private val settingsFuture = ApplicationManager.getApplication()
        .executeOnPooledThread<KtorProjectSettings?> {
            try {
                api.fetchSettings()
            } catch (e: Exception) {
                com.intellij.openapi.diagnostic.Logger
                    .getInstance(KtorSettingsStep::class.java)
                    .error("Failed to fetch Ktor settings", e)
                null
            }
        }

    override fun setupUI(builder: Panel) {
        // Ждём результата — к этому моменту запрос скорее всего уже завершён
        val settings = settingsFuture.get()

        if (settings != null) {
            buildSystemOptions.addAll(settings.buildSystem.options)
            engineOptions.addAll(settings.engine.options)
            ktorVersionOptions.addAll(settings.ktorVersion.options)
            configurationOptions.addAll(settings.configurationIn.options)
            kotlinVersionProperty.set(settings.kotlinVersion.defaultId)
            groupIdProperty.set("com.example")
            artifactIdProperty.set(settings.projectName.default)
            packageNameProperty.set("com.example.${settings.projectName.default}")
            ktorVersionProperty.set(settings.ktorVersion.defaultId)
            buildSystemProperty.set(settings.buildSystem.options.first { it.id == settings.buildSystem.defaultId })
            engineProperty.set(settings.engine.options.first { it.id == settings.engine.defaultId })
            configurationInProperty.set(settings.configurationIn.options.first { it.id == settings.configurationIn.defaultId })
        }

        with(builder) {
            row("Group ID:") {
                textField()
                    .bindText(groupIdProperty)
                    .align(AlignX.FILL)
            }
            row("Artifact ID:") {
                textField()
                    .bindText(artifactIdProperty)
                    .align(AlignX.FILL)
            }
            row("Package name:") {
                textField()
                    .bindText(packageNameProperty)
                    .align(AlignX.FILL)
            }
            row("Build tool:") {
                comboBox(buildSystemOptions.map { it.name })
                    .bindItem(
                        { buildSystemOptions.find { o -> o.id == buildSystemProperty.get().id }?.name ?: "" },
                        { name ->
                            buildSystemProperty.set(buildSystemOptions.find { o -> o.name == name } ?: Option(
                                "",
                                ""
                            ))
                        }
                    )
            }
            row("Engine:") {
                segmentedButton(engineOptions) { text = it.name }
                    .bind(engineProperty)
            }
            row("Ktor version:") {
                comboBox(ktorVersionOptions.map { it.id })
                    .bindItem(
                        { ktorVersionProperty.get() },
                        { ktorVersionProperty.set(it ?: "") }
                    )
            }
            row("Configuration in:") {
                segmentedButton(configurationOptions) { text = it.name }
                    .bind(configurationInProperty)
            }
            row {
                checkBox("Use version catalog")
                    .bindSelected(versionCatalogProperty)
            }
            separator()
            row {
                button("Add plugins") {
                    val version = ktorVersionProperty.get()
                    if (featuresCache.isEmpty()) {
                        featuresCache = try {
                            api.fetchFeatures(version)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                    if (featuresCache.isNotEmpty()) {
                        val pluginsPanel = KtorPluginsPanel(featuresCache, selectedPluginIds)
                        val ownerWindow = java.awt.KeyboardFocusManager
                            .getCurrentKeyboardFocusManager()
                            .activeWindow

                        val dialog = JDialog(
                            ownerWindow,
                            "Add Ktor Plugins",
                            java.awt.Dialog.ModalityType.APPLICATION_MODAL
                        )
                        dialog.contentPane = pluginsPanel
                        dialog.setSize(750, 550)
                        dialog.setLocationRelativeTo(ownerWindow)
                        dialog.isVisible = true
                        selectedPluginsLabel.text = if (selectedPluginIds.isEmpty()) "No plugins selected"
                        else "${selectedPluginIds.size} plugin(s) selected"
                    }
                }
            }
            row {
                cell(selectedPluginsLabel)
            }
        }

        groupIdProperty.afterChange { newGroup ->
            if (updatingPackage) return@afterChange
            updatingPackage = true
            packageNameProperty.set("$newGroup.${artifactIdProperty.get()}")
            updatingPackage = false
        }

        artifactIdProperty.afterChange { newArtifact ->
            if (updatingPackage) return@afterChange
            updatingPackage = true
            packageNameProperty.set("${groupIdProperty.get()}.$newArtifact")
            updatingPackage = false
        }
    }

    override fun setupProject(project: Project) {
        val payload = buildString {
            append(
                """
        {
            "settings": {
                "project_name": "${artifactIdProperty.get()}",
                "company_website": "${groupIdProperty.get().split(".").reversed().joinToString(".")}",
                "ktor_version": "${ktorVersionProperty.get()}",
                "kotlin_version": "${kotlinVersionProperty.get()}",
                "build_system": "${buildSystemProperty.get().id}",
                "build_system_args": {"version_catalog": ${versionCatalogProperty.get()}},
                "engine": "${engineProperty.get().id}"
            },
            "features": [${selectedPluginIds.joinToString(",") { "\"$it\"" }}],
            "configurationOption": "${configurationInProperty.get().id}",
            "addDefaultRoutes": true,
            "addWrapper": true
        }
        """.trimIndent()
            )
        }

        val projectDir = java.io.File(project.basePath!!)

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val zipStream = api.generateProject(payload)
                unzipTo(zipStream, projectDir)

                ApplicationManager.getApplication().invokeLater {
                    val baseDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                        .refreshAndFindFileByIoFile(projectDir)
                    baseDir?.refresh(false, true)
                }
            } catch (e: Exception) {
                com.intellij.openapi.diagnostic.Logger
                    .getInstance(KtorSettingsStep::class.java)
                    .error("Failed to generate project", e)
            }
        }
    }

    private fun unzipTo(inputStream: java.io.InputStream, targetDir: java.io.File) {
        java.util.zip.ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val file = java.io.File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    file.outputStream().use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
}