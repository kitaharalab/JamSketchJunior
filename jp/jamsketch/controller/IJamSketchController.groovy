package jp.jamsketch.controller;

/**
 * JamSketchの操作クラスのインターフェース
 */
public interface IJamSketchController{

    /**
     * 初期化する
     */
    public void init();

	/**
     * Curveを更新する
     * @param from 始点
     * @param thru 終点
     * @param y Y座標
     */
    public void updateCurve(int from, int thru, int y);

    /**
     * リセットする
     */
    public void reset();
}