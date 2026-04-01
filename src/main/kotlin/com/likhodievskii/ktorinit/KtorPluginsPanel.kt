package com.likhodievskii.ktorinit

import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.likhodievskii.ktorinit.model.KtorFeature
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.tree.DefaultTreeModel

class KtorPluginsPanel(
    private val allFeatures: List<KtorFeature>,
    private val selectedIds: MutableSet<String>
) : JPanel(BorderLayout()) {

    init {
        val root = CheckedTreeNode("Plugins")
        val grouped = allFeatures.groupBy { it.group }.toSortedMap()
        grouped.forEach { (group, features) ->
            val groupNode = CheckedTreeNode(group)
            groupNode.isChecked = false
            features.forEach { feature ->
                val featureNode = CheckedTreeNode(feature)
                featureNode.isChecked = feature.xmlId in selectedIds
                groupNode.add(featureNode)
            }
            root.add(groupNode)
        }

        val descriptionArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
        }

        val tree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
            override fun customizeRenderer(
                tree: JTree, value: Any, selected: Boolean,
                expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
            ) {
                val node = value as? CheckedTreeNode ?: return
                when (val obj = node.userObject) {
                    is KtorFeature -> textRenderer.append(obj.name)
                    is String -> textRenderer.append(obj)
                }
            }
        }, root, CheckboxTreeBase.CheckPolicy(
            checkChildrenWithCheckedParent = false,
            uncheckChildrenWithUncheckedParent = false,
            checkParentWithCheckedChild = false,
            uncheckParentWithUncheckedChild = false
        ))

        tree.addTreeSelectionListener { e ->
            val node = e.path?.lastPathComponent as? CheckedTreeNode
            val feature = node?.userObject as? KtorFeature
            descriptionArea.text = feature?.description ?: ""
        }

        tree.addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                val feature = node.userObject as? KtorFeature ?: return
                if (node.isChecked) selectWithDependencies(feature)
                else selectedIds.remove(feature.xmlId)
                refreshTreeNodes(root, tree)
            }
        })

        val closeButton = JButton("Done").apply {
            addActionListener {
                SwingUtilities.getWindowAncestor(this@KtorPluginsPanel)?.dispose()
            }
        }

        val splitter = com.intellij.ui.JBSplitter(false, 0.4f).apply {
            firstComponent = JBScrollPane(tree)
            secondComponent = JBScrollPane(descriptionArea)
        }
        add(splitter, BorderLayout.CENTER)
        add(closeButton, BorderLayout.SOUTH)
    }

    private fun selectWithDependencies(feature: KtorFeature) {
        selectedIds.add(feature.xmlId)
        feature.requiredFeatures.forEach { depId ->
            val dep = allFeatures.find { it.xmlId == depId }
            if (dep != null && dep.xmlId !in selectedIds) selectWithDependencies(dep)
        }
    }

    private fun refreshTreeNodes(root: CheckedTreeNode, tree: CheckboxTree) {
        val expandedPaths = (0 until tree.rowCount)
            .filter { tree.isExpanded(it) }
            .map { tree.getPathForRow(it) }

        fun traverse(node: CheckedTreeNode) {
            val feature = node.userObject as? KtorFeature
            if (feature != null) node.isChecked = feature.xmlId in selectedIds
            for (i in 0 until node.childCount) traverse(node.getChildAt(i) as CheckedTreeNode)
        }
        traverse(root)
        (tree.model as DefaultTreeModel).reload()

        expandedPaths.forEach { tree.expandPath(it) }
    }
}