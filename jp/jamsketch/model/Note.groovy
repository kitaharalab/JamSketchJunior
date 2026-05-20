package jp.jamsketch.model

class Note {

    private MusicPosition position;
    private int pitch, duration;


    public Note(int measure, int tick, int pitch, int duration){
        this(new MusicPosition(measure, tick), pitch, duration)
    }

    public Note(MusicPosition position, int pitch, int duration){
        this.position = position
        this.duration = duration
        this.pitch = pitch
    }

    @Override
    boolean equals(Object obj) {
        if(obj instanceof Note){
            Note note = (Note) obj;

            return note.position.equals(position) && note.pitch == pitch && note.duration == duration
        }
        return false
    }
}
