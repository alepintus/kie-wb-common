/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.client.widgets.components.glyph;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.errai.common.client.api.IsElement;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.stunner.core.client.components.glyph.DOMGlyphRenderer;
import org.kie.workbench.common.stunner.core.client.components.views.ImageElementRendererView;
import org.kie.workbench.common.stunner.core.client.shape.SvgDataUriGlyph;
import org.kie.workbench.common.stunner.core.client.util.SvgDataUriGenerator;

/**
 * Extracts the SVG content from the data-uri and appends the content into the DOM.
 * It renders as an SVG DOM element.
 */
@ApplicationScoped
public class SvgElementGlyphRenderer implements DOMGlyphRenderer<SvgDataUriGlyph> {

    private final SvgDataUriGenerator svgDataUriUtil;
    private final Supplier<ImageElementRendererView> viewInstanceSupplier;

    protected SvgElementGlyphRenderer() {
        this.svgDataUriUtil = null;
        this.viewInstanceSupplier = null;
    }

    @Inject
    public SvgElementGlyphRenderer(final SvgDataUriGenerator svgDataUriUtil,
                                   final ManagedInstance<ImageElementRendererView> viewInstances) {
        this.svgDataUriUtil = svgDataUriUtil;
        this.viewInstanceSupplier = viewInstances::get;
    }

    SvgElementGlyphRenderer(final SvgDataUriGenerator svgDataUriUtil,
                            final Supplier<ImageElementRendererView> viewInstanceSupplier) {
        this.svgDataUriUtil = svgDataUriUtil;
        this.viewInstanceSupplier = viewInstanceSupplier;
    }

    @Override
    public Class<SvgDataUriGlyph> getGlyphType() {
        return SvgDataUriGlyph.class;
    }

    @Override
    public IsElement render(final SvgDataUriGlyph glyph,
                            final double width,
                            final double height) {
        final String content = svgDataUriUtil
                .generate(glyph.getSvg(),
                          glyph.getDefs(),
                          glyph.getValidUseRefIds());
        final ImageElementRendererView view = viewInstanceSupplier.get();
        return view.setDOMContent(content,
                                  (int) width,
                                  (int) height);
    }
}
