package jp.jamsketch.model

class MusicPosition {
    private final int measure;
    private final int tick;

    public MusicPosition(int measure, int tick){
        this.measure = measure;
        this.tick = tick;
    }

    public MusicPosition(double mouseX, double mouseY){

    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof MusicPosition){
            MusicPosition mp = (MusicPosition) obj;
            return mp.tick == tick && mp.measure == measure;
        }else{
            return false
        }
    }

}
