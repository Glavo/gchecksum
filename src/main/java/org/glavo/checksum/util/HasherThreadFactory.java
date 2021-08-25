package org.glavo.checksum.util;

import java.util.concurrent.ThreadFactory;

public final class HasherThreadFactory implements ThreadFactory {
    public HasherThreadFactory() {
    }

    @Override
    public Thread newThread(Runnable r) {
        return new HasherThread(r);
    }
}
