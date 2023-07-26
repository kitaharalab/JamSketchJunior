import groovy.transform.*
import jp.crestmuse.cmx.inference.*
import jp.crestmuse.cmx.processing.CMXController

class SCCGenerator implements MusicCalculator {

  def CFG
  def target_part
  def sccdiv
  def curveLayer
  def expgen
  static def firstMeasure = 0
  
  SCCGenerator(target_part, sccdiv, curveLayer, expgen, CFG) {
    this.CFG = CFG
    this.target_part = target_part
    this.sccdiv = sccdiv
    this.curveLayer = curveLayer
    this.expgen = expgen
  }

  void updated(int measure, int tick, String layer,
		 MusicRepresentation mr) {
      //      def sccdiv = scc.getDivision()
      //def firstMeasure = pianoroll.getDataModel().getFirstMeasure()
      def e = mr.getMusicElement(layer, measure, tick)
      if (!e.rest() && !e.tiedFromPrevious()) {
	//def curvevalue = curve2[measure * CFG.DIVISION + tick]
		def curvevalue =
		  mr.getMusicElement(curveLayer, measure, tick).getMostLikely()
		if (curvevalue != null) {
		// int notenum = getNoteNum(e.getMostLikely(), curvevalue)
		int notenum = e.getMostLikely()
		// println(notenum)
		// println(layer)
		int duration = e.duration() * sccdiv /
		(CFG.DIVISION / CFG.BEATS_PER_MEASURE)
		int onset = ((firstMeasure + measure) * CFG.DIVISION + tick) * sccdiv /
		(CFG.DIVISION / CFG.BEATS_PER_MEASURE)
		//	if (onset > pianoroll.getTickPosition()) {
			synchronized(this) {
				if (onset > CMXController.getInstance().getTickPosition()) {
					//	    def oldnotes =
					//	      SCCUtils.getNotesBetween(target_part, onset,
					//				       onset+duration, sccdiv, true, true)
					//data.target_part.getNotesBetween2(onset, onset+duration)
					//	      target_part.remove(oldnotes)
					// edit 2020.03.04
					target_part.eachnote { note ->

						if (note.onset() < onset && onset <= note.offset()) {
							target_part.remove(note)
//							println("cond1 remove ${note}")
							target_part.addNoteElement(note.onset(), onset-1,
									note.notenum(),
									note.velocity(),
									note.offVelocity())
//							println("cond1 add ${note.onset}, ${onset-1}, ${note.notenum()}")
						}
						if (onset <= note.onset() && note.offset() <= onset+duration) {
							target_part.remove(note)
//							println("cond2 remove ${note}")
						}
						if (note.onset() < onset+duration &&
								onset+duration < note.offset()) {
							//		  note.setOnset(onset+duration)
						}
					}
					target_part.addNoteElement(onset, onset+duration, notenum+CFG.TF_NOTE_NUM_START,
							100, 100)
					println("all add ${onset}, ${onset+duration}, ${notenum}")
					//	  }
				}
			}
		}
      }

      if (CFG.EXPRESSION) {
	def fromTick = (firstMeasure + measure) * CFG.BEATS_PER_MEASURE *
	  CFG.DIVISION
	def thruTick = fromTick + CFG.BEATS_PER_MEASURE * CFG.DIVISION
	expgen.execute(fromTick, thruTick, CFG.DIVISION)
      }
    }

//   @CompileStatic
//   int getNoteNum(int notename, double neighbor) {
//     int best = 0
//     for (int i in 0..11) {
//       def notenum = i * 12 + notename
//       if (Math.abs(notenum - neighbor) < Math.abs(best - neighbor)) 
// 	best = notenum
//     }
//     best
//   }

}
