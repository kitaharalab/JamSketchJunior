import jp.crestmuse.cmx.filewrappers.*
import jp.crestmuse.cmx.misc.*

interface JamSketchEngine {

  void init(SCC scc, SCC.Part target_part, def cfg)
  void setMelodicOutline(int measure, int tick, double value)
  double getMelodicOutline(int measure, int tick)
  ChordSymbol2 getChord(int measure, int tick)
  void setFirstMeasure(int number)
  void resetMelodicOutline()
  // Map<String,Double> parameters()
  // Map<String,String> kamnn()
}
