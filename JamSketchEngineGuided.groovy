import jp.crestmuse.cmx.filewrappers.*

class JamSketchEngineGuided extends JamSketchEngineAbstract {

  def musicCalculatorForOutline() {
    def chGuide = cfg.CHANNEL_GUIDE
    def partGuide = scc.getFirstPartWithChannel(chGuide)
    return new NoteSeqGeneratorGuided(MELODY_LAYER, CHORD_LAYER, partGuide,
    mr, cfg.INITIAL_BLANK_MEASURES, cfg.BEATS_PER_MEASURE)
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