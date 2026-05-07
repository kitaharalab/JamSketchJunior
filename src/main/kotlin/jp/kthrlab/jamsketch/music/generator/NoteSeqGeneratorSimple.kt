package jp.kthrlab.jamsketch.music.generator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jp.crestmuse.cmx.inference.MusicCalculator
import jp.crestmuse.cmx.inference.MusicElement
import jp.crestmuse.cmx.inference.MusicRepresentation
import jp.crestmuse.cmx.misc.ChordSymbol2
import jp.kthrlab.jamsketch.deprecated.model.CurveData
import jp.kthrlab.jamsketch.deprecated.model.MelodyData
import jp.kthrlab.jamsketch.deprecated.model.engine.IMelodyGenerateEngine
import jp.kthrlab.jamsketch.deprecated.model.melodygenerator.AbstractNoteSeqGenerator
import java.io.File

/**
 * This class uses a trigram or bigram model to select notes and generate melodies.
 * Calculate rhythm and note scores based on a given outline or chord progression to generate the optimal melody.
 */
class NoteSeqGeneratorSimple(
    private val noteLayer: String = "",
    private val chordLayer: String = "",
    private val beatsPerMeas: Int = 0,
    private val modelPath: String = "",
    private val entropy_bias: Double = 0.0,
    private val w1: Double = 0.5,
    private val w2: Double = 0.8,
    private val w3: Double = 1.2,
    private val w4: Double = 2.0,
    private val rhythm_weights: List<Double> = listOf(1.0, 0.2, 0.4, 0.8, 0.2, 0.4, 1.0, 0.2, 0.4, 0.8, 0.2, 0.4),
    private val rhythm_thrs: Double = 0.1,
): AbstractNoteSeqGenerator(), MusicCalculator {

    private var trigram: Map<String, List<Double>>
    private var bigram: List<List<Double>>
    private var chord_beat_dur_unigram: Map<String, List<Double>>
    private var entropy_mean: Double
    lateinit var model: MutableMap<String, Any?>

    init {
        initModel()
        trigram = model["trigram"] as Map<String, List<Double>>
        bigram = model["bigram"] as List<List<Double>>
        chord_beat_dur_unigram = model["chord_beat_dur_unigram"] as Map<String, List<Double>>
        entropy_mean = (model["entropy"] as LinkedHashMap<*,*>)["mean"] as Double
    }

    private fun initModel() {
        val mapper = jacksonObjectMapper()
        val jsonFile = File(javaClass.getResource("/${modelPath}").toURI())
        model = mapper.readValue(jsonFile)
    }

    override fun generateMusicDataFromCurveData(curveData: CurveData, engine: IMelodyGenerateEngine): MelodyData {
        TODO("Not yet implemented")
    }

    override fun generateMusicDataFromLength(noteLength: Int, engine: IMelodyGenerateEngine): MelodyData {
        TODO("Not yet implemented")
    }

    override fun generateMusicDataFromMeasure(measureLength: Int, engine: IMelodyGenerateEngine): MelodyData {
        TODO("Not yet implemented")
    }

    /**
     * Generate notes based on outlines.
     * CMX calls back to this method.
     */
    override fun updated(measure: Int, tick: Int, layer: String, mr: MusicRepresentation) {
        val e_curve = mr.getMusicElement(layer, measure, tick)
        val value = e_curve.mostLikely as Double
        if (!value.isNaN()) {
            val e_melo = mr.getMusicElement(noteLayer, measure, tick)
            val b = decideRhythm(value, prev(e_curve, 1, Double.NaN) as Double, tick, e_melo, mr)
            if (!b) {
                val prev1 = prev(e_melo, 1, -1) as Int
                val prev2 = prev(e_melo, 2, -1) as Int
                val c = mr.getMusicElement(chordLayer, measure, tick).mostLikely as ChordSymbol2
                val scores: MutableList<Double?> = MutableList(12){null}
                val prevlist: MutableList<Int> = ArrayList()

                for (i in 0 until tick) {
                    prevlist.add(
                        mr.getMusicElement(noteLayer, measure, i).mostLikely as Int
                    )
                }

                (0..11).forEach { i ->
                    var value12 = value - (value / 12).toInt() * 12
                    var simil = -Math.log((value12 - i) * (value12 - i))
                    var logtrigram = calcLogTrigram(i, prev1 as Int, prev2 as Int)
                    var logchord = calcLogChordBeatUnigram(i, c, tick, beatsPerMeas,
                        e_melo.duration(), mr)
                    var entdiff = calcEntropyDiff(i, prevlist)
                    scores[i] = w1 * simil + w2 * logtrigram + w3 * logchord +
                            w4 * (-entdiff)
                }
                e_melo.setEvidence(argmax(scores.toList() as List<Double>))
            }
        }
    }

    private fun decideRhythm(value: Double, prev: Double, tick: Int, e: MusicElement, mr: MusicRepresentation): Boolean {
        e.setTiedFromPrevious(false)
        if (!value.isNaN() && !prev.isNaN()) {
            val score = Math.abs(value - prev) * rhythm_weights[tick]
            if (score < rhythm_thrs) {
                e.setTiedFromPrevious(true)
                return true
            }
        }
        return false
    }

    /**
     * Calculate scores based on trigram models.
     */
    private fun calcLogTrigram(number: Int, prev1: Int, prev2: Int): Double {
        if (prev2 == -1) {
            if (prev1 == -1) {
                return 0.0
            } else {
                println("      $prev1")
                println("      $number")
                println(bigram)
                println(bigram[prev1][number])
                return Math.log(bigram[prev1][number])
            }
        } else {
            val key = "$prev2,$prev1"
            return if (trigram.containsKey(key)) {
                trigram[key]!![number]
            } else {
                Math.log(0.001)
            }
        }
    }

    /**
     * Calculate score based on chord progression and beats.
     */
    private fun calcLogChordBeatUnigram(
        number: Int,
        chord: ChordSymbol2,
        tick: Int,
        beatsPerMeas: Int,
        duration: Int,
        mr: MusicRepresentation
    ): Double {
        val s_chord = chord.toString()
        val div4 = mr.division / beatsPerMeas
        val s_beat = (if (tick == 0) "head" else (if (tick % div4 == 0) "on" else "off"))
        val s_dur = (if (duration >= 2 * div4) "long" else (if (duration >= div4) "mid" else "short"))
        val key = s_chord + "_" + s_beat + "_" + s_dur
        return Math.log(chord_beat_dur_unigram[key]!![number] + 0.001)
    }

    private fun calcEntropyDiff(number: Int, prevlist: List<Int>): Double {
        val entropy = calcEntropy(number, prevlist)
        val entdiff = entropy - entropy_mean - entropy_bias
        return entdiff * entdiff
    }

    /**
     * Calculate the difference in entropy to influence note selection.
     */
    private fun calcEntropy(number: Int, prevlist: List<Int>): Double {
        val freq = MutableList(12){0}
        var sum = 0
        prevlist.forEach {
            freq[it] += 1
            sum++
        }
        freq[number] += 1
        sum++

        var entropy = 0.0
        (0..11).forEach { i ->
            if (freq[i] > 0) {
                val p: Double = freq[i].toDouble() / sum
                entropy += -Math.log(p) * p / Math.log(2.0)
            }
        }
        return entropy
    }

    /**
     * Returns the index with the highest value from the score list.
     */
    private fun argmax(list: List<Double>): Int {
        var max = list[0]
        var index = 0
        list.forEachIndexed { i, x ->
            if (x > max!!) {
                max = x
                index = i
            }
        }
        return index
    }

    private fun prev(e: MusicElement?, rep: Int, ifnull: Any): Any {
        return if (e == null) {
            ifnull
        } else if (rep == 0) {
            e.mostLikely
        } else {
            prev(e.prev(), rep - 1, ifnull)
        }
    }


}