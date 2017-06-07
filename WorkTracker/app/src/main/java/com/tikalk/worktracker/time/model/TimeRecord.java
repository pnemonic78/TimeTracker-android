/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tikalk.worktracker.time.model;

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
public class TimeRecord extends TimeRecordEntity {

    // Need this for the Room DAO generator.
    private long projectId;
    // Need this for the Room DAO generator.
    private long projectRemoteId;
    // Need this for the Room DAO generator.
    private int projectVersion;
    // Need this for the Room DAO generator.
    private String projectName;
    // Need this for the Room DAO generator.
    private String projectDescription;
    // Need this for the Room DAO generator.
    private long projectTaskRemoteId;
    // Need this for the Room DAO generator.
    private int projectTaskVersion;
    // Need this for the Room DAO generator.
    private String projectTaskName;
    // Need this for the Room DAO generator.
    private String projectTaskDescription;

    public long getProjectId() {
        return getTask().getProjectId();
    }

    public void setProjectId(long id) {
        getTask().setProjectId(id);
    }

    public long getProjectRemoteId() {
        return getTask().getProjectRemoteId();
    }

    public void setProjectRemoteId(long id) {
        getTask().setProjectRemoteId(id);
    }

    public int getProjectVersion() {
        return getTask().getProjectVersion();
    }

    public void setProjectVersion(int version) {
        getTask().setProjectVersion(version);
    }

    public String getProjectName() {
        return getTask().getProjectName();
    }

    public void setProjectName(String name) {
        getTask().setProjectName(name);
    }

    public String getProjectDescription() {
        return getTask().getProjectDescription();
    }

    public void setProjectDescription(String description) {
        getTask().setProjectDescription(description);
    }

    public long getProjectTaskRemoteId() {
        return getTask().id;
    }

    public void setProjectTaskRemoteId(long id) {
        getTask().id = id;
    }

    public int getProjectTaskVersion() {
        return getTask().version;
    }

    public void setProjectTaskVersion(int version) {
        getTask().version = version;
    }

    public String getProjectTaskName() {
        return getTask().name;
    }

    public void setProjectTaskName(String name) {
        getTask().name = name;
    }

    public String getProjectTaskDescription() {
        return getTask().description;
    }

    public void setProjectTaskDescription(String description) {
        getTask().description = description;
    }
}
