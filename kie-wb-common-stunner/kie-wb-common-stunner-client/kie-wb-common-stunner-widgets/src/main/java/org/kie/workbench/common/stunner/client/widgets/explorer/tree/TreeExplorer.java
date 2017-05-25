/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.client.widgets.explorer.tree;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.ait.lienzo.client.widget.LienzoPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.kie.workbench.common.stunner.client.lienzo.util.LienzoPanelUtils;
import org.kie.workbench.common.stunner.core.client.api.ShapeManager;
import org.kie.workbench.common.stunner.core.client.canvas.Canvas;
import org.kie.workbench.common.stunner.core.client.canvas.CanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.event.AbstractCanvasEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.AbstractCanvasHandlerEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.CanvasClearEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementAddedEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementRemovedEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementUpdatedEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementsClearEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasClearSelectionEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasElementSelectedEvent;
import org.kie.workbench.common.stunner.core.client.shape.factory.ShapeFactory;
import org.kie.workbench.common.stunner.core.client.shape.view.glyph.Glyph;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.relationship.Child;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.AbstractChildrenTraverseCallback;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessor;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;
import org.kie.workbench.common.stunner.core.util.DefinitionUtils;
import org.uberfire.client.mvp.UberView;
import org.uberfire.ext.widgets.core.client.tree.TreeItem;

// TODO: Use incremental updates, do not visit whole graph on each model update.
@Dependent
public class TreeExplorer implements IsWidget {

    private static Logger LOGGER = Logger.getLogger(TreeExplorer.class.getName());
    private final int icoHeight = 13;
    private final int icoWidth = 13;
    ChildrenTraverseProcessor childrenTraverseProcessor;
    ShapeManager shapeManager;
    Event<CanvasElementSelectedEvent> elementSelectedEventEvent;
    View view;
    DefinitionUtils definitionUtils;
    private CanvasHandler canvasHandler;

    String selectedItemCanvasUuid;

    @Inject
    public TreeExplorer(final ChildrenTraverseProcessor childrenTraverseProcessor,
                        final Event<CanvasElementSelectedEvent> elementSelectedEventEvent,
                        final DefinitionUtils definitionUtils,
                        final ShapeManager shapeManager,
                        final View view) {
        this.childrenTraverseProcessor = childrenTraverseProcessor;
        this.elementSelectedEventEvent = elementSelectedEventEvent;
        this.definitionUtils = definitionUtils;
        this.shapeManager = shapeManager;
        this.view = view;
    }

    @PostConstruct
    public void init() {
        view.init(this);
    }

    @SuppressWarnings("unchecked")
    public void show(final CanvasHandler canvasHandler) {
        this.canvasHandler = canvasHandler;
        if (null != canvasHandler && null != canvasHandler.getDiagram()) {
            doShow(canvasHandler.getDiagram().getGraph());
        }
    }

    private void doShow(final Graph<org.kie.workbench.common.stunner.core.graph.content.view.View, Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>> graph) {
        traverseChildrenEdges(graph,
                              true);
    }

    private void traverseChildrenEdges(final Graph<org.kie.workbench.common.stunner.core.graph.content.view.View, Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>> graph,
                                       final boolean expand) {
        assert graph != null;
        clear();
        childrenTraverseProcessor.traverse(graph,
                                           new AbstractChildrenTraverseCallback<Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>, Edge<Child, Node>>() {

                                               @Override
                                               public boolean startNodeTraversal(final List<Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>> parents,
                                                                                 final Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge> node) {
                                                   super.startNodeTraversal(parents,
                                                                            node);
                                                   addItem(parents.get(parents.size() - 1),
                                                           node,
                                                           expand);
                                                   return true;
                                               }

                                               @Override
                                               public void startNodeTraversal(final Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge> node) {
                                                   super.startNodeTraversal(node);
                                                   addItem(node,
                                                           expand);
                                               }
                                           });
    }

    private Glyph getGlyph(final String shapeSetId,
                           final Element<org.kie.workbench.common.stunner.core.graph.content.view.View> element) {
        final Object definition = element.getContent().getDefinition();
        final String defId = definitionUtils.getDefinitionManager().adapters().forDefinition().getId(definition);
        final ShapeFactory factory = shapeManager.getShapeSet(shapeSetId).getShapeFactory();
        return factory.glyph(defId,
                             icoWidth,
                             icoHeight);
    }

    private void inc(final List<Integer> levels,
                     final int level) {
        if (levels.size() < (level + 1)) {
            levels.add(0);
        } else {
            final int idx = levels.get(level);
            levels.set(level,
                       idx + 1);
        }
    }

