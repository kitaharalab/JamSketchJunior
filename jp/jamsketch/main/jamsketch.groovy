package jp.jamsketch.main

import controlP5.ControlP5
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

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

import java.awt.*;
import javax.swing.*;


import jp.crestmuse.cmx.filewrappers.SCCDataSet
import jp.crestmuse.cmx.processing.gui.SimplePianoRoll

import jp.jamsketch.main.JamSketchEventListner;
import jp.jamsketch.main.JamSketchEventListnerImpl;

import jp.jamsketch.controller.IJamSketchController;
import jp.jamsketch.controller.JamSketchController;
import jp.jamsketch.controller.JamSketchClientController;
import jp.jamsketch.controller.JamSketchServerController;
import jp.jamsketch.web.ServiceLocator;

class JamSketch extends SimplePianoRoll {

  GuideData guideData
  MelodyData2 melodyData
  boolean nowDrawing = false
  String username = ""
  int fullMeasure
  int mCurrentMeasure
  double DebugModeDraw

  // JamSketch操作クラス
  IJamSketchController controller;

  // ダイアログ
  JPanel panel;

  static def CFG

  void setup() {
    super.setup()
    size(1200, 700)
    showMidiOutChooser()
    def p5ctrl = new ControlP5(this)

    if (CFG.mode == "client") {
      p5ctrl.addButton("reconnect").
      setLabel("Reconnect").setPosition(20, 645).
        setSize(120, 40)
    }
    else{
      p5ctrl.addButton("startMusic").
      setLabel("Start / Stop").setPosition(20, 645).
        setSize(120, 40)
      p5ctrl.addButton("loadCurve").
      setLabel("Load").setPosition(300, 645).setSize(120, 40)
    }
    p5ctrl.addButton("resetMusic").
    setLabel("Reset").setPosition(160, 645).setSize(120, 40)

    panel = new JPanel();
    BoxLayout layout = new BoxLayout( panel, BoxLayout.Y_AXIS );
    panel.setLayout(layout);    
    panel.add( new JLabel( "接続が切断されました。" ) ); 
    JamSketchEventListner listner = new JamSketchEventListnerImpl(panel);

    if (CFG.MOTION_CONTROLLER != null) {
      CFG.MOTION_CONTROLLER.each { mCtrl ->
        JamSketch.main("JamSketchSlave", [mCtrl] as String[])
      }
    }

    def melodyData = initData()

    // JamSketch操作クラスを初期化
    def origController = new JamSketchController(melodyData, this::initData);
    
    if (CFG.mode == "server"){
      // サーバーで動かす場合に使う操作クラスを設定
      controller = new JamSketchServerController(CFG.host, CFG.port, origController);
    }    
    else if (CFG.mode == "client"){
      // クライアントで動かす場合に使う操作クラスを設定
      controller = new JamSketchClientController(CFG.host, CFG.port, origController, listner);
    }
    else{
      // スタンドアロンで動かす場合はそのまま
      controller = origController;
    }
    def serviceLocator = ServiceLocator.GetInstance();
		serviceLocator.setContoller(controller);

    // add WindowListener (windowClosing) which calls exit();
  }

  MelodyData2 initData() {
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
    
    setTickPosition(0)
    dataModel.setFirstMeasure(CFG.INITIAL_BLANK_MEASURES)

    // 初期化済みオブジェクトを返す
    return melodyData;
  }


  void draw() {
    super.draw()    
    if (guideData != null)
      drawGuideCurve()
    if (CFG.FORCED_PROGRESS) {
      mouseX = beat2x(getCurrentMeasure() + CFG.HOW_IN_ADVANCE, getCurrentBeat());
    }

    if(pmouseX < mouseX &&
            mouseX > beat2x(getCurrentMeasure(), getCurrentBeat()) + 10) {
      if (isUpdatable()) {
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

    (0..<(melodyData.curve1.size()-1)).each { i ->
      if (melodyData.curve1[i] != null &&
          melodyData.curve1[i+1] != null) {
        line(i+CFG.getKeyboardWidth, melodyData.curve1[i] as int, i+CFG.getKeyboardWidth+1,
             melodyData.curve1[i+1] as int)
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
    // JamSketch操作クラスを使用して楽譜データを更新する
    if (pmouseX != -1 && mouseX != -1)
      this.controller.updateCurve(pmouseX, mouseX, mouseY)
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
      
	if (controller instanceof jp.jamsketch.controller.JamSketchServerController) {
       	   controller.resetClients();
	}

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
    // JamSketch操作クラスを使用してリセットする
    this.controller.reset();
    makeLog("reset")
  }

  @Override
  void musicStopped() {
    super.musicStopped()
//    if (microsecondPosition >= sequencer.getMicrosecondLength())
//      resetMusic()
  }

  /**
    * 再接続する
    */
  void reconnect() {
    this.controller.init();
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
  }
  
  void mouseReleased() {
    nowDrawing = false
    mouseX = -1
    if (isInside(mouseX, mouseY)) {
      if (!melodyData.engine.automaticUpdate()) {
        melodyData.engine.outlineUpdated(
	   x2measure(mouseX) % CFG.NUM_OF_MEASURES,
           CFG.DIVISION - 1)
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

  static void main(String[] args) {
    JamSketch.CFG = new GroovyShell().evaluate(new File("./config.txt"))
//    JamSketch.CFG = new GroovyShell().evaluate(new File("./config_guided.txt"))
    JamSketch.start("jp.jamsketch.main.JamSketch")
  }

}
//JamSketch.CFG = evaluate(new File("./config.txt"))
//JamSketch.CFG = evaluate(new File("./config_guided.txt"))
//JamSketch.start("jp.jamsketch.main.JamSketch")
// JamSketch.main("JamSketch", ["--external"] as String[])
  
