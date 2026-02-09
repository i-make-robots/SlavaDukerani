package com.marginallyclever;

import java.util.EventListener;

public interface FlagChangeListener extends EventListener {
    void flagCountChanged(int flagCount);
}
