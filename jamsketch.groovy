import controlP5.ControlP5
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import processing.core.PImage

// added by yonamine 20230208
import java.io.File
import java.io.FileReader
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


import jp.crestmuse.cmx.filewrappers.SCCDataSet
import jp.crestmuse.cmx.processing.gui.SimplePianoRoll

// tomb added below
import java.awt.*;


class Particle {
  int x, y;
  Color color;
  int size;
  int elongation;
  int age;  // New variable to represent particle age

  public Particle(int x, int y, Color color, int size, int elongation) {
    this.x = x;
    this.y = y;
    this.color = color;
    this.size = size;
    this.elongation = elongation;
    this.age = 0;  // Initialize age to 0
  }
}


private void updateParticles() {
  // Get mouse pointer coordinates
  Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
  SwingUtilities.convertPointFromScreen(mouseLocation, this);


  // Add new particles around the mouse pointer
  if (particles.size() < 100) {
    int x = mouseLocation.x;
    int y = mouseLocation.y;

    // Use a color gradient (yellow to red)
    int red = 255;
    int green = random.nextInt(100) + 100; // Vary green to simulate color variation
    int blue = random.nextInt(50);

    // Make particles semi-transparent and vary transparency over time
    int alpha = 255;

//            Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    // Make particles semi-transparent
    Color color = new Color(red, green, blue, alpha);
    // Vary the particle size
    int size = 5 + random.nextInt(10);
    int elongation = 10 + random.nextInt(10);

    particles.add(new Particle(x, y, color, size, elongation));
  }

  // Update particle properties and remove old particles
  Iterator<Particle> iterator = particles.iterator();
  while (iterator.hasNext()) {
    Particle particle = iterator.next();

    // Update particle positions
    int dx = (int) (3 * Math.sin(random.nextDouble() * Math.PI * 2));
    int dy = -2 - random.nextInt(2);

    particle.x += dx;
    particle.y += dy;

    // Update particle age
    particle.age++;

    // Gradually reduce particle transparency as it ages
    int alpha = (int) (255 - (255 * particle.age / particle.elongation));
    particle.color = new Color(particle.color.getRed(), particle.color.getGreen(),
            particle.color.getBlue(), alpha);

    // Remove particles that are too old (reached their lifespan)
    if (particle.age > particle.elongation) {
      iterator.remove();
    }
  }
}


void drawParticles(float x, float y) {
  // Draw particles only if drawParticles is true
  if (drawParticles) {
    for (Particle particle : particles) {
      fill(particle.color.getRGB());
      noStroke();

      // Draw ellipses to create a flame-like effect
      ellipse(particle.x, particle.y, particle.size, particle.size);
    }
  }
}


class JamSketch extends SimplePianoRoll {

  GuideData guideData
  MelodyData2 melodyData
  boolean nowDrawing = false
  String username = ""
  int fullMeasure
  int mCurrentMeasure
  double DebugModeDraw

  static def CFG
  PImage backgroundImage;
  boolean drawParticles = false;

  void setup() {
    super.setup()
    size(1200, 700)

    // Load the background image
    backgroundImage = loadImage("C:/Users/asano/JamSketchJunior/images/texasBar.jpeg");

    showMidiOutChooser()
    def p5ctrl = new ControlP5(this)
    p5ctrl.addButton("startMusic").
    setLabel("Start / Stop").setPosition(20, 645).
      setSize(120, 40)
    p5ctrl.addButton("resetMusic").
    setLabel("Reset").setPosition(160, 645).setSize(120, 40)
    

      p5ctrl.addButton("loadCurve").
      setLabel("Load").setPosition(300, 645).setSize(120, 40)


    if (CFG.MOTION_CONTROLLER != null) {
      CFG.MOTION_CONTROLLER.each { mCtrl ->
        JamSketch.main("JamSketchSlave", [mCtrl] as String[])
      }
    }

    initData()
    // add WindowListener (windowClosing) which calls exit();
  }

  void initData() {
    melodyData = new MelodyData2(CFG.MIDFILENAME, (width - CFG.getKeyboardWidth) as int, this, this, CFG)
    smfread(melodyData.scc.getMIDISequence())
    def part =
      melodyData.scc.getFirstPartWithChannel(CFG.CHANNEL_ACC)
    setDataModel(
      part.getPianoRollDataModel(
	    CFG.INITIAL_BLANK_MEASURES,
            CFG.INITIAL_BLANK_MEASURES + CFG.NUM_OF_MEASURES
      ))
    if (CFG.SHOW_GUIDE)
      guideData = new GuideData(CFG.MIDFILENAME, (width - CFG.getKeyboardWidth) as int, this)
    fullMeasure = dataModel.getMeasureNum() * CFG.REPEAT_TIMES;
  }


