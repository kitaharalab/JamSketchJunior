package jp.jamsketch.controller;

import jp.jamsketch.main.MelodyData2;
import java.util.function.Supplier;

/**
 * JamSketchの操作クラス
 * サーバーやクライアントとして使う場合、このクラスのインスタンスを委譲する
 */
public class JamSketchController implements IJamSketchController{

    // 楽譜データ
    private MelodyData2 melodyData;

    // 初期化メソッド
    private Supplier<MelodyData2> initData;
    
    /**
     * コンストラクタ
     * @param melodyData 楽譜データ
     * @param initData 初期化メソッド
     */
    public JamSketchController(MelodyData2 melodyData, Supplier<MelodyData2> initData){
        this.melodyData = melodyData;
        this.initData = initData;
    }

    /**
     * 初期化する
     */
    public void init(){
        // do nothing
    }

    /**
     * Curveを更新する
     * @param from 始点
     * @param thru 終点
     * @param y Y座標
     */
    @Override
    public void updateCurve(int from, int thru, int y){
        this.melodyData.storeCursorPosition(from, thru, y)
        this.melodyData.updateCurve(from, thru);
    }

    /**
     * リセットする
     */
    @Override
    public void reset(){
        // 渡してきた初期化メソッドを実行する
        // 本来は初期化処理をこちらに移植した方が理想であるが、初期化処理が画面の操作と絡まっているため、渡してきたメソッドをそのまま実行することで実現している
        this.melodyData = this.initData.get();
    }
}