package com.marginallyclever;

import java.util.EventListener;

public interface GameOverListener extends EventListener {
    void gameOver(boolean won);
}
