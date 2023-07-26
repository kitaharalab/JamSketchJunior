class JamSketchSlave extends JamSketch implements TargetMover {

  def motionController
  int startPosition = 0

  void setup() {
    background(255)
    smooth()
    size(1200, 700)
    showMidiOutChooser()

    if (args[0] != "Cursor") {
      motionController = Class.forName(args[0]).newInstance()
      motionController.setTargetMover(this)
      motionController.init()
      motionController.start()
    }

    initData()

  }

  void processLastMeasure() {
    startPosition = 0
    super.processLastMeasure()
  }

  void drawProgress() {
    super.drawProgress()
    if (nowDrawing) {
      textSize(32)
      fill(255, 165, 0)
      text("Drawing", 100, 675)    
    }
  }

  void mousePressed() {
    if (motionController == null) {
      nowDrawing = true
    }
  }
  
  void mouseReleased() {
    if (motionController == null) {
      nowDrawing = false
    }
  }

  void mouseDragged() {
    if (motionController == null) {
      if (isUpdatable()) {
        storeCursorPosition()
        updateCurve()
      }
    }
  }

  void setTargetXY(double x, double y) {
      int measure = getCurrentMeasure()
      double beat = getCurrentBeat()
      if (measure >= 0) {
        def currentX = beat2x(measure + 1, beat) as int
        if (nowDrawing) {
          melodyData.curve1[currentX] = y
          println("melodyData.curve1[${currentX}]=${y}")
          fillCurve1(currentX)
          melodyData.updateCurve(currentX % CFG.NUM_OF_MEASURES)
        }
    }
  }

  int height() {
    height
  }

  int width() {
    width
  }

  void sendEvent(int event) {
    if (event == TargetMover.ONSET) {
      startPosition = (beat2x(getCurrentMeasure() + 1, getCurrentBeat())) as int
      nowDrawing = true
    } else if (event == TargetMover.OFFSET) {
      nowDrawing = false
    }
  }

  private void fillCurve1(currentValueIndex) {
    if (currentValueIndex > startPosition) {
      def pIndex = getPreviousValueIndex(currentValueIndex)
      if (pIndex >= 0 && (currentValueIndex - pIndex) > 1) {
        def diff = (melodyData.curve1[currentValueIndex] - melodyData.curve1[pIndex]) / (currentValueIndex - pIndex)
        for (i in currentValueIndex - 1 .. pIndex + 1) {
          melodyData.curve1[i] = melodyData.curve1[i + 1] - diff
        }
      }
    }e
}

private int getPrviousValueIndex(currentValueIndex) {
    for (i in currentValueIndex - 1 .. startPosition) {
      if (melodyData.curve1[i] != null) return i
      if (i == startPosition) return -1
    }
  }

}