package jp.jamsketch.web;

import jakarta.websocket.Session;

import jp.jamsketch.controller.IJamSketchController;

/**
 * サービスロケーター
 * サーバークラスに引数でオブジェクトを渡せないため、そのために用意した
 */
public class ServiceLocator{
    
    // インスタンス
    private static final ServiceLocator instance = new ServiceLocator();
    
    // JamSketchの操作クラス
    private IJamSketchController controller;

    // WebSocketサーバーのセッション
    private Session session;

    /**
     * 操作クラスを取得する
     */
    public IJamSketchController getContoller(){
        return controller;
    }

    /**
     * 操作クラスを設定する
     * @param controller 操作クラスのインスタンス
     */
    public IJamSketchController setContoller(IJamSketchController controller){
        this.controller = controller;
    }

    /**
     * セッションを取得する
     */
    public Session getSession(){
        return session;
    }

    /**
     * セッションを設定する
     * @param session セッションのインスタンス
     */
    public Session setSession(Session session){
        this.session = session;
    }

    /**
     * コンストラクタ
     */
    private ServiceLocator(){

    }

    /**
     * インスタンスを取得する
     */
    public static ServiceLocator GetInstance(){
        return instance;
    }
}