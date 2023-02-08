package com.github.luben.zstd;

public class ZstdFrameProgression {
    
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private long ingested;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private long consumed;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private long produced;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private long flushed;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private int currentJobID;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private int nbActiveWorkers;
    
    public ZstdFrameProgression(long ingested, long consumed, long produced, long flushed, int currentJobID,
            int nbActiveWorkers) {
        this.ingested = ingested;
        this.consumed = consumed;
        this.produced = produced;
        this.flushed = flushed;
        this.currentJobID = currentJobID;
        this.nbActiveWorkers = nbActiveWorkers;
    }

    /**
     * The number of input bytes read and buffered.
     */
    public long getIngested() {
        return ingested;
    }

    /**
     * The number of input bytes actually compressed.
     * Note: ingested - consumed = amount of input data buffered internally, not yet compressed.
     */
    public long getConsumed() {
        return consumed;
    }

    /**
     * The number of compressed bytes generated and buffered.
     */
    public long getProduced() {
        return produced;
    }

    /**
     * The number of compressed bytes flushed.
     */
    public long getFlushed() {
        return flushed;
    }

    /**
     * The last started job number. Only applicable if multi-threading is enabled.
     */
    public int getCurrentJobID() {
        return currentJobID;
    }

    /**
     * The number of workers actively compressing. Only applicable if multi-threading is enabled.
     */
    public int getNbActiveWorkers() {
        return nbActiveWorkers;
    }
    
}
