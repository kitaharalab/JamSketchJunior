package jp.jamsketch.web;

/**
 * クライアントの送信用パラメータ
 */
class ClientParameter{
    // 始点
    public int from;

    // 終点
    public int thru;

    // Y座標
	public int y;

    /**
     * デフォルトコンストラクタ
     * これが無いとJSONをデリシアライズできない
     */
    public ClientParameter(){
        
    }

    /**
     * コンストラクタ
     * @param from 始点
     * @param thru 終点
     * @param y Y座標
     */
    public ClientParameter(int from, int thru, int y){
        this.from = from;
        this.thru = thru;
        this.y = y;
    }
}