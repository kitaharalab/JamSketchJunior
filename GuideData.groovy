import groovy.json.JsonSlurper
import static JamSketch.CFG

class GuideData {
    def curveGuide
    def curveGuideView // y coord per pixel
    def from = 0
    def size
    def scc
    def pianoroll

    GuideData(filename, size, pianoroll) {
        this.size = size
	this.pianoroll = pianoroll
        scc = pianoroll.readSMFAsSCC(filename)
        def guide_part = scc.getFirstPartWithChannel(CFG.CHANNEL_GUIDE)
	def smoothness = CFG.GUIDE_SMOOTHNESS as int
        curveGuide = createCurve(guide_part, smoothness)
        updateCurve(from, size)
    }

    def createCurve(part, smoothness) {
	def curve = [null] * (size * 4)	// 4 is temporary
	def beats = CFG.BEATS_PER_MEASURE
	def initial = CFG.INITIAL_BLANK_MEASURES
	part.eachnote { note ->
            try {
	    def y = pianoroll.notenum2y(note.notenum() - 0.5)
            def onset = note.onset(480) / 480
	    def m1 = (onset / beats) as int
	    def b1 = onset - m1 * beats
	    def x1 = pianoroll.beat2x(m1 - initial, b1) - 100// 100 is temporary
	    def offset = note.offset(480) / 480
	    def m2 = (offset / beats) as int
	    def b2 = offset - m2 * beats
	    def x2 = pianoroll.beat2x(m2 - initial, b2) - 100
	    for (x in x1..x2) {
	        curve[x] = y
            }
	    } catch (UnsupportedOperationException e){}
	}
	return smoothCurve(curve, smoothness)
    }

    def smoothCurve(curve, K) {
      println("K="+K)
      def curve2 = curve.clone()
      for (i in 0..<curve.size()) {
         if (curve[i] != null) {
         def n = 1
         for (k in 1..K) {
	   if (i - k >= 0 && curve[i-k] != null) {
	     curve2[i] += curve[i-k]
	     n++
	   }
	   if (i + k < curve.size() && curve[i+k] != null) {
	     curve2[i] += curve[i+k]
	     n++
	   }
	 }
	 curve2[i] /= n
	 }
       }
       curve2
     }

    def shiftCurve() {
        curveGuideView = [null] * size
        updateCurve(from += size, from + size)
    }

    def updateCurve(from, to) {
        if (from < curveGuide.size()) {
            def toIndex = (to <= curveGuide.size()) ? to : curveGuide.size()
//            println("from = ${from}, toIndex = ${toIndex}")
            curveGuideView = curveGuide.subList(from, toIndex)
//            println(curveGuideView)
        }
    }

}