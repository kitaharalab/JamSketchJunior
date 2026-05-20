package jp.jamsketch.main

/**
 * JamSketchのイベントリスナー
 */
public interface JamSketchEventListner extends EventListener {
 
    /**
     * 切断された状態を通知する
     */
    public void disconnected();
}