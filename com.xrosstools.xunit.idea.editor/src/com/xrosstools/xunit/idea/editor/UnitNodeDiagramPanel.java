package com.xrosstools.xunit.idea.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.xrosstools.xunit.idea.editor.commands.DeleteNodeCommand;
import com.xrosstools.xunit.idea.editor.figures.Figure;
import com.xrosstools.xunit.idea.editor.figures.UnitNodeDiagramFigure;
import com.xrosstools.xunit.idea.editor.io.UnitNodeDiagramFactory;
import com.xrosstools.xunit.idea.editor.model.*;
import com.xrosstools.xunit.idea.editor.parts.EditContext;
import com.xrosstools.xunit.idea.editor.parts.EditPart;
import com.xrosstools.xunit.idea.editor.parts.UnitNodePartFactory;
import com.xrosstools.xunit.idea.editor.policies.DiagramLayoutPolicy;
import com.xrosstools.xunit.idea.editor.policies.UnitNodeLayoutPolicy;
import com.xrosstools.xunit.idea.editor.treeparts.TreeEditPart;
import com.xrosstools.xunit.idea.editor.treeparts.UnitNodeTreePartFactory;
import com.xrosstools.xunit.idea.editor.util.*;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

//TODO make it factoryinstead of subclass of JPAnel
public class UnitNodeDiagramPanel extends JPanel implements PropertyChangeListener, UnitConstants {
    private Project project;
    private VirtualFile virtualFile;

    private boolean showed;
    private JSplitPane mainPane;
    private JSplitPane diagramPane;
    private Tree treeNavigator;
    private JBTable tableProperties;

    private UnitNodeDiagramFactory factory = new UnitNodeDiagramFactory();
    private UnitNodeDiagram diagram;

    private UnitPanel unitPanel;
    private EditPart root;

    private Figure lastSelected;
    private Figure lastHover;
    private UnitNode newUnitNode;

    private DiagramLayoutPolicy diagramLayoutPolicy = new DiagramLayoutPolicy();
    private UnitNodeLayoutPolicy nodeLayoutPolicy = new UnitNodeLayoutPolicy();

    public UnitNodeDiagramPanel(Project project, VirtualFile virtualFile) throws Exception {
        this.project = project;
        this.virtualFile = virtualFile;
        diagram = factory.getFromDocument(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(virtualFile.getInputStream()));
        diagram.setFilePath(virtualFile);
        createVisual();
        registerListener();
        build();
    }