    private int[] getParentsIdx(final List<Integer> idxList,
                                final int maxLevel) {

        if (!idxList.isEmpty()) {
            final int targetPos = (idxList.size() - (idxList.size() - maxLevel)) + 1;
            final int[] resultArray = new int[targetPos];
            for (int x = 0; x < targetPos; x++) {
                resultArray[x] = idxList.get(x);
            }
            return resultArray;
        }
        return new int[]{};
    }

    public void clear() {
        view.clear();
    }

    void onSelect(final String uuid) {
        LOGGER.log(Level.SEVERE,
                   "onSelect with uuid: " + uuid);

        selectShape(canvasHandler.getCanvas(),
                    uuid);
    }

    private void selectShape(final Canvas canvas,
                             final String uuid) {
        elementSelectedEventEvent.fire(new CanvasElementSelectedEvent(canvasHandler,
                                                                      uuid));
    }

    void onCanvasClearEvent(@Observes CanvasClearEvent canvasClearEvent) {
        if (null != canvasHandler &&
                null != canvasHandler.getCanvas() &&
                canvasHandler.getCanvas().equals(canvasClearEvent.getCanvas())) {
            clear();
        }
    }

    void onCanvasElementAddedEvent(final @Observes CanvasElementAddedEvent canvasElementAddedEvent) {

        if (checkEventContext(canvasElementAddedEvent)) {
            onElementAdded(canvasElementAddedEvent.getElement());
        }
    }

    private void onElementAdded(Element element) {

        LOGGER.log(Level.SEVERE,
                   "---------------- onElementAdded:" + element.getUUID());

        Element parent = GraphUtils.getParent((Node<?, ? extends Edge>) element);

        addItem(parent,
                (Node) element,
                true);
    }

    private void onElementUpdated(Element element,
                                  CanvasHandler canvasHandler) {
        LOGGER.log(Level.SEVERE,
                   "---------------- onElementUpdated:" + element.getUUID());

        Long count = Long.valueOf(0);

        if (element != null) {
            GraphUtils.countChildren((Node) element);
        }

        if (view.isItemChanged(element.getUUID(),
                               getItemName(element),
                               count)) {

            boolean hasChildren = GraphUtils.hasChildren((Node<?, ? extends Edge>) element);

            view.removeItem(element.getUUID());

            Element parent = GraphUtils.getParent((Node<?, ? extends Edge>) element);

            addItem(parent,
                    (Node) element,
                    true);

            if (hasChildren) {
                childrenTraverseProcessor.setRootUUID(element.getUUID()).traverse(canvasHandler.getDiagram().getGraph(),
                                                                                  new AbstractChildrenTraverseCallback<Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>, Edge<Child, Node>>() {

                                                                                      @Override
                                                                                      public boolean startNodeTraversal(final List<Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge>> parents,
                                                                                                                        final Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge> node) {
                                                                                          super.startNodeTraversal(parents,
                                                                                                                   node);
                                                                                          addItem(parents.get(parents.size() - 1),
                                                                                                  node,
                                                                                                  true);
                                                                                          return true;
                                                                                      }

                                                                                      @Override
                                                                                      public void startNodeTraversal(final Node<org.kie.workbench.common.stunner.core.graph.content.view.View, Edge> node) {
                                                                                          super.startNodeTraversal(node);
                                                                                          addItem(node,
                                                                                                  true);
                                                                                      }
                                                                                  });
            }
        }
    }

    private void onElementRemoved(Element element) {
        LOGGER.log(Level.SEVERE,
                   "onElementRemoved:" + element.getUUID());
        String uuid = element.getUUID();
        view.removeItem(uuid);
    }

