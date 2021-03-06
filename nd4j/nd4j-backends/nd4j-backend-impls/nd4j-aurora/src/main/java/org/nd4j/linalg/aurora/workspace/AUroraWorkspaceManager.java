/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */


package org.nd4j.linalg.aurora.workspace;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.memory.MemoryWorkspace;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.memory.enums.DebugMode;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.api.memory.abstracts.DummyWorkspace;
import org.nd4j.linalg.api.memory.provider.BasicWorkspaceManager;

/**
 * @author Adam Gibson
 */
@Slf4j
public class AUroraWorkspaceManager extends BasicWorkspaceManager {

    public AUroraWorkspaceManager() {
        super();
    }

    protected MemoryWorkspace newWorkspace(WorkspaceConfiguration configuration) {
        return Nd4j.getWorkspaceManager().getDebugMode() == DebugMode.BYPASS_EVERYTHING ? new DummyWorkspace() : new AuroraWorkspace(configuration);
    }

    protected MemoryWorkspace newWorkspace(WorkspaceConfiguration configuration, String id) {
        return Nd4j.getWorkspaceManager().getDebugMode() == DebugMode.BYPASS_EVERYTHING ? new DummyWorkspace() : new AuroraWorkspace(configuration, id);
    }

    protected MemoryWorkspace newWorkspace(WorkspaceConfiguration configuration, String id, int deviceId) {
        return Nd4j.getWorkspaceManager().getDebugMode() == DebugMode.BYPASS_EVERYTHING ? new DummyWorkspace() : new AuroraWorkspace(configuration, id, deviceId);
    }

    @Override
    public MemoryWorkspace createNewWorkspace(@NonNull WorkspaceConfiguration configuration) {
        ensureThreadExistense();

        MemoryWorkspace workspace = newWorkspace(configuration);

        backingMap.get().put(workspace.getId(), workspace);

        if (Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.BYPASS_EVERYTHING)
            pickReference(workspace);

        return workspace;
    }

    @Override
    public MemoryWorkspace createNewWorkspace() {
        ensureThreadExistense();

        MemoryWorkspace workspace = newWorkspace(defaultConfiguration);

        backingMap.get().put(workspace.getId(), workspace);

        if (Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.BYPASS_EVERYTHING)
            pickReference(workspace);

        return workspace;
    }

    @Override
    public MemoryWorkspace createNewWorkspace(@NonNull WorkspaceConfiguration configuration, @NonNull String id) {
        ensureThreadExistense();

        MemoryWorkspace workspace = newWorkspace(configuration, id);

        backingMap.get().put(id, workspace);

        if (Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.BYPASS_EVERYTHING)
            pickReference(workspace);

        return workspace;
    }

    @Override
    public MemoryWorkspace createNewWorkspace(@NonNull WorkspaceConfiguration configuration, @NonNull String id, Integer deviceId) {
        ensureThreadExistense();

        MemoryWorkspace workspace = newWorkspace(configuration, id, deviceId);

        backingMap.get().put(id, workspace);

        if (Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.BYPASS_EVERYTHING)
            pickReference(workspace);

        return workspace;
    }

    @Override
    public MemoryWorkspace getWorkspaceForCurrentThread(@NonNull WorkspaceConfiguration configuration, @NonNull String id) {
        ensureThreadExistense();

        MemoryWorkspace workspace = backingMap.get().get(id);
        if (workspace == null) {
            workspace = newWorkspace(configuration, id);
            backingMap.get().put(id, workspace);

            if (Nd4j.getWorkspaceManager().getDebugMode() != DebugMode.BYPASS_EVERYTHING)
                pickReference(workspace);
        }

        return workspace;
    }

    @Override
    protected void pickReference(MemoryWorkspace w) {
        Nd4j.getDeallocatorService().pickObject(w);
    }
}
