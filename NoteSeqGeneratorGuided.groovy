import jp.crestmuse.cmx.inference.*
import jp.crestmuse.cmx.inference.models.*

class NoteSeqGeneratorGuided implements MusicCalculator {
  String noteLayer, chordLayer
  def guidepart
  def mr
  def initialBlank
  def beatsPerMeas
  def bigram
  def unigram1

  def w1 = 1.0
  def w2 = 1.0

  NoteSeqGeneratorGuided(noteLayer, chordLayer, guidepart, mr, initial,
  				    beatsPerMeas) {
    this.noteLayer = noteLayer
    this.chordLayer = chordLayer
    this.guidepart = guidepart
    this.mr = mr
    this.beatsPerMeas = beatsPerMeas
    this.initialBlank = initial * beatsPerMeas
    makeBigram()
    decideRhythm()
  }

  def makeBigram() {
    unigram1 = [0] * 12
    bigram = []
    12.times { bigram.add([0] * 12) }
//    println(unigram1)
//    println(bigram)
    def prev = null
    guidepart.eachnote { note ->
      try {
        def nn = note.notenum() % 12
	if (prev == null) {
	  unigram1[nn] += 1
	} else {
	  bigram[prev][nn] += 1
	}
	prev = nn
      } catch (UnsupportedOperationException e) {}
    }
    unigram1 = unigram1.collect{ (double)it / unigram1.sum() }
    (0..11).each { i ->
      bigram[i] = bigram[i].collect{ (double)it / bigram[i].sum() }
    }
  }

//  def decideRhythm() {
//     // TO DO: how to deal with rest?
//    guidepart.eachnote { note ->
//      def onset = note.onset(480) / 480 - initialBlank
//      def i1 = (onset * mr.division / beatsPerMeas) as int
//      def offset = note.offset(480) / 480 - initialBlank
//      def i2 = (offset * mr.division / beatsPerMeas) as int
//      println("Note!!!!: onset=$onset, i1=$i1, offset=$offset, i2=$i2")
//
//      if (i1 < mr.measureNum * mr.division) {
//        mr.getMusicElement(noteLayer, 0, i1).setTiedFromPrevious(false)
//      }
//      for ( i in i1+1..<i2) {
//      	if (i < mr.measureNum * mr.division) {
//          println("Setting tied for index $i")
//          mr.getMusicElement(noteLayer, 0, i).setTiedFromPrevious(true)
//	}
//      }
//    }
//  }

  def decideRhythm() {
    // TO DO: how to deal with rest?
    guidepart.eachnote { note ->
      def onset = note.onset(480) / 480 - initialBlank
      def i1 = Math.max(0, (onset * mr.division / beatsPerMeas) as int)  // Ensure non-negative index
      def offset = note.offset(480) / 480 - initialBlank
      def i2 = Math.max(0, (offset * mr.division / beatsPerMeas) as int)  // Ensure non-negative index

//      println("Note: onset=$onset, i1=$i1, offset=$offset, i2=$i2")

      if (i1 < mr.measureNum * mr.division) {
        mr.getMusicElement(noteLayer, 0, i1).setTiedFromPrevious(false)
      }

      for (i in i1 + 1..<i2) {
        if (i < mr.measureNum * mr.division) {
          println("Setting tied for index $i")
          mr.getMusicElement(noteLayer, 0, i).setTiedFromPrevious(true)
        }
      }
    }
  }


  void updated(int measure, int tick, String layer, MusicRepresentation mr) {
//    println("measure ${measure}")
//    println("tick ${tick}") // 8 ticks in a measure?

    MusicElement e_curve = mr.getMusicElement(layer, measure, tick)//e_curve represents the element at the specified position in the layer, which is likely the layer where the blue curve information is stored
    double value = e_curve.getMostLikely() as double
    def score = []
    if (!Double.isNaN(value)) {
      MusicElement e_melody = mr.getMusicElement(noteLayer, measure, tick) //  the noteLayer, which is likely the layer where the yellow melody bars are stored.
      double value12 = value - (int)(value / 12) * 12
      (0..11).each { i -> // 0-11 represents each half note from C to B
        double simil = -Math.log((value12 - i) * (value12 - i)) // simil is the closer note value on Y axis to the line which users draw
//	    println("simil ${simil}")
        def prev = e_melody.prev()
	double logbigram =
	  prev == null ? calcLogBigram(i, null) : // LogBigram has probability values and this func calculates the value. If the bigger value, the more chance to get chosen.
	  calcLogBigram(i, e_melody.prev().mostLikely)
//        println("logbigram ${logbigram}")

        score[i] = w1 * simil + w2 * logbigram // To get guessed note . For example Change w1 values bigger, w2 zero, it only gives note along the line which users draw so be musically ignored
      }
      e_melody.setEvidence(argmax(score))
    }
  }

  def calcLogBigram(nn, prev) {
    // println(nn + " " + prev)
    if (prev == null)
      Math.log(unigram1[nn])
    else
      Math.log(bigram[prev][nn])
  }

  int argmax(List<Double> list) {
    double max = list[0]
    int index = 0
    list.eachWithIndex{x, i ->
      if (x > max) {
	max = x
	index = i
      }
    }
    index
  }

}