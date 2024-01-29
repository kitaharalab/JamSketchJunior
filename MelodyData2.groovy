class MelodyData2 {
  def width
  def pianoroll
  def engine
  def curve1
  def scc
  def cfg
  
  MelodyData2(filename, width, cmxcontrol, pianoroll, cfg) {
    this.width = width
    this.pianoroll = pianoroll
    this.cfg = cfg
    scc = cmxcontrol.readSMFAsSCC(filename)
    scc.repeat(cfg.INITIAL_BLANK_MEASURES * cfg.BEATS_PER_MEASURE *
	       scc.division,
	       (cfg.INITIAL_BLANK_MEASURES + cfg.NUM_OF_MEASURES) *
	       cfg.BEATS_PER_MEASURE * scc.division, cfg.REPEAT_TIMES - 1)
    def target_part = scc.getFirstPartWithChannel(1)
//    engine = new JamSketchEngineSimple()
    engine = Class.forName(cfg.JAMSKETCH_ENGINE).newInstance()
    engine.init(scc, target_part, cfg)
    resetCurve()
  }

  def resetCurve() {
    curve1 = [null] * width
    engine.resetMelodicOutline()
//    println("Curve reset: ${curve1}")
  }


  def updateCurve(int from, int thru) {
    println("updateCurve called")
    int nMeas = cfg.NUM_OF_MEASURES
    int div = cfg.DIVISION
    int size2 = nMeas * div

    for (int i in from..thru) {
      int ii = i - cfg.getKeyboardWidth
      int position = (int)(ii * size2 / (width))

      if (curve1[i - cfg.getKeyboardWidth] != null) {
        double nn = pianoroll.y2notenum(curve1[i - cfg.getKeyboardWidth])
        if (position >= 0 && position < size2) {
          engine.setMelodicOutline((int)(position / div), position % div, nn)
        }
      }
    }
//    println("Curve reset in updatedC: ${curve1}")
  }
}