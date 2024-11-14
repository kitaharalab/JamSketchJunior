package jp.jamsketch.model.engine

import jp.crestmuse.cmx.inference.MusicRepresentation
import jp.jamsketch.model.CurveData
import jp.jamsketch.model.MelodyData

interface IMelodyGenerateEngine {
    /**
     * getNextNoteは @see[SimpleNoteSeqGenerator]で導き出された
     * 「更新するべき旋律の場所と音高」と「旋律を格納する器」をうけとり、旋律を生成する。
     *
     * @param curveData : 変更された部分のカーブデータ
     * @param mr : 更新する旋律を格納する
     */
    fun getNextNote(curveData: CurveData, mr: MusicRepresentation)
}