package com.ultramega.refinedflowanalytics.screen;

public final class MenuRequestTracker {
    private long generation;
    private long nextRequestId;
    private long pendingRequestId = -1;
    private long pendingGeneration;

    public void invalidate() {
        this.generation++;
    }

    public long tryStartRequest() {
        if (this.pendingRequestId != -1) {
            return -1;
        }
        this.pendingGeneration = this.generation;
        this.pendingRequestId = this.nextRequestId++;
        return this.pendingRequestId;
    }

    public Completion complete(final long requestId) {
        if (this.pendingRequestId == -1 || requestId != this.pendingRequestId) {
            return Completion.IGNORED;
        }
        this.pendingRequestId = -1;
        return this.pendingGeneration == this.generation ? Completion.CURRENT : Completion.OBSOLETE;
    }

    public enum Completion {
        IGNORED,
        CURRENT,
        OBSOLETE
    }
}
