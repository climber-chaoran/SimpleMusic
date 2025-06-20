package org.litepal.crud.async;

import org.litepal.crud.callback.FindCallback;

/* loaded from: classes.dex */
public class FindExecutor<T> extends AsyncExecutor {
    private FindCallback<T> cb;

    public void listen(FindCallback<T> findCallback) {
        this.cb = findCallback;
        execute();
    }

    public FindCallback<T> getListener() {
        return this.cb;
    }
}