    private void createVisual() {
        setLayout(new BorderLayout());
        mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPane.setDividerSize(3);
        add(mainPane, BorderLayout.CENTER);

        mainPane.setLeftComponent(createMain());
        mainPane.setRightComponent(createProperty());

        //TODO to optimize portion setting, we shall intercept sync event
        mainPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                initPortion();
            }
        });
    }

    public void initPortion() {
        if (!showed) {
            mainPane.setDividerLocation(0.8);
            diagramPane.setDividerLocation(0.8);
            showed = true;
        }
    }

    private JComponent createMain() {
        diagramPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        diagramPane.setDividerSize(3);

        diagramPane.setLeftComponent(createButtons());
        diagramPane.setRightComponent(createTree());

        return diagramPane;
    }

    private JComponent createButtons() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel toolbar = new JPanel();
        GridLayout layout = new GridLayout(0, 1, 10,0);
        toolbar.setLayout(layout);
        toolbar.add(createResetButton());
        toolbar.add(createButton("Processor", Activator.PROCESSOR, ProcessorNode.class));
        toolbar.add(createButton("Converter", Activator.CONVERTER, ConverterNode.class));
        toolbar.add(createButton("Validator", Activator.VALIDATOR, ValidatorNode.class));
        toolbar.add(createButton("Locator", Activator.LOCATOR, LocatorNode.class));
        toolbar.add(createButton("Chain", Activator.CHAIN, ChainNode.class));
        toolbar.add(createButton("If/else", Activator.BI_BRANCH, BiBranchNode.class));
        toolbar.add(createButton("Branch", Activator.BRANCH, BranchNode.class));
        toolbar.add(createButton("While loop", Activator.WHILE, PreValidationLoopNode.class));
        toolbar.add(createButton("Do while loop", Activator.DO_WHILE, PostValidationLoopNode.class));
        toolbar.add(createButton("Start", Activator.START_POINT, StartPointNode.class));
        toolbar.add(createButton("End", Activator.END_POINT, EndPointNode.class));
        toolbar.add(createButton("Decorator", Activator.DECORATOR, DecoratorNode.class));
        toolbar.add(createButton("Adapter", Activator.ADAPTER, AdapterNode.class));
        mainPanel.add(toolbar, BorderLayout.WEST);

        unitPanel = new UnitPanel();
        JScrollPane diagramPane = new JBScrollPane(unitPanel);
        diagramPane.setLayout(new ScrollPaneLayout());
        diagramPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        diagramPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        diagramPane.getVerticalScrollBar().setUnitIncrement(50);

        mainPanel.add(diagramPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JComponent createTree() {
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("aaa");
        node1.add(new DefaultMutableTreeNode("aaa-1"));
        node1.add(new DefaultMutableTreeNode("aaa-2"));
        treeNavigator = new Tree(node1);

        JScrollPane treePane = new JBScrollPane(treeNavigator);
        treePane.setLayout(new ScrollPaneLayout());
        treePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        treePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        treePane.getVerticalScrollBar().setUnitIncrement(50);

        return treePane;
    }

    private JComponent createProperty() {
        PropertyTableModel model = new PropertyTableModel(diagram, this);
        tableProperties = new JBTable(model);
        JScrollPane scrollPane = new JBScrollPane(tableProperties);
        scrollPane.setLayout(new ScrollPaneLayout());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(50);

        return scrollPane;
    }

    private void save() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    String contentStr = XmlHelper.format(factory.writeToDocument(diagram));
                    virtualFile.setBinaryContent(contentStr.getBytes(virtualFile.getCharset()));
                } catch (Throwable e) {
                    throw new IllegalStateException("Can not save document " + virtualFile.getName(), e);
                }
            }
        });
    }

    private void build() {
        EditContext context = new EditContext(unitPanel);

        UnitNodePartFactory f = new UnitNodePartFactory(context);
        root = f.createEditPart(null, diagram);
        root.build();
        updateVisual();

        UnitNodeTreePartFactory treePartFactory = new UnitNodeTreePartFactory(context);
        TreeEditPart treeRoot = treePartFactory.createEditPart(null, diagram);
        treeNavigator.setModel(new DefaultTreeModel(treeRoot.build(), false));
    }

    private void rebuild() {
        build();
    }

    private void registerListener() {
        unitPanel.addMouseMotionListener(new MouseMotionListener() {
            private void updateHover(MouseEvent e) {
                Figure f = root.getFigure().findFigureAt(e.getX(), e.getY());
                f = f == null ? root.getFigure() : f;

                if(lastHover == f)
                    return;

                if(lastHover != null) {
                    lastHover.setInsertionPoint(null);
                    unitPanel.repaint(lastHover.getBound());
                }

                if(f != null) {
                    f.setInsertionPoint(e.getPoint());
                    unitPanel.repaint(f.getBound());
                }

                lastHover = f;

            }
            @Override
            public void mouseDragged(MouseEvent e) {
                updateHover(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Figure f = root.getFigure().findFigureAt(e.getX(), e.getY());
                if(f == null || f == root.getFigure())
                    unitPanel.setToolTipText(null);
                else
                    unitPanel.setToolTipText(f.getToolTipText());

                if(newUnitNode != null)
                    updateHover(e);
            }
        });

        unitPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Create new node
                if (newUnitNode != null && lastHover != null) {
                    if (lastHover instanceof UnitNodeDiagramFigure)
                        update(diagramLayoutPolicy.getCreateCommand(lastHover, newUnitNode));
                    else
                        update(nodeLayoutPolicy.getCreateCommand(lastHover.getPart(), newUnitNode));
                    return;
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                Figure f = root.getFigure().selectFigureAt(e.getX(), e.getY());

                if(lastSelected == f)
                    return;

                if(lastSelected != null) {
                    lastSelected.setSelected(false);
                    deselectedFigure(lastSelected);
                }

                if(f != null) {
                    f.setSelected(true);
                    selectedFigure(f);
                }

                lastSelected = f;
                unitPanel.repaint();

                if (e.isPopupTrigger())
                    showContexMenu(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if(lastHover != null) {
                    lastHover.setInsertionPoint(null);
                    unitPanel.repaint(lastHover.getBound());
                }

                // Drag and drop
                if (lastSelected != null && lastHover != null && lastSelected != lastHover) {
                    if (lastHover instanceof UnitNodeDiagramFigure)
                        update(diagramLayoutPolicy.createAddCommand(lastHover, lastSelected.getPart()));
                    else
                        update(nodeLayoutPolicy.getAddCommand(lastHover.getPart(), lastSelected.getPart()));
                    return;
                }

                if (e.isPopupTrigger())
                    showContexMenu(e.getX(), e.getY());
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(lastSelected == null)
                    return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE :
                        update(new DeleteNodeCommand(lastSelected.getPart().getParent().getModel(), (UnitNode)lastSelected.getPart().getModel()));
                }
            }
        });
    }

    private void deselectedFigure(Figure deselected) {

    }

    private void selectedFigure(Figure selected) {
        PropertyTableModel model = new PropertyTableModel((IPropertySource)selected.getPart().getModel(), this);
        tableProperties.setModel(model);
        tableProperties.setDefaultRenderer(Object.class, new SimpleTableRenderer(model));
        tableProperties.getColumnModel().getColumn(1).setCellEditor(new SimpleTableCellEditor(model));
    }

    private void showContexMenu(int x, int y) {
        UnitContextMenuProvider builder = new UnitContextMenuProvider();
        builder.buildContextMenu(project, diagram, lastSelected.getPart()).show(unitPanel, x, y);
    }

    public void reset() {
        if(lastSelected != null) {
            lastSelected.setSelected(false);
            lastSelected = null;
        }

        newUnitNode = null;
        unitPanel.repaint();
    }

    private JButton createResetButton() {
        JButton btn = new JButton("Select", IconLoader.findIcon(Activator.getIconPath(Activator.PROCESSOR)));
        btn.setContentAreaFilled(false);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        return btn;
    }

    private JButton createButton(String name, String icon, final Class unitNodeClass) {
        JButton btn = new JButton(name, IconLoader.findIcon(Activator.getIconPath(icon)));
        btn.setContentAreaFilled(false);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newUnitNode = (UnitNode) unitNodeClass.newInstance();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        });
        return btn;
    }

    private void update(Runnable action) {
        if(action == null)
            return;

        action.run();
        rebuild();
        reset();
        save();
    }

    private void updateVisual() {
        unitPanel.getPreferredSize();
        repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        save();
    }

    private class UnitPanel extends JPanel {
        @Override
        protected void paintChildren(Graphics g) {
            root.getFigure().paint(g);
        }

        @Override
        public Dimension getPreferredSize() {
            if(root == null)
                return new Dimension(500,800);

            Dimension size = root.getFigure().getPreferredSize();
            root.getFigure().layout();
            return size;
        }
    }
}