    private void addItem(Element parent,
                         Node item,
                         boolean expand) {

        LOGGER.log(Level.SEVERE,
                   "*** ITEM : " + item.getUUID());
        if (parent != null) {
            LOGGER.log(Level.SEVERE,
                       "*** PARENT : " + parent.getUUID());
        }
        TreeItem.Type itemType;
        Glyph ico = getGlyph(getShapeSetId(),
                             item);

        LienzoPanel icon = LienzoPanelUtils.newPanel(ico,
                                                     icoWidth,
                                                     icoHeight);

        String name = getItemName(item);
        if (GraphUtils.hasChildren(item)) {
            itemType = TreeItem.Type.CONTAINER;
        } else {
            itemType = TreeItem.Type.ITEM;
        }

        if (parent != null) {
            Long count = GraphUtils.countChildren((Node) parent);

            // If item is the first child added to the parent change the type of the parent from ITEM to CONTAINER
            if (count == 1) {
                Glyph parentIco = getGlyph(getShapeSetId(),
                                           parent);

                LienzoPanel parentIcon = LienzoPanelUtils.newPanel(parentIco,
                                                                   icoWidth,
                                                                   icoHeight);

                    view.removeItem(parent.getUUID());

                    view.addItem(parent.getUUID(),
                                 GraphUtils.getParent((Node<?, ? extends Edge>) parent).getUUID(),
                                 getItemName(parent),
                                 parentIcon,
                                 TreeItem.Type.CONTAINER,
                                 expand);

            }

            view.addItem(item.getUUID(),
                         parent.getUUID(),
                         name,
                         icon,
                         itemType,
                         expand
            );
        } else {
            view.addItem(item.getUUID(),
                         name,
                         icon,
                         itemType,
                         expand);
        }
    }

    private void addItem(final Node item,
                         final boolean expand) {
        addItem(null,
                item,
                expand);
    }

    void onCanvasElementRemovedEvent(final @Observes CanvasElementRemovedEvent elementRemovedEvent) {
        if (checkEventContext(elementRemovedEvent)) {
            onElementRemoved(elementRemovedEvent.getElement());
        }
    }

    void onCanvasElementsClearEvent(final @Observes CanvasElementsClearEvent canvasClearEvent) {

        if (checkEventContext(canvasClearEvent)) {
            LOGGER.log(Level.SEVERE,
                       "onCanvasElementsClearEvent");
            showEventGraph(canvasClearEvent);
        }
    }

    void onCanvasElementUpdatedEvent(final @Observes CanvasElementUpdatedEvent canvasElementUpdatedEvent) {

        if (checkEventContext(canvasElementUpdatedEvent)) {
            onElementUpdated(canvasElementUpdatedEvent.getElement(),
                             canvasElementUpdatedEvent.getCanvasHandler());
        }
    }

    void onCanvasElementSelectedEvent(final @Observes CanvasElementSelectedEvent event) {
        if (checkEventContext(event)) {
            if (null != getCanvasHandler()) {
                final String uuid = event.getElementUUID();

                if (!(uuid.equals(this.selectedItemCanvasUuid))) {
                    this.selectedItemCanvasUuid = uuid;
                    view.setSelectedItem(uuid);
                }
            }
        }
    }

    private void checkNotNull(String clearSelectionEvent,
                              CanvasClearSelectionEvent clearSelectionEvent1) {
    }

    private boolean checkEventContext(final AbstractCanvasHandlerEvent canvasHandlerEvent) {
        final CanvasHandler _canvasHandler = canvasHandlerEvent.getCanvasHandler();
        return canvasHandler != null && canvasHandler.equals(_canvasHandler);
    }

    private boolean checkEventContext(final AbstractCanvasEvent canvasEvent) {
        final Canvas canvas = canvasEvent.getCanvas();
        return null != canvasHandler && null != canvasHandler.getCanvas()
                && canvasHandler.getCanvas().equals(canvas);
    }

    private String getShapeSetId() {
        return canvasHandler.getDiagram().getMetadata().getShapeSetId();
    }

    @SuppressWarnings("unchecked")
    private void showEventGraph(final AbstractCanvasHandlerEvent canvasHandlerEvent) {
        doShow(canvasHandlerEvent.getCanvasHandler().getDiagram().getGraph());
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }

    public CanvasHandler getCanvasHandler() {
        return canvasHandler;
    }

    private String getItemName(final Element<org.kie.workbench.common.stunner.core.graph.content.view.View> item) {
        final String name = definitionUtils.getName(item.getContent().getDefinition());

        final String title = definitionUtils.getDefinitionManager().adapters().forDefinition().getTitle(item.getContent().getDefinition());

        if (name != null && name.trim().equals("") && title != null) {
            return title;
        }
        return (name != null ? name : "- No name -");
    }

    public interface View extends UberView<TreeExplorer> {

        View addItem(final String uuid,
                     final String name,
                     final IsWidget icon,
                     final TreeItem.Type itemType,
                     final boolean state);

        View addItem(final String uuid,
                     final String parentUuid,
                     final String name,
                     final IsWidget icon,
                     final TreeItem.Type itemType,
                     final boolean state);

        View setSelectedItem(final String uuid);

        View removeItem(String uuid);

        View clear();

        boolean isItemChanged(final String uuid,
                              final String name,
                              final Long countChildren);
    }
}