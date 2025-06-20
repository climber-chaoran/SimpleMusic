package org.litepal.crud.async;

import org.litepal.crud.callback.FindMultiCallback;

/* loaded from: classes.dex */
public class FindMultiExecutor<T> extends AsyncExecutor {
    private FindMultiCallback<T> cb;

    public void listen(FindMultiCallback<T> findMultiCallback) {
        this.cb = findMultiCallback;
        execute();
    }

    public FindMultiCallback<T> getListener() {
        return this.cb;
    }
}
