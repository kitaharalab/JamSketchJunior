package jp.jamsketch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;


import jp.jamsketch.web.WebSocketApi;
import jp.jamsketch.web.ServerParameter;
import jp.jamsketch.web.ServiceLocator;

import org.glassfish.tyrus.server.Server;

/**
 * JamSketchを操作するクラスにサーバー送信機能を持たせたもの
 * JamSketchの操作自体は委譲によって実現する
 */
public class JamSketchServerController implements IJamSketchController{

    // 内部で持つ操作クラス
    private IJamSketchController innerController;
    
    // WebSocketサーバークラス
    private Server server;

    /**
     * コンストラクタ
     * @param host 自身のホスト
     * @param port 自身のポート
     * @param controller 内部で持つ操作クラス
     */
    public JamSketchServerController(String host, int port, IJamSketchController controller){
        this.innerController = controller;
        this.server = new Server(host, port, "/websockets", null, WebSocketApi.class);
        this.init();
    }

    /**
     * 初期化する
     */
    public void init(){
        this.server.start();
    }

    /**
     * Curveを更新する
     * @param from 始点
     * @param thru 終点
     * @param y Y座標
     */
    @Override
    public void updateCurve(int from, int thru, int y){
      // 内部で持つ操作クラスで更新操作をする
      // サーバーは更新時にデータは送らないため、他の処理はしない
      this.innerController.updateCurve(from, thru, y);
    }

    /**
     * リセットする
     */
    @Override
    public void reset(){
        // 内部で持つ操作クラスでリセット処理をする
        this.innerController.reset();
	resetClients();
    }

    public void resetClients() {
        def serviceLocator = ServiceLocator.GetInstance();
    	def currentSession = serviceLocator.getSession();
        Set<Session> sessions = currentSession.getOpenSessions();
	for (Session session : sessions) {
	    try {
	    	session.getBasicRemote().sendText(new ObjectMapper().writeValueAsString(new ServerParameter("reset")));
                System.out.println(new ObjectMapper().writeValueAsString(new ServerParameter("reset")));
   	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }
}