  void draw() {
    super.draw()

    // Tomb added BG and transparency
    tint(255, 100);
    image(backgroundImage, 0, 0, width, height);
    noTint();


    if (guideData != null)
      drawGuideCurve()
    if (CFG.FORCED_PROGRESS) {
      mouseX = beat2x(getCurrentMeasure() + CFG.HOW_IN_ADVANCE, getCurrentBeat());
    }

    if(pmouseX < mouseX &&
            mouseX > beat2x(getCurrentMeasure(), getCurrentBeat()) + 10) {
      if (isUpdatable()) {
        storeCursorPosition()
        updateCurve()
      }
    }
    drawCurve()
    if (getCurrentMeasure() == CFG.NUM_OF_MEASURES - CFG.NUM_OF_RESET_AHEAD)
      processLastMeasure()
    melodyData.engine.setFirstMeasure(getDataModel().
      getFirstMeasure())
    enhanceCursor()
    drawProgress()
  }

  void drawCurve() {
    strokeWeight(3)
    stroke(0, 0, 255)

    for (int i = 0; i < melodyData.curve1.size() - 1; i++) {
      if (melodyData.curve1[i] != null && melodyData.curve1[i + 1] != null) {
        // Draw a line between two points
        line(i + CFG.getKeyboardWidth, melodyData.curve1[i] as int, i + CFG.getKeyboardWidth + 1,
                melodyData.curve1[i + 1] as int);

        // Draw particles at each point
        drawParticles(i + CFG.getKeyboardWidth, melodyData.curve1[i] as int);
      }
    }
  }

  void drawParticles(float x, float y) {
    // Draw particles only if drawParticles is true
    if (drawParticles) {
      // Vary the particle size
      float particleSize = random(5, 15);

      // Create a gradient from yellow to red
      float red = 255;
      float green = random(100, 255);
      float blue = 0;

      // Vary the transparency
      for (int i = 255; i >= 0; i -= 5) {
        fill(red, green, blue, i); // Yellow to red with varying transparency
        noStroke();

        // Draw ellipses to create a flame-like effect
        ellipse(x, y, particleSize, particleSize);

        // Offset the position slightly to create a dispersed effect
        x += random(-2, 2);
        y += random(-2, 2);
      }
    }
  }




  void drawGuideCurve() {
    def xFrom = 100
    strokeWeight(3)
    stroke(100, 200, 200)
    (0..<(guideData.curveGuideView.size()-1)).each { i ->
      if (guideData.curveGuideView[i] != null &&
      guideData.curveGuideView[i+1] != null) {
        line(i+xFrom, guideData.curveGuideView[i] as int,
             i+1+xFrom, guideData.curveGuideView[i+1] as int)
      }
    }
  }

  void updateCurve() {
    melodyData.updateCurve(pmouseX, mouseX)
  }

  void storeCursorPosition() {
    (pmouseX..mouseX).each { i ->

      melodyData.curve1[i-CFG.getKeyboardWidth] = mouseY
      println("Y axis ${mouseY}")
      println("X axis ${mouseX}")
    }
  }

  boolean isUpdatable() {
    if ((!CFG.ON_DRAG_ONLY || nowDrawing) &&
            isInside(mouseX, mouseY)) {
      int m1 = x2measure(mouseX)
      int m0 = x2measure(pmouseX)
      0 <= m0 && pmouseX < mouseX
    } else {
      false
    }
  }

  void processLastMeasure() {
    makeLog("melody")
    if (CFG.MELODY_RESETTING) {
      if (mCurrentMeasure < (fullMeasure - CFG.NUM_OF_RESET_AHEAD)) getDataModel().shiftMeasure(CFG.NUM_OF_MEASURES)
      melodyData.resetCurve()
      if (guideData != null) guideData.shiftCurve()
    }
  }

  void enhanceCursor() {
    if (CFG.CURSOR_ENHANCED) {
      fill(255, 0, 0)
      ellipse(mouseX, mouseY, 10, 10)
    }
  }

  void drawProgress() {
    if (isNowPlaying()) {
      def dataModel = getDataModel()
      mCurrentMeasure = getCurrentMeasure() +
              dataModel.getFirstMeasure() -
              CFG.INITIAL_BLANK_MEASURES + 1
      int mtotal = dataModel.getMeasureNum() *
                   CFG.REPEAT_TIMES
      textSize(32)
      fill(0, 0, 0)
      text(mCurrentMeasure + " / " + mtotal, 460, 675)
    }
  }
  
  void stop() {
    super.stop()
    //featext.stop()
  }

  void startMusic() {
    if (isNowPlaying()) {
      stopMusic()
      makeLog("stop")
    } else {
      playMusic()
      makeLog("play")
    }
  }

  void resetMusic() {
    initData()
    setTickPosition(0)
    dataModel.setFirstMeasure(CFG.INITIAL_BLANK_MEASURES)
    makeLog("reset")
  }

  @Override
  void musicStopped() {
    super.musicStopped()
//    if (microsecondPosition >= sequencer.getMicrosecondLength())
//      resetMusic()
  }



