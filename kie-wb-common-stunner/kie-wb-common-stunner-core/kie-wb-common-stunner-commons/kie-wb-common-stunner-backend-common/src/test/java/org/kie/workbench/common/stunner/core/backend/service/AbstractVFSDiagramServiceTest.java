/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.api.DefinitionManager;
import org.kie.workbench.common.stunner.core.api.FactoryManager;
import org.kie.workbench.common.stunner.core.definition.DefinitionSetResourceType;
import org.kie.workbench.common.stunner.core.definition.service.DefinitionSetService;
import org.kie.workbench.common.stunner.core.definition.service.DiagramMarshaller;
import org.kie.workbench.common.stunner.core.definition.service.DiagramMetadataMarshaller;
import org.kie.workbench.common.stunner.core.diagram.Diagram;
import org.kie.workbench.common.stunner.core.diagram.Metadata;
import org.kie.workbench.common.stunner.core.factory.diagram.DiagramFactory;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.content.definition.DefinitionSet;
import org.kie.workbench.common.stunner.core.registry.BackendRegistryFactory;
import org.kie.workbench.common.stunner.core.registry.factory.FactoryRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.commons.data.Pair;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Files;
import org.uberfire.java.nio.file.SimpleFileVisitor;
import org.uberfire.java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
public abstract class AbstractVFSDiagramServiceTest<M extends Metadata, D extends Diagram<Graph, M>> {

    public static final String DEFINITION_SET_ID = "DEFINITION_SET_ID";

    public static final String RESOURCE_TYPE_SUFFIX = "SUFFIX";

    public static final String RESOURCE_TYPE_PREFIX = "PREFIX";

    public static final String DIR_URI = "default://master@diagrams/diagramsDirectory";

    public static final String FILE_NAME = "TestFile";

    public static final String FILE_NAME_IN_RESOURCE_FORMAT = RESOURCE_TYPE_PREFIX + FILE_NAME + "." + RESOURCE_TYPE_SUFFIX;

    public static final String FILE_URI = DIR_URI + "/" + FILE_NAME_IN_RESOURCE_FORMAT;

    public static final String DIAGRAM_MARSHALLED = "DIAGRAM_MARSHALLED";

    public static final String METADATA_MARSHALLED = "METADATA_MARSHALLED";

    @Mock
    protected DefinitionManager definitionManager;

    @Mock
    protected FactoryManager factoryManager;

    @Mock
    protected Instance<DefinitionSetService> definitionSetServiceInstances;

    @Mock
    protected DefinitionSetService definitionSetService;

    @Mock
    protected DefinitionSetResourceType resourceType;

    @Mock
    protected DiagramMarshaller diagramMarshaller;

    @Mock
    protected DiagramMetadataMarshaller metadataMarshaller;

    @Mock
    protected IOService ioService;

    @Mock
    protected BackendRegistryFactory registryFactory;

    @Mock
    protected FactoryRegistry factoryRegistry;

    protected AbstractVFSDiagramService<M, D> diagramService;

    protected D diagram;

    protected M metadata;

    @Before
    public void setUp() throws IOException {
        when(resourceType.getPrefix()).thenReturn(RESOURCE_TYPE_PREFIX);
        when(resourceType.getSuffix()).thenReturn(RESOURCE_TYPE_SUFFIX);
        doReturn(Object.class).when(resourceType).getDefinitionSetType();

        when(definitionSetService.getResourceType()).thenReturn(resourceType);
        when(definitionSetService.getDiagramMarshaller()).thenReturn(diagramMarshaller);
        when(diagramMarshaller.getMetadataMarshaller()).thenReturn(metadataMarshaller);
        List<DefinitionSetService> services = new ArrayList<>();
        services.add(definitionSetService);
        when(definitionSetService.accepts(DEFINITION_SET_ID)).thenReturn(true);
        when(definitionSetServiceInstances.iterator()).thenReturn(services.iterator());

        when(factoryManager.registry()).thenReturn(factoryRegistry);

        diagram = mockDiagram();
        metadata = mockMetadata();
        when(diagram.getMetadata()).thenReturn(metadata);
        when(metadata.getDefinitionSetId()).thenReturn(DEFINITION_SET_ID);
        when(diagramMarshaller.marshall(diagram)).thenReturn(DIAGRAM_MARSHALLED);
        when(metadataMarshaller.marshall(metadata)).thenReturn(METADATA_MARSHALLED);

        diagramService = spy(createVFSDiagramService());
    }

    public abstract AbstractVFSDiagramService<M, D> createVFSDiagramService();

