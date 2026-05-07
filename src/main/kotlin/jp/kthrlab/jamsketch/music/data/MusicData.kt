package jp.kthrlab.jamsketch.music.data

import java.io.File
import jp.crestmuse.cmx.filewrappers.SCC
import jp.crestmuse.cmx.processing.CMXController

/**
 * 音楽生成の入出力データ
 *
 * @param filename                  伴奏ファイル名
 * @param size                     カーブの座標を保持するリストのサイズ（タイムラインのwidth）
 * @param initial_blank_measures
 * @param beats_per_measure
 * @param num_of_measures
 * @param repeat_times
 * @param division                  TODO: scc.division との違いを確認
 */
class MusicData(
    filename: String?,
    val size: Int,
    val initial_blank_measures: Int,
    val beats_per_measure: Int,
    val num_of_measures: Int,
    val repeat_times: Int,
    val division: Int,
    val channel_acc: Int = 0,
) {
    var curve1: MutableList<Int?> = arrayOfNulls<Int>(size).toMutableList()
    var scc: SCC = CMXController.readSMFAsSCC(File(javaClass.getResource("/${filename}").toURI()).path)

    init {
        scc.toDataSet().repeat(
            (initial_blank_measures * beats_per_measure * scc.division).toLong(),
            ((initial_blank_measures + num_of_measures) * beats_per_measure * scc.division).toLong(),
            repeat_times - 1
        )
    }

    fun resetCurve() {
        curve1 = arrayOfNulls<Int>(size).toMutableList()
    }

    fun storeCursorPosition(from: Int, thru: Int, y: Int) {
        (from..thru).forEach { i: Int -> curve1[i] = y }
    }

    fun storeCursorPosition(i: Int, y: Int) {
        curve1[i] = y
    }

}
