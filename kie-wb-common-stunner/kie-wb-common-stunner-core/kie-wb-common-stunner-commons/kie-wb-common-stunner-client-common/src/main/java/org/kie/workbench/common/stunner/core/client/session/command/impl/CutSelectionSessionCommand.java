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

package org.kie.workbench.common.stunner.core.client.session.command.impl;

import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.clipboard.ClipboardControl;
import org.kie.workbench.common.stunner.core.client.canvas.event.registration.CanvasElementsClearEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasClearSelectionEvent;
import org.kie.workbench.common.stunner.core.client.canvas.event.selection.CanvasSelectionEvent;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.command.Command;

import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;

/**
 * This session command copy to the clipboard using {@link CopySelectionSessionCommand} selected elements and delete them using the {@link DeleteSelectionSessionCommand}. *
 */
@Dependent
public class CutSelectionSessionCommand extends AbstractSelectionAwareSessionCommand<ClientFullSession> {

    private final CopySelectionSessionCommand copySelectionSessionCommand;
    private final DeleteSelectionSessionCommand deleteSelectionSessionCommand;
    private final Event<CutSelectionSessionCommandExecutedEvent> commandExecutedEvent;
    private static Logger LOGGER = Logger.getLogger(CopySelectionSessionCommand.class.getName());
    private final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    private ClipboardControl clipboardControl;

    protected CutSelectionSessionCommand() {
        this(null,
             null,
             null);
    }

    @Inject
    public CutSelectionSessionCommand(final SessionCommandFactory sessionCommandFactory,
                                      final @Session SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                                      final Event<CutSelectionSessionCommandExecutedEvent> commandExecutedEvent) {
        super(true);
        this.copySelectionSessionCommand = sessionCommandFactory.newCopySelectionCommand();
        this.deleteSelectionSessionCommand = sessionCommandFactory.newDeleteSelectedElementsCommand();
        this.sessionCommandManager = sessionCommandManager;
        this.commandExecutedEvent = commandExecutedEvent;
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        copySelectionSessionCommand.bind(session);
        deleteSelectionSessionCommand.bind(session);
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
        this.clipboardControl = session.getClipboardControl();
    }

    protected void onKeyDownEvent(final Key... keys) {
        handleCtrlX(keys);
    }

    private void handleCtrlX(Key[] keys) {
        if (doKeysMatch(keys,
                        Key.CONTROL,
                        Key.X)) {
            this.execute(newDefaultCallback("Error while trying to cut selected items."));
        }
    }

    @Override
    public <V> void execute(Callback<V> callback) {
        copySelectionSessionCommand.execute(new Callback<V>() {
            @Override
            public void onSuccess() {
                deleteSelectionSessionCommand.execute(callback);
                //get the executed command by deleteSelectionSessionCommand
                Command<AbstractCanvasHandler, CanvasViolation> command = sessionCommandManager.getRegistry().peek();
                clipboardControl.setRollbackCommand(command);
                commandExecutedEvent.fire(new CutSelectionSessionCommandExecutedEvent(CutSelectionSessionCommand.this,
                                                                                      CutSelectionSessionCommand.this.getSession()));
            }

            @Override
            public void onError(V error) {
                LOGGER.severe("Error on cut selection." + String.valueOf(error));
                callback.onError(error);
            }
        });
    }

    @Override
    protected void handleCanvasSelectionEvent(final CanvasSelectionEvent event) {
        if (event.getIdentifiers().isEmpty() || onlyCanvasRootSelected(event)) {
            enable(false);
        } else {
            enable(true);
        }
    }

    @Override
    protected void handleCanvasClearSelectionEvent(final CanvasClearSelectionEvent event) {
        enable(false);
    }

    @Override
    protected void handleCanvasElementsClearEvent(final CanvasElementsClearEvent event) {
        enable(false);
    }
}