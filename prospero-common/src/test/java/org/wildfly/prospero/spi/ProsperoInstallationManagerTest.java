/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.prospero.spi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelManifestCoordinate;
import org.wildfly.channel.Repository;
import org.wildfly.installationmanager.InstallationChanges;
import org.wildfly.prospero.actions.InstallationHistoryAction;
import org.wildfly.prospero.actions.UpdateAction;
import org.wildfly.prospero.api.ChannelChange;
import org.wildfly.prospero.updates.UpdateSet;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProsperoInstallationManagerTest {
    private static final Channel CHANNEL_1 = new Channel("channel-1", null, null, null,
            List.of(new Repository("repo1", "url1"), new Repository("repo2", "url2")),
            new ChannelManifestCoordinate("foo", "bar"));

    private static final Channel CHANNEL_2 = new Channel("channel-1", null, null, null,
            List.of(new Repository("repo1", "url1b"), new Repository("repo2", "url2")),
            new ChannelManifestCoordinate("foo", "bar2"));
    @Mock
    private ProsperoInstallationManager.ActionFactory actionFactory;

    @Mock
    private UpdateAction updateAction;

    @Mock
    private InstallationHistoryAction historyAction;

    @Test
    public void testChannelChanges() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getHistoryAction()).thenReturn(historyAction);
        when(historyAction.compare(any())).thenReturn(
                new org.wildfly.prospero.api.InstallationChanges(Collections.emptyList(), List.of(ChannelChange.modified(CHANNEL_1, CHANNEL_2))));

        final InstallationChanges changes = mgr.revisionDetails("abcd");

        final org.wildfly.installationmanager.ChannelChange channelChange = changes.channelChanges().get(0);
        final org.wildfly.installationmanager.Channel oldChannel = channelChange.getOldChannel().get();
        final org.wildfly.installationmanager.Channel newChannel = channelChange.getNewChannel().get();
        assertEquals("channel-1", channelChange.getName());
        assertEquals("foo:bar", oldChannel.getManifestCoordinate().get());
        assertEquals("foo:bar2", newChannel.getManifestCoordinate().get());
        assertEquals("repo1", oldChannel.getRepositories().get(0).getId());
        assertEquals("repo1", newChannel.getRepositories().get(0).getId());
        assertEquals("url1", oldChannel.getRepositories().get(0).getUrl());
        assertEquals("url1b", newChannel.getRepositories().get(0).getUrl());
    }

    @Test
    public void testChannelChangesNewChannel() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getHistoryAction()).thenReturn(historyAction);
        when(historyAction.compare(any())).thenReturn(
                new org.wildfly.prospero.api.InstallationChanges(Collections.emptyList(), List.of(ChannelChange.added(CHANNEL_1))));

        final InstallationChanges changes = mgr.revisionDetails("abcd");

        final org.wildfly.installationmanager.ChannelChange channelChange = changes.channelChanges().get(0);
        final org.wildfly.installationmanager.Channel oldChannel = channelChange.getOldChannel().orElse(null);
        final org.wildfly.installationmanager.Channel newChannel = channelChange.getNewChannel().orElse(null);

        assertEquals("channel-1", channelChange.getName());
        assertNull(oldChannel);
        assertEquals("foo:bar", newChannel.getManifestCoordinate().get());
        assertEquals(2, newChannel.getRepositories().size());
        assertEquals("repo1", newChannel.getRepositories().get(0).getId());
        assertEquals("url1", newChannel.getRepositories().get(0).getUrl());
    }

    @Test
    public void findUpdateWithNullRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(Collections.emptyList())).thenReturn(updateAction);
        when(updateAction.findUpdates()).thenReturn(new UpdateSet(Collections.emptyList()));

        mgr.findUpdates(null);
    }

    @Test
    public void findUpdateWithEmptyRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(Collections.emptyList())).thenReturn(updateAction);
        when(updateAction.findUpdates()).thenReturn(new UpdateSet(Collections.emptyList()));

        mgr.findUpdates(null);
    }

    @Test
    public void findUpdateWithRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(List.of(new Repository("test", "http://test.te")))).thenReturn(updateAction);
        when(updateAction.findUpdates()).thenReturn(new UpdateSet(Collections.emptyList()));

        mgr.findUpdates(List.of(new org.wildfly.installationmanager.Repository("test", "http://test.te")));
    }

    @Test
    public void prepareUpdateWithNullRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(Collections.emptyList())).thenReturn(updateAction);
        when(updateAction.buildUpdate(any())).thenReturn(true);

        mgr.prepareUpdate(Path.of("test"), null);
    }

    @Test
    public void prepareUpdateWithEmptyRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(Collections.emptyList())).thenReturn(updateAction);
        when(updateAction.buildUpdate(any())).thenReturn(true);

        mgr.prepareUpdate(Path.of("test"), Collections.emptyList());
    }

    @Test
    public void prepareUpdateWithRepositoryListPassesEmptyList() throws Exception {
        final ProsperoInstallationManager mgr = new ProsperoInstallationManager(actionFactory);
        when(actionFactory.getUpdateAction(List.of(new Repository("test", "http://test.te")))).thenReturn(updateAction);
        when(updateAction.buildUpdate(any())).thenReturn(true);

        mgr.prepareUpdate(Path.of("test"), List.of(new org.wildfly.installationmanager.Repository("test", "http://test.te")));
    }
}