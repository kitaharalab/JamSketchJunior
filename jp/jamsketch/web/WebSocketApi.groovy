package jp.jamsketch.web;

import java.io.IOException;
import java.util.Set;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WebSocketのサーバークラス
 */
@ServerEndpoint("/WebSocketApi")
class WebSocketApi {

	// 現在のセッション
	Session currentSession = null;
	
	/**
     * 接続時のコールバック
     * @param session セッション
	 * @param ec エンドポイントの設定
     */
	@OnOpen
	public void onOpen(Session session, EndpointConfig ec) {
		// セッションを記録する
		this.currentSession = session;
		def serviceLocator = ServiceLocator.GetInstance();
    	serviceLocator.setSession(this.currentSession);
	}
	
	/**
     * メッセージ受信時のコールバック
     * @param message メッセージ 
     */
	@OnMessage
	public void receiveMessage(String message) throws IOException {
		// メッセージをコンソールに出力する
		System.out.println(message);
		
		try{
			// 操作クラスのインスタンスを取得する
			def serviceLocator = ServiceLocator.GetInstance();
			def controller = serviceLocator.getContoller();

			// JSONで送られたメッセージをデコードする
			ObjectMapper mapper = new ObjectMapper();
			ClientParameter info = mapper.readValue(message, ClientParameter.class);
		
		    // 操作クラスを使って楽譜データを更新する
        	controller.updateCurve(info.from, info.thru, info.y);
		}
		catch (Exception e){
			// 例外をコンソールに出力する
			System.out.println(e);
		}
	}

	/**
     * 接続終了時のコールバック
     * @param session セッション
	 * @param reason 終了原因
     */
	@OnClose
	public void onClose(Session session, CloseReason reason) {
	}

	/**
     * 接続エラー時のコールバック
     * @param t エラー内容
     */
	@OnError
	public void onError(Throwable t) {
	}
}