  void makeLog(action) {
    def logname = "output_" + (new Date()).toString().replace(" ", "_").replace(":", "-")
    if (action == "melody") {
      def midname = "${CFG.LOG_DIR}/${logname}_melody.mid"
      melodyData.scc.toWrapper().toMIDIXML().writefileAsSMF(midname)
      println("saved as ${midname}")
      def sccname = "${CFG.LOG_DIR}/${logname}_melody.sccxml"
      melodyData.scc.toWrapper().writefile(sccname)
      println("saved as ${sccname}")
      def jsonname = "${CFG.LOG_DIR}/${logname}_curve.json"
      saveStrings(jsonname, [JsonOutput.toJson(melodyData.curve1)] as String[])
      println("saved as ${jsonname}")
      def pngname = "${CFG.LOG_DIR}/${logname}_screenshot.png"
      save(pngname)
      println("saved as ${pngname}")
      // for debug
      new File("${CFG.LOG_DIR}/${logname}_noteList.txt").text = (melodyData.scc as SCCDataSet).getFirstPartWithChannel(1).getNoteList().toString()
//      new File("${CFG.LOG_DIR}/${logname}_noteOnlyList.txt").text = (melodyData.scc as SCCDataSet).getFirstPartWithChannel(1).getNoteOnlyList().toString()

    } else {
      def txtname = "${CFG.LOG_DIR}/${logname}_${action}.txt"
      saveStrings(txtname, [action] as String[])
      println("saved as ${txtname}")
    }
  }

  void loadCurve() {
    selectInput("Select a file to process:", "loadFileSelected")
  }

  void loadFileSelected(File selection) {
    if (selection == null) {
      println("Window was closed or the user hit cancel.")
    } else {
      def absolutePath = selection.getAbsolutePath()
      println("User selected " + absolutePath)
      if (absolutePath.endsWith(".json")) {
        def json = new JsonSlurper()

        melodyData.curve1 = json.parseText(selection.text)
        int count = melodyData.curve1.count(null)

        melodyData.updateCurve(0, width - (DebugModeDraw as int))
      } else if (selection.getCanonicalPath().endsWith(".txt")) {
        println("Reading ${absolutePath}")
        def table = loadTable(absolutePath, "csv")
        melodyData.curve1 = [null] * (width - (DebugModeDraw as int))
        int n = table.getRowCount()
        int m = melodyData.curve1.size()
        for (int i in (getKeyboardWidth() as int)..<(melodyData.curve1.size() - 1)) {
          int from = (i - (getKeyboardWidth() as int)) * n / m
          int thru = ((i + 1) - (getKeyboardWidth() as int)) * n / m - 1
          melodyData.curve1[i] =
                  (from..thru).collect { notenum2y(table.getFloat(it, 0)) }.sum() /
                          (from..thru).size()
        }
        melodyData.updateCurve(0,  width - (DebugModeDraw as int))
      }else {
        println("File is not supported")
        return
      }
    }
  }

  void mousePressed() {
    nowDrawing = true
    drawParticles = true;
  }

  void mouseReleased() {
    nowDrawing = false;
    // Set drawParticles to false when the mouse is released
    drawParticles = false;
    if (isInside(mouseX, mouseY)) {
      if (!melodyData.engine.automaticUpdate()) {
        melodyData.engine.outlineUpdated(
                x2measure(mouseX) % CFG.NUM_OF_MEASURES,
                CFG.DIVISION - 1
        );
      }
    }
  }

  void mouseDragged() {

  }

  void keyReleased() {
    if (key == ' ') {
      if (isNowPlaying()) {
      	stopMusic()
      } else {
        setTickPosition(0)
        getDataModel().setFirstMeasure(CFG.INITIAL_BLANK_MEASURES)
        playMusic()
      }
    } else if (key == 'b') {
      setNoteVisible(!isNoteVisible());
      println("Visible=${isVisible()}")
   } else if (key == 'u') {
     melodyData.updateCurve('all')
    }

}
  void LoadOutlineLayerData() {

    def jsonFile = new File(CFG.INPUT_FILE_PATH)
    def jsonSlurper = new JsonSlurper()
    def jsonData  = jsonSlurper.parse(jsonFile)
    jsonData.eachWithIndex{v, i-> 

      melodyData.curve1[i] = v

    }

    melodyData.updateCurve(0, (width - getKeyboardWidth()) as int)
    // processLastMeasure()
  }

  public void exit() {
    println("exit() called.")
    super.exit()
    if (CFG.MOTION_CONTROLLER.any{mCtrl == "RfcommServer"}) RfcommServer.close()
  }

//  static void main(String[] args) {
//     JamSketch.CFG = evaluate(new File("./config.txt"))
////    JamSketch.CFG = evaluate(new File("./config_guided.txt"))
//    JamSketch.start("JamSketch")
//  }

}
//JamSketch.CFG = evaluate(new File("./config.txt"))
JamSketch.CFG = evaluate(new File("./config_guided.txt"))
JamSketch.start("JamSketch")
// JamSketch.main("JamSketch", ["--external"] as String[])
  
