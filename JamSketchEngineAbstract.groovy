import groovy.json.*
import jp.crestmuse.cmx.filewrappers.*
import jp.crestmuse.cmx.processing.*
import jp.crestmuse.cmx.inference.*
import jp.crestmuse.cmx.misc.*
import static jp.crestmuse.cmx.misc.ChordSymbol2.*

abstract class JamSketchEngineAbstract implements JamSketchEngine {
  MusicRepresentation mr
  CMXController cmx
  def cfg
  def model
  def scc
  def expgen = null
  static String OUTLINE_LAYER = "curve"
  static String MELODY_LAYER = "melody"
  static String CHORD_LAYER = "chord"
  
  void init(SCC scc, SCC.Part target_part, def cfg) {
    this.scc = scc
    this.cfg = cfg
    def json = new JsonSlurper()
    model = json.parseText((new File(cfg.MODEL_FILE)).text)
    cmx = CMXController.getInstance()
    mr = cmx.createMusicRepresentation(cfg.NUM_OF_MEASURES,
                                       cfg.DIVISION)
    mr.addMusicLayerCont(OUTLINE_LAYER)
    // mr.addMusicLayer(MELODY_LAYER, (0..11) as int[])
    mr.addMusicLayer(MELODY_LAYER, (0..(cfg.TF_NOTE_CON_COL_START-1)) as int[])
    mr.addMusicLayer(CHORD_LAYER,
                     [C, F, G] as ChordSymbol2[],	// temporary
                     cfg.DIVISION)
    cfg.chordprog.eachWithIndex{ c, i ->
      mr.getMusicElement(CHORD_LAYER, i, 0).setEvidence(c)
    }
    if (cfg.EXPRESSION) {
       expgen = new ExpressionGenerator()
       expgen.start(scc.getFirstPartWithChannel(1),
	            getFullChordProgression(), cfg.BEATS_PER_MEASURE)
    }
    def sccgen = new SCCGenerator(target_part, scc.division,
    OUTLINE_LAYER, expgen, cfg)
    mr.addMusicCalculator(MELODY_LAYER, sccgen)
    def calc = musicCalculatorForOutline()
    if (calc != null) {
      mr.addMusicCalculator(OUTLINE_LAYER, calc)
    }
    init_local()
  }

  def init_local() {
    // do nothing
  }

  def getFullChordProgression() {
    [NON_CHORD] * cfg.INITIAL_BLANK_MEASURES + cfg.chordprog * cfg.REPEAT_TIMES
  }

  abstract def musicCalculatorForOutline()
  

  void setMelodicOutline(int measure, int tick, double value) {

    def e = mr.getMusicElement(OUTLINE_LAYER, measure, tick)
    if (!automaticUpdate()) {
      e.suspendUpdate()
    }
    e.setEvidence(value)
    outlineUpdated(measure, tick)
    
  }

  double getMelodicOutline(int measure, int tick) {
    mr.getMusicElement(OUTLINE_LAYER, measure, tick).
      getMostLikely()
  }

  abstract def outlineUpdated(measure, tick)

  abstract def automaticUpdate()
    
  void resetMelodicOutline() {
    (0..<cfg.NUM_OF_MEASURES).each { i ->
      (0..<cfg.DIVISION).each { j ->
	mr.getMusicElement(OUTLINE_LAYER, i, j).
          setEvidence(Double.NaN)
      }
    }
  }

  void setFirstMeasure(int num) {
    SCCGenerator.firstMeasure = num
  }

  ChordSymbol2 getChord(int measure, int tick) {
    mr.getMusicElement(CHORD_LAYER, measure, tick).
      getMostLikely()
  }
}
