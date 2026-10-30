package dev.local.ysmenhancements.ui;

import java.util.List;

/** Keeps the current form in place while YSM asynchronously rebuilds the same form. */
final class ControlView {
    private List<Object> displayed=List.of();
    private boolean waiting;
    private long started;
    void begin(long now){waiting=!displayed.isEmpty();started=now;}
    List<Object> update(List<Object> current,long now){
        // A radio action rebuilds all the same widgets. Do not expose its temporary empty list.
        // Fall back to partial content if an unrelated form callback fails to complete.
        if(waiting&&current.size()<displayed.size()&&now-started<2_000_000_000L)return displayed;
        waiting=false;displayed=List.copyOf(current);return displayed;
    }
    boolean waiting(){return waiting;}
    long started(){return started;}
    void clear(){displayed=List.of();waiting=false;}
}
