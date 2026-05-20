package jp.jamsketch.web;

/**
 * サーバーの送信用パラメータ
 */
class ServerParameter {

    // 操作モード
    // 現在は"reset"のみ
    public String mode;

    /**
     * デフォルトコンストラクタ
     * これが無いとJSONをデリシアライズできない
     */
    public ServerParameter(){
        
    }

    /**
     * コンストラクタ
     * @param mode 操作モード
     */
    public ServerParameter(String mode){
        this.mode = mode;
    }
}