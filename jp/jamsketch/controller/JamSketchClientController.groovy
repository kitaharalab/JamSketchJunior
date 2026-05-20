package jp.jamsketch.controller;

import java.net.URI;
import java.util.Scanner;

import javax.websocket.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.jamsketch.main.JamSketchEventListner;
import jp.jamsketch.web.WebSocketClient;
import jp.jamsketch.web.ClientParameter;

/**
 * JamSketchを操作するクラスにデータ送信機能を持たせたもの
 * JamSketchの操作自体は委譲によって実現する
 */
public class JamSketchClientController implements IJamSketchController{

    // 内部で持つ操作クラス
    private IJamSketchController innerController;

    // WebSocketクライアントクラス
    private WebSocketClient webSocketClient;

    // 接続先ホスト
    private String host;

    // 接続先ポート
    private int port;

    // イベントリスナー
    private JamSketchEventListner listner;

    /**
     * コンストラクタ
     * @param host 接続先ホスト
     * @param port 接続先ポート
     * @param controller 内部で持つ操作クラス
     * @param listner イベントリスナー
     */
    public JamSketchClientController(String host, int port, IJamSketchController controller, JamSketchEventListner listner){
      this.innerController = controller;
      this.webSocketClient = new WebSocketClient();
      this.host = host;
      this.port = port;
      this.listner = listner;  
      this.init();
    }

    /**
     * 初期化する
     */
    public void init(){      
      this.webSocketClient.Init(this.host, this.port, this.listner);
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
      this.innerController.updateCurve(from, thru, y);

      // WebSocketで更新情報をサーバーに送る
      this.webSocketClient.Send(new ClientParameter(from, thru, y));
    }

    /**
     * リセットする
     */
    @Override
    public void reset(){
        // 内部で持つ操作クラスでリセット操作をする
        // クライアントはリセット時にデータは送らないため、他の処理はしない
        this.innerController.reset();
    }
}