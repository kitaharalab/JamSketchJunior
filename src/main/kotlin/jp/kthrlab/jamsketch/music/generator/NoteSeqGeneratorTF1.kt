package jp.kthrlab.jamsketch.music.generator

import jp.crestmuse.cmx.inference.MusicCalculator
import jp.crestmuse.cmx.inference.MusicRepresentation
import jp.crestmuse.cmx.misc.ChordSymbol2
import org.tensorflow.SavedModelBundle
import org.tensorflow.ndarray.FloatNdArray
import org.tensorflow.ndarray.NdArrays
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import java.io.File

class NoteSeqGeneratorTF1(
    private val num_of_measures: Int,
    private val division: Int,
    private val melody_execution_span: Int,
    private val tf_model_dir: String,
    private val tf_model_layer: String,
    private val tf_note_num_start: Int,
    private val tf_model_input_col: Int,
    private val tf_model_output_col: Int,
    private val tf_rest_col: Int,
    private val tf_chord_col_start: Int,
    private val tf_num_of_melody_element: Int,
) : MusicCalculator {
    private var lastUpdateMeasure = -1
    private var lastUpdateTime = -1L
    private val CHORD_VECTORS = mapOf(
        "C" to floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f),
        "F" to floatArrayOf(1f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f),
        "G" to floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 1f)
    )

    private var tfModel = SavedModelBundle.load(File(javaClass.getResource("/${tf_model_dir}").toURI()).path)

    override fun updated(measure: Int, tick: Int, layer: String?, mr: MusicRepresentation?) {

        synchronized(tfModel) {

            val currentTime = System.nanoTime()
            if (currentTime - lastUpdateTime >= 1000 * 1000 * melody_execution_span.toLong()) {

                mr!!.getMusicElement("curve", measure, tick).resumeUpdate()

                lastUpdateMeasure = measure
                lastUpdateTime = currentTime

                val tf_input = preprocessing(measure, mr)
		//println(TFloat32.tensorOf(tf_input).shape().toString())
                val tf_output = tfModel.session()
                    .runner()
                    .feed(tf_model_layer, TFloat32.tensorOf(tf_input))
                    .fetch("StatefulPartitionedCall")
                    .run()
                    .get(0) as TFloat32
                var normalized_data = normalize(tf_output)
                setEvidences(measure, tick, normalized_data, mr)
            }
        }
    }

    //preprocessing input data
    private fun preprocessing(measure: Int, mr: MusicRepresentation): FloatNdArray {
        val nn_from = tf_note_num_start
        val tf_row = division.toLong()
        val tf_column = tf_model_input_col.toLong()

        val shape = Shape.of(1 , tf_row, tf_column, 1)
        val tf_input = NdArrays.ofFloats(shape)
        // TODO: need to init tf_input with 0.0f?

        val mes = mr.getMusicElementList("curve")
        val me_start = (mes.size / num_of_measures) * measure
        val me_end = me_start + division
        val me_per_measure = mes.subList(me_start, me_end)

        for (i in 0 until tf_row) {
            val note_num_f : Double = me_per_measure[i.toInt()].mostLikely as Double
            print("$note_num_f, ")
            if (!note_num_f.isNaN()) {
                val note_number = Math.floor(note_num_f) - nn_from
                tf_input[0][i][note_number.toLong()][0].setFloat(1.0f)
            } else {
                tf_input[0][i][tf_rest_col.toLong()][0].setFloat(1.0f)
            }

            //set chord
            val chord_column_from = tf_chord_col_start
            val chord = mr.getMusicElement("chord", measure, i.toInt()).mostLikely as ChordSymbol2//getChord(measure, i)
            val chord_vec = CHORD_VECTORS[chord.toString()]

            for (j in 0 until tf_num_of_melody_element) {
                val chord_value = chord_vec!![j]
                tf_input[0][i][(chord_column_from + j).toLong()][0].setFloat(chord_value)
            }
        }
        return tf_input
    }

    //Normalize the prediction data which if it is under 0.5, be 0, if it is over 0.5, be 1.
    private fun normalize (tf_output: TFloat32): TFloat32 {
        val tf_row = tf_output.shape()[1]
        val tf_column = tf_output.shape()[2]
        val size3 = (tf_column - 1) / 2

        // Create a 16x60 matrix.
        val tmp_output = NdArrays.ofFloats(Shape.of(1, tf_row, size3, 1))

        // Assign the elements from columns 60 to 120 (inclusive) of all rows in tf_output to tmp_output.
        for (i in 0 until tf_row) {
            for (j in size3 until tf_column - 1) {
                println("i==$i, j==$j, j-size3==${j-size3}")
                tmp_output[0][i][j - size3][0].setFloat(tf_output[0][i][j][0].getFloat())
            }
        }

        // Handle the processing of the function (make_note_msgs) in 24measure_MelodyGenerationDemo. ipynb.
        for (i in 0 until tf_row) {
            for (j in 0 until size3) {
//                println("tf_output[0][${i}][${j}][0] = ${tf_output[0][i][j][0]}")
//                println("tmp_output[0][${i}][${j}][0] = ${tmp_output[0][i][j][0]}")
                if (tf_output[0][i][j][0].getFloat() <= 0.5
                    && tmp_output[0][i][j][0].getFloat() > 0.5)
                {
                    if (i >= 1 && tmp_output[0][i - 1][j][0].getFloat() < 0.5 && tf_output[0][i - 1][j][0].getFloat() < 0.5) {
                        tf_output[0][i][j][0].setFloat(tmp_output[0][i][j][0].getFloat() + tf_output[0][i][j][0].getFloat())
                        tf_output[0][i][j + size3][0].setFloat(0.0f)
                    }
                }
            }
        }

        // For each value in tf_output, replace it with 1.0 if it's greater than or equal to 0.5, and 0 otherwise.
        for (i in 0 until tf_row) {
            for (j in 0 until tf_column) {
                if (tf_output[0][i][j][0].getFloat() >= 0.5) {
                    tf_output[0][i][j][0].setFloat(1.0f)
                } else {
                    tf_output[0][i][j][0].setFloat(0.0f)
                }
            }
        }
        return tf_output
    }

    /**
     * Input:
     *       tf_normalized: A matrix with a value of 0 or 1 in 16x121
     *       measure: Bar
     *       lasttick: A musical note
     *
     * Variables used internally:
     *       tf_normalized2: Stores data up to the 60th ~ 120th column of the tf_normalized.
     *
     * Output:
     *       tf_output: A matrix in which 0.5 or greater is replaced by 1 and values less than 0.5 are replaced by 0.
     *
     */
    private fun setEvidences(measure: Int, lastTick: Int, tf_normalized: TFloat32, mr: MusicRepresentation) {

        val tf_row = division
        val tf_column = tf_model_output_col
        val tf_normalized2 = NdArrays.ofFloats(Shape.of(1, tf_row.toLong(), ((tf_column - 1) / 2).toLong(), 1))

        for (i in 0 until tf_row.toLong()) {
            for (j in 60 until tf_column.toLong() - 1) {
                tf_normalized2[0][i][(j - 60).toLong()][0].setFloat(tf_normalized[0][i][j][0].getFloat())
            }
        }

        for (i in 0..lastTick) {
            val e = mr.getMusicElement("melody", measure, i)

            if (tf_normalized[0][i.toLong()][120][0].getFloat() == 1.0f) {
                e.setRest(true)
            } else {
                e.setRest(false)

                if (i >= 1) {
                    for (j in 0 until (tf_column - 1) / 2) {
                        if ((tf_normalized2[0][(i - 1).toLong()][j.toLong()][0].getFloat() == 1.0f || tf_normalized[0][(i - 1).toLong()][j.toLong()][0].getFloat() == 1.0f) && tf_normalized2[0][i.toLong()][j.toLong()][0].getFloat() == 1.0f) {
                            e.setTiedFromPrevious(true)
                        } else if (tf_normalized[0][i.toLong()][j.toLong()][0].getFloat() == 1.0f) {
                            e.setEvidence(j)
                        }
                    }
                }
            }
        }
    }

}