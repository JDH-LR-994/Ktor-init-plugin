package com.likhodievskii.ktorinit

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class KtorProjectGenerator : GeneratorNewProjectWizard {

    override val id: @NonNls String = "ktor.generator"
    override val name: @Nls(capitalization = Nls.Capitalization.Title) String = "Ktor"
    override val icon: Icon = IconLoader.getIcon("/icons/Ktor.svg", javaClass)


    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep { NewProjectWizardBaseStep(it) }
            .nextStep { KtorSettingsStep(context) }
}