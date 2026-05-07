package jp.jamsketch.main;

import jp.crestmuse.cmx.filewrappers.*

class JamSketchEngineSimple extends JamSketchEngineAbstract {
    
  def musicCalculatorForOutline() {
    new NoteSeqGenerator(MELODY_LAYER, CHORD_LAYER, cfg.BEATS_PER_MEASURE,
			 cfg.ENT_BIAS, model)
  }

  def outlineUpdated(measure, tick) {
    // do nothing
  }

  def automaticUpdate() {
    true
  }

  Map<String,Double> parameters() {
    [:]
  }

  Map<String,String> paramDesc() {
    [:]
  }

}
