package jp.jamsketch.web;

import jakarta.websocket.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.jamsketch.main.JamSketchEventListner;

/**
 * WebSocketのクライアントクラス
 */
@ClientEndpoint
class WebSocketClient {

    // セッション
    private Session session;

    // イベントリスナー
    private JamSketchEventListner listner;

    /**
     * コンストラクタ
     * @param listner イベントリスナー
     */
    public WebSocketClinet() {
        super();
    }

    /**
     * 接続時のコールバック
     * @param session セッション
     */
    @OnOpen
    public void onOpen(Session session) {
    }

    /**
     * メッセージ受信時のコールバック
     * @param message メッセージ
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
		
		try{
			def serviceLocator = ServiceLocator.GetInstance();
			def controller = serviceLocator.getContoller();
			ObjectMapper mapper = new ObjectMapper();
			ServerParameter info = mapper.readValue(message, ServerParameter.class);
		
        	if (info.mode == "reset"){
                controller.reset();
            }
		}
		catch (Exception e){
			System.out.println(e);
		}
    }

    /**
     * エラー時のコールバック
     * @param th エラー内容
     */
    @OnError
    public void onError(Throwable th) {
        this.listner.disconnected();
    }

    /**
     * 終了時のコールバック
     * @param session セッション
     */
    @OnClose
    public void onClose(Session session) {
        this.listner.disconnected();
    }

    /**
     * メッセージ送信処理
     * @param object 送信オブジェクト
     */
    public void Send(Object object) {
        this.session.getBasicRemote().sendText(new ObjectMapper().writeValueAsString(object));
    }

    /**
     * 初期化処理
     * @param host 接続先ホスト
     * @param port 接続先ポート
     */
    public void Init(String host, int port, JamSketchEventListner listner) {
      // 初期化のため WebSocket コンテナのオブジェクトを取得する
      WebSocketContainer container = ContainerProvider
              .getWebSocketContainer();
      // サーバー・エンドポイントの URI
      URI uri = URI
              .create("ws://" + host + ":" + port + "/websockets/WebSocketApi");
      this.session = container.connectToServer(this, uri);
      
      this.listner = listner;
    }
}