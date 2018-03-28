package com.xrosstools.xunit.idea.editor.treeparts;

import com.intellij.openapi.util.IconLoader;
import com.xrosstools.xunit.idea.editor.Activator;
import com.xrosstools.xunit.idea.editor.model.UnitConstants;
import com.xrosstools.xunit.idea.editor.model.UnitNode;
import com.xrosstools.xunit.idea.editor.parts.EditContext;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class TreeEditPart implements UnitConstants, PropertyChangeListener {
    private UnitNodeTreePartFactory factory;
    private TreeEditPart parent;
    private EditContext editContext;
    private Object model;
    private List<TreeEditPart> childEditParts = new ArrayList<>();

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    protected List<UnitNode> getModelChildren() {
        return new ArrayList<>();
    }

    public final DefaultMutableTreeNode build() {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(getText());
        List children = getModelChildren();
        for (int i = 0; i < children.size(); i++) {
            TreeEditPart childEditPart = factory.createEditPart(this, children.get(i));
            childEditParts.add(childEditPart);
            treeNode.add(childEditPart.build());
        }

        return treeNode;
    }

    public String toString() {
        return getText();
    }

    protected String getText() {
        return "";
    }

    protected Icon getImage() {
        return IconLoader.findIcon(Activator.getIconPath(getModel().getClass()));
    }

    public void refresh() {

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        refresh();
    }

    public EditContext getContext() {
        return editContext;
    }

    public void setContext(EditContext editContext) {
        this.editContext = editContext;
    }

    public TreeEditPart getParent() {
        return parent;
    }

    public void setParent(TreeEditPart parent) {
        this.parent = parent;
    }

    public void setEditPartFactory(UnitNodeTreePartFactory factory) {
        this.factory = factory;
    }
}