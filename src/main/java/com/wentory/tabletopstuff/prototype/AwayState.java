package com.wentory.tabletopstuff.prototype;

/** Tick-based participation transitions; zero timeout disables expiry. */
public final class AwayState {
    public static final int ACTIVE=0,LEAVING=1,AWAY=2,RETURN_FADE=3,PICKUP=4,RETIRED=5;
    private int state,leavingTicks=30;
    private long changed,deadline;
    public int state(){return state;}
    public long changed(){return changed;}
    public boolean paused(){return state!=ACTIVE&&state!=RETIRED;}
    public int leavingTicks(){return leavingTicks;}
    public boolean leave(long tick){return leave(tick,true);}
    public boolean leave(long tick,boolean hasCards){if(state!=ACTIVE)return false;leavingTicks=hasCards?30:0;state=LEAVING;changed=tick;return true;}
    /** Death has no leaving animation and must not renew an existing absence deadline. */
    public boolean died(long tick,int timeoutMinutes){
        if(state==AWAY||state==RETIRED)return false;
        if(state==ACTIVE||state==LEAVING)deadline=timeoutMinutes==0?Long.MAX_VALUE:tick+timeoutMinutes*1200L;
        state=AWAY;changed=tick;return true;
    }
    public int remainingSeconds(long tick){
        if(state!=AWAY&&state!=RETURN_FADE)return 0;
        return deadline==Long.MAX_VALUE?-1:(int)Math.max(0,(deadline-tick+19)/20);
    }
    public boolean detach(long tick,int timeoutMinutes){
        if(state!=LEAVING||tick-changed<leavingTicks)return false;
        state=AWAY;changed=tick;deadline=timeoutMinutes==0?Long.MAX_VALUE:tick+timeoutMinutes*1200L;return true;
    }
    public boolean returning(long tick){if(state!=AWAY||expired(tick))return false;state=RETURN_FADE;changed=tick;return true;}
    public boolean pickup(long tick){if(state!=RETURN_FADE||expired(tick))return false;state=PICKUP;changed=tick;return true;}
    public boolean finishPickup(long tick){if(state!=PICKUP||tick-changed<30)return false;state=ACTIVE;changed=tick;return true;}
    public boolean expired(long tick){return (state==AWAY||state==RETURN_FADE)&&tick>=deadline;}
    public void retire(long tick){state=RETIRED;changed=tick;}
}
