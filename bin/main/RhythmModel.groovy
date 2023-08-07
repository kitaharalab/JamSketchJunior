import groovy.transform.*
import groovy.json.*
import org.apache.commons.math3.genetics.*
import jp.crestmuse.cmx.inference.*
import jp.crestmuse.cmx.inference.models.*
import java.util.concurrent.*
					 //import static JamSketch.CFG


class RhythmModel {

  class RhythmTree {
    int note, freq
    List<RhythmTree> next = []
    }

  RhythmTree rhythmtree
  Map<List<Integer>,Integer> rhythmunigram
  HMM ga
  RhythmGACalc gacalc
  int DIVISION
  int BEATS
  int GA_TIME
  double R_TH = 0.25
  //  double RHYTHM_DENSITY
  def engine
  
  RhythmModel(engine, CFG) {
    this.engine = engine
    def json = new JsonSlurper()
    def model = json.parseText((new File(CFG.MODEL_FILE)).text)
    rhythmtree = makeRhythmTree(model.rhythmtree)
//    rhythmunigram = model.rhythmunigram
    rhythmunigram = [:]
    model.rhythmunigram.each { k, v ->
      rhythmunigram[json.parseText(k)] = v
    }
    //    RHYTHM_DENSITY = CFG.RHYTHM_DENSITY
    gacalc = new RhythmGACalc()
    //    gacalc = new RhythmGACalc(engine.parameters().RHYTHM_DENSITY)
    ga = new HMMContWithGAImpl(gacalc,
			       (int)(CFG.GA_POPUL_SIZE / 2),
			       CFG.GA_POPUL_SIZE, (double)0.2,
			       new UniformCrossover(0.5), (double)0.8,
			       new BinaryMutation(), (double)0.2,
			       new TournamentSelection(10))
    DIVISION = CFG.DIVISION
    BEATS = CFG.BEATS_PER_MEASURE
    GA_TIME = CFG.GA_TIME_R
  }

  def makeRhythmTree(map) {
    if (map != null) {
      def node = new RhythmTree(note: map.note, freq: map.freq)
      map.next.each {
	node.next.add(makeRhythmTree(it))
      }
      node
    } else {
      null
    }
  }

  def decideRhythm(curve) {
    ga.mostLikelyStateSequence(curve, null)
  }

  //  def decideRhythm(curve, double density) {
  //    ga.mostLikelyStateSequence(curve, null, new RhythmGACalc(density))
  //  }

  /*
  class RhythmGACalc3 extends RhythmGACalc {

    Random random;

    RhythmGACalc3() {
      //      super(density)
      random = new Random()
    }

    @CompileStatic
    List<Integer> createInitial(int size) {
      List<Integer> seq = []
      for (int i = 0 ; i < BEATS; i++) {
        Set<List<Integer>> keys = rhythmunigram.keySet()
        seq.addAll(keys[random.nextInt(keys.size())])
      }
      seq
    }

    @CompileStatic
    double evaluateRhythm(RhythmTree rhythmtree, List<Integer> seq, int index) {
      int div = (int)(DIVISION / BEATS)
      double l = 0.0
      for (int i = 0; i < BEATS; i++) {
        List<Integer> seq1 = seq[(i*div)..((i+1)*div-1)]
        l += Math.log((rhythmunigram[seq1] ? 
                      rhythmunigram[seq1] : 0.001) as double)
      }
      l
    }
  }
  */  

  class RhythmGACalc extends GACalculator<Integer,Double> {

    //    double rhythmDensity

//    RhythmGACalc() {
//      this(RhythmModel.RHYTHM_DENSITY)
//    }
    
//    RhythmGACalc(double density) {
//      rhythmDensity = density;
//    }
    
    @CompileStatic
    StoppingCondition getStoppingCondition() {
      new FixedElapsedTime(GA_TIME, TimeUnit.MILLISECONDS);
    }

    @CompileStatic
    void populationUpdated(Population p, int gen, List<MusicElement> e) {
      Chromosome c = p.getFittestChromosome()
    }

    @CompileStatic
    List<Integer> createInitial(int size) {
      List<Integer> seq = [0] * DIVISION
      generateRhythm(rhythmtree, seq, 0)
      seq
    }

    @CompileStatic
    double calcFitness(List<Integer> s, List<Double> o, List<MusicElement> e) {
      double rmse = calcRMSE(s, o);
      double likelihood = evaluateRhythm(rhythmtree, s, 0)
      double density = calcRhythmDensity(s)
      //      double swing = calcSwingRatio(s)
      //      double ent = calcDurationEntropy(s)
      //      println("RMSE=${rmse}\tL=${likelihood}")
      1.0 * rmse + 1.0 * likelihood + 20.0 * density
    }