    public abstract Class<? extends Metadata> getMetadataType();

    public abstract D mockDiagram();

    public abstract M mockMetadata();

    @Test
    public void testCreate() throws IOException {
        Path path = mock(Path.class);
        when(path.toURI()).thenReturn(DIR_URI);

        final org.uberfire.java.nio.file.Path expectedNioPath = Paths.convert(path).resolve(FILE_NAME_IN_RESOURCE_FORMAT);

        when(factoryManager.newDiagram(FILE_NAME,
                                       DEFINITION_SET_ID,
                                       metadata)).thenReturn(diagram);
        diagramService.create(path,
                              FILE_NAME,
                              DEFINITION_SET_ID,
                              metadata);

        verify(ioService,
               times(1)).write(expectedNioPath,
                               DIAGRAM_MARSHALLED);
    }

    @Test
    public void testGetRawContent() throws IOException {
        String result = diagramService.getRawContent(diagram);
        assertEquals(DIAGRAM_MARSHALLED,
                     result);
    }

    @Test
    public void testGetDiagramByPath() throws IOException {
        Path path = mock(Path.class);
        when(path.toURI()).thenReturn(FILE_URI);
        String fileName = FILE_NAME + "." + RESOURCE_TYPE_SUFFIX;
        when(path.getFileName()).thenReturn(fileName);
        when(resourceType.accept(path)).thenReturn(true);
        final org.uberfire.java.nio.file.Path expectedNioPath = Paths.convert(path);

        byte[] content = DIAGRAM_MARSHALLED.getBytes();
        when(ioService.readAllBytes(expectedNioPath)).thenReturn(content);

        Graph<DefinitionSet, ?> graph = mock(Graph.class);
        DefinitionSet graphContent = mock(DefinitionSet.class);
        when(graph.getContent()).thenReturn(graphContent);
        when(graphContent.getDefinition()).thenReturn("DefinitionSet");

        when(diagramMarshaller.unmarshall(anyObject(),
                                          anyObject())).thenReturn(graph);

        DiagramFactory diagramFactory = mock(DiagramFactory.class);
        when(factoryRegistry.getDiagramFactory("DefinitionSet",
                                               getMetadataType())).thenReturn(diagramFactory);

        when(diagramFactory.build(eq(FILE_NAME),
                                  any(Metadata.class),
                                  eq(graph))).thenReturn(diagram);

        Diagram result = diagramService.getDiagramByPath(path);
        assertEquals(diagram,
                     result);
    }

    @Test
    public void testContains() {
        Path path = mock(Path.class);
        when(metadata.getPath()).thenReturn(path);
        doReturn(diagram).when(diagramService).getDiagramByPath(path);

        assertTrue(diagramService.contains(diagram));
        verify(diagramService,
               times(1)).getDiagramByPath(path);
    }

    @PrepareForTest({Files.class, Paths.class})
    @Test
    public void testGetAll() {
        ArgumentCaptor<SimpleFileVisitor> visitorArgumentCaptor = ArgumentCaptor.forClass(SimpleFileVisitor.class);
        mockStatic(Files.class);
        mockStatic(Paths.class);

        org.uberfire.java.nio.file.Path root = mock(org.uberfire.java.nio.file.Path.class);

        D diagram = mockDiagram();
        List<Pair<Path, org.uberfire.java.nio.file.Path>> visitedPaths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Path diagramPath = mock(Path.class);
            org.uberfire.java.nio.file.Path nioDiagramPath = mock(org.uberfire.java.nio.file.Path.class);
            when(Paths.convert(diagramPath)).thenReturn(nioDiagramPath);
            when(Paths.convert(nioDiagramPath)).thenReturn(diagramPath);
            visitedPaths.add(new Pair<>(diagramPath,
                                        nioDiagramPath));
            when(resourceType.accept(diagramPath)).thenReturn(true);
            doReturn(diagram).when(diagramService).getDiagramByPath(diagramPath);
        }
        BasicFileAttributes attrs = mock(BasicFileAttributes.class);

        when(ioService.exists(root)).thenReturn(true);
        diagramService.getDiagramsByPath(root);

        verifyStatic();
        Files.walkFileTree(eq(root),
                           visitorArgumentCaptor.capture());

        visitedPaths.forEach(pair -> {
            visitorArgumentCaptor.getValue().visitFile(pair.getK2(),
                                                       attrs);
            verify(diagramService,
                   times(1)).getDiagramByPath(pair.getK1());
        });
    }
}
