package jp.jamsketch.model.engine

import jp.jamsketch.model.CurveData
import jp.jamsketch.model.MelodyData
import jp.crestmuse.cmx.inference.MusicRepresentation


/**
 * A MelodyGenerateEngine that generates simple melodies
 */
class SimpleMelodyGenerateEngine : AbstractMelodyGenerateEngine(modelPath = "") {
    private lateinit var model:Any


    override fun outlineUpdated(measure: Int, tick: Int) {
        TODO("Not yet implemented")
    }

    override fun getNextNote(curveData: CurveData, melodyData: MelodyData?) {
        TODO("Not yet implemented")

    override fun loadModel(modelPath: String) {
        TODO("Not yet implemented")
    }

    override fun getNextNote(curveData: CurveData, mr: MusicRepresentation) {

    }
}