    @CompileStatic
    List<Integer> makeTentativeRhythm(List<Double> curve) {
      List<Integer> r1 = [1]
      double th = R_TH
      (0..<(curve.size()-1)).each { i ->
	if (curve[i+1] == null) {
	  r1.add(0)
	} else if (curve[i] == null && curve[i+1] != null) {
	  r1.add(1)
	} else {
	  double d = Math.abs(curve[i+1] - curve[i])
	  double p = Math.tanh(2.0 * d as double)
	  //	  System.out.println(p)
	  r1.add(Math.random() < p ? 1 : 0)
	  //	} else if (Math.abs(curve[i+1] - curve[i]) >= th) {
	  //	  r1.add(1)
	  //	  	} else if (Math.abs(curve[i+1] - curve[i]) >= th / 2) {
	  //	  	  r1.add(Math.random() >= 0.5 ? 1 : 0)
	  //	} else {
	  //	  r1.add(0)
	}
      }
      //      println(r1)
      r1
    }

    @CompileStatic
    double calcRMSE(List<Integer> s, List<Double> o) {
      List<Integer> r1 = makeTentativeRhythm(o)
      double e = 0.0
      for (int i = 0; i < s.size(); i++) {
	e += (r1[i] - s[i]) * (r1[i] - s[i])
      }
      -Math.sqrt(e)
    }

//    @CompileStatic
    double calcRhythmDensity(List<Integer> s) {
      double e = (engine.parameters().RHYTHM_DENSITY - (int)s.sum())
      //        double e = (rhythmDensity - (int)s.sum())
      -e * e
    }

    @CompileStatic
    double calcSwingRatio(List<Integer> s) {
      int div = (int)(DIVISION / BEATS)
      int trip = 0
      int non_trip = 0
      for (int n in 0..<BEATS) {
         List<Integer> subseq = s[(n*div)..((n+1)*div-1)]
         if (subseq[(int)(div/3)] == 1 || subseq[(int)(div*2/3)] == 1) {
           trip++
         }
         if (subseq[(int)(div/2)] == 1) {
           non_trip++
         }
      }
      println("${trip}\t${non_trip}")
      (double)trip / (double)(non_trip)
    }

    @CompileStatic 
    double calcDurationEntropy(List<Integer> s) {
      0
    }    
    
    @CompileStatic
    void generateRhythm(RhythmTree tree, List<Integer> seq, int index) {
      if (tree.next[0] != null && tree.next[1] != null) {
	double freq0 = tree.next[0].freq
	double freq1 = tree.next[1].freq
	if (Math.random() < freq0 / (freq0 + freq1)) {
	  seq[index] = 0
	  generateRhythm(tree.next[0], seq, index+1)
	} else {
	  seq[index] = 1
	  generateRhythm(tree.next[1], seq, index+1)
	}
      } else if (tree.next[0] != null && tree.next[1] == null) {
	seq[index] = 0
	generateRhythm(tree.next[0], seq, index+1)
      } else if (tree.next[0] == null && tree.next[1] != null) {
	seq[index] = 1
	generateRhythm(tree.next[1], seq, index+1)
      }
    }

    @CompileStatic
    double evaluateRhythm(RhythmTree tree, List<Integer> seq, int index) {
      if (index < seq.size()) {
	if (tree.next[0] == null && tree.next[1] == null) {
	  return Double.NEGATIVE_INFINITY;
	} else if (tree.next[0] == null && tree.next[1] != null) {
	  if (seq[index] == 0) {
	    return Double.NEGATIVE_INFINITY;
	    //return Math.log(0.001)
	  } else {
	    return evaluateRhythm(tree.next[1], seq, index+1)
	  }
	} else if (tree.next[0] != null && tree.next[1] == null) {
	  if (seq[index] == 0) {
	    return evaluateRhythm(tree.next[0], seq, index+1)
	  } else {
	    return Double.NEGATIVE_INFINITY;
	    //return Math.log(0.001)
	  }
	} else {
	  double freq0 = tree.next[0].freq
	  double freq1 = tree.next[1].freq
	  if (seq[index] == 0) {
	    double logL = Math.log(freq0 / (freq0 + freq1))
	    return logL + evaluateRhythm(tree.next[0], seq, index+1)
	  } else {
	    double logL = Math.log(freq1 / (freq0 + freq1))
	    return logL + evaluateRhythm(tree.next[1], seq, index+1)
	  }
	}
      } else {
	0.0
      }
    }
  }
  
}

