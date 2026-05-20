package jp.jamsketch.main

import java.awt.*;
import javax.swing.*;

/**
 * JamSketchのイベントリスナー
 */
public class JamSketchEventListnerImpl implements JamSketchEventListner {
    
    // ダイアログ
    private JPanel panel;
    
    /**
     * コンストラクタ
     * @param panel ダイアログ
     */
    public JamSketchEventListnerImpl(JPanel panel){
        this.panel = panel;
    }

    /**
     * 切断された状態を通知する
     */
    @Override
    public void disconnected(){
        JOptionPane.showConfirmDialog( 
            null,   
            panel,    
            "Error", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.ERROR_MESSAGE
        );
    }
}