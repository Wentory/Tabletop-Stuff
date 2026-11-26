package com.wentory.tabletopstuff.prototype;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record PreparationUpdate(UUID owner,BlockPos table,boolean active,int phase,int remaining,int actionSerial,int action,UUID target,int packetSize,int insertionDepth,List<Actor> actors,List<Played> discarded,Rules rules,Rps rps,Vote vote,Reveal reveal){
    public PreparationUpdate(UUID owner,BlockPos table,boolean active,int phase,int remaining,int actionSerial,int action,UUID target,int packetSize,int insertionDepth,List<Actor> actors,List<Played> discarded,Rules rules,Rps rps,Vote vote){
        this(owner,table,active,phase,remaining,actionSerial,action,target,packetSize,insertionDepth,actors,discarded,rules,rps,vote,NO_REVEAL);
    }
    public static final int REVEAL_TURN_TICKS=10, REVEAL_TOTAL_TICKS=130;
    public static int revealDurationTicks(int sourceSeat,int viewerSeat){
        return Math.floorMod(viewerSeat-sourceSeat,4)==2?REVEAL_TOTAL_TICKS:2*REVEAL_TURN_TICKS+100;
    }
    public int revealDurationTicks(){
        if(reveal.remainingTicks()==0)return 0;
        int sourceSeat=actors.stream().filter(actor->actor.id().equals(reveal.source())).findFirst().orElseThrow().seat();
        int viewerSeat=actors.stream().filter(actor->actor.id().equals(reveal.viewer())).findFirst().orElseThrow().seat();
        return revealDurationTicks(sourceSeat,viewerSeat);
    }
    public record Reveal(UUID source,UUID viewer,int remainingTicks){}
    public static final Reveal NO_REVEAL=new Reveal(new UUID(0,0),new UUID(0,0),0);
    public PreparationUpdate(UUID owner,BlockPos table,boolean active,int phase,int remaining,int actionSerial,int action,UUID target,int packetSize,int insertionDepth,List<Actor> actors,List<Played> discarded,Rules rules,Rps rps){this(owner,table,active,phase,remaining,actionSerial,action,target,packetSize,insertionDepth,actors,discarded,rules,rps,NO_VOTE);}
    public record Vote(boolean active,int yes,int no,int total,int seconds,int ownChoice){}
    public static final Vote NO_VOTE=new Vote(false,0,0,0,0,-1);
    public static final int START_CANCEL_VOTE=117,CANCEL_VOTE=118;
    public static final int FADING=0,PICKUP=1,HELD=2,PLACING=3,READY=4;
    public static final int SHUFFLE=100,DEAL=101,TAKE=102,PLACE=103,DRAW=105,PLAY=107,COLOR=108,PASS=109,CHALLENGE=110,LAST=111,NEXT_ROUND=112,CLOSE_REVEAL=113;
    public record Actor(UUID id,int seat,Vec3 feet,float yaw,boolean seated,int delivered,int[] pile,int[] faces,int takeSerial,int taken,boolean fromDeck,boolean hidden,int debt,int[] playable,boolean canDraw,boolean canPass,int score,int awayState,String name,int awaySeconds){
        public Actor(UUID id,int seat,Vec3 feet,float yaw,boolean seated,int delivered,int[] pile,int[] faces,int takeSerial,int taken,boolean fromDeck,boolean hidden,int debt,int[] playable,boolean canDraw,boolean canPass,int score,int awayState){
            this(id,seat,feet,yaw,seated,delivered,pile,faces,takeSerial,taken,fromDeck,hidden,debt,playable,canDraw,canPass,score,awayState,"",0);
        }
    }
    public record Played(int id,UUID player,int index,int face,float yaw,float x,float z){}
    public static final UUID NONE=new UUID(0,0);
    public record Rules(boolean started,UUID turn,int direction,int color,UUID chooser,UUID challenger,UUID vulnerable,UUID winner,int round,UUID lastCaller,int[] revealed,boolean challengeWon,boolean reveal,boolean matchOver,int lastSerial,boolean lastFalse,UUID host,boolean paused,UUID dealer,boolean lastStanding,UUID loser){
        public Rules(boolean started,UUID turn,int direction,int color,UUID chooser,UUID challenger,UUID vulnerable,UUID winner,int round,UUID lastCaller,int[] revealed,boolean challengeWon,boolean reveal,boolean matchOver,int lastSerial,boolean lastFalse,UUID host,boolean paused,UUID dealer){
            this(started,turn,direction,color,chooser,challenger,vulnerable,winner,round,lastCaller,revealed,challengeWon,reveal,matchOver,lastSerial,lastFalse,host,paused,dealer,false,NONE);
        }
    }
    public static final int STEP_AWAY=114,AWAY_BLACK_READY=115;
    public static final int RPS_CHOICE=116,RPS_PHASE=5;
    public record Rps(boolean active,List<UUID> players,int[] choices,int ownChoice,int submitted,boolean revealed,int round,int remaining,UUID winner){}
    public static final Rps NO_RPS=new Rps(false,List.of(),new int[0],-1,0,false,1,0,NONE);
    public void write(FriendlyByteBuf b){
        b.writeUUID(owner);b.writeBlockPos(table);b.writeBoolean(active);b.writeByte(phase);b.writeVarInt(remaining);
        b.writeVarInt(actionSerial);b.writeVarInt(action);b.writeUUID(target);b.writeVarInt(packetSize);b.writeVarInt(insertionDepth);b.writeVarInt(actors.size());
        for(var a:actors){b.writeUUID(a.id);b.writeByte(a.seat);b.writeDouble(a.feet.x);b.writeDouble(a.feet.y);b.writeDouble(a.feet.z);b.writeFloat(a.yaw);b.writeBoolean(a.seated);
            b.writeVarInt(a.delivered);b.writeVarIntArray(a.pile);b.writeVarIntArray(a.faces);b.writeVarInt(a.takeSerial);b.writeVarInt(a.taken);b.writeBoolean(a.fromDeck);b.writeBoolean(a.hidden);b.writeVarInt(a.debt);b.writeVarIntArray(a.playable);b.writeBoolean(a.canDraw);b.writeBoolean(a.canPass);b.writeVarInt(a.score);b.writeByte(a.awayState);b.writeUtf(a.name,256);b.writeVarInt(a.awaySeconds);}
        b.writeVarInt(discarded.size());for(var d:discarded){b.writeVarInt(d.id);b.writeUUID(d.player);b.writeVarInt(d.index);b.writeVarInt(d.face);b.writeFloat(d.yaw);b.writeFloat(d.x);b.writeFloat(d.z);}
        b.writeBoolean(rules.started);b.writeUUID(rules.turn);b.writeByte(rules.direction);b.writeByte(rules.color);b.writeUUID(rules.chooser);b.writeUUID(rules.challenger);b.writeUUID(rules.vulnerable);b.writeUUID(rules.winner);b.writeVarInt(rules.round);b.writeUUID(rules.lastCaller);b.writeVarIntArray(rules.revealed);b.writeBoolean(rules.challengeWon);b.writeBoolean(rules.reveal);b.writeBoolean(rules.matchOver);b.writeVarInt(rules.lastSerial);
        b.writeBoolean(rules.lastFalse);
        b.writeUUID(rules.host);b.writeBoolean(rules.paused);
        b.writeUUID(rules.dealer);
        b.writeBoolean(rules.lastStanding);b.writeUUID(rules.loser);
        b.writeBoolean(rps.active);b.writeVarInt(rps.players.size());rps.players.forEach(b::writeUUID);b.writeVarIntArray(rps.choices);
        b.writeByte(rps.ownChoice);b.writeByte(rps.submitted);b.writeBoolean(rps.revealed);b.writeVarInt(rps.round);b.writeByte(rps.remaining);b.writeUUID(rps.winner);
        b.writeBoolean(vote.active);b.writeVarInt(vote.yes);b.writeVarInt(vote.no);b.writeVarInt(vote.total);b.writeVarInt(vote.seconds);b.writeByte(vote.ownChoice);
        b.writeUUID(reveal.source);b.writeUUID(reveal.viewer);b.writeVarInt(reveal.remainingTicks);
    }
    public static PreparationUpdate read(FriendlyByteBuf b){
        UUID owner=b.readUUID();BlockPos table=b.readBlockPos();boolean active=b.readBoolean();int phase=b.readUnsignedByte(),remaining=b.readVarInt();
        int serial=b.readVarInt(),action=b.readVarInt();UUID target=b.readUUID();int packet=b.readVarInt(),depth=b.readVarInt();int size=b.readVarInt();
        if(phase>5||remaining<0||remaining>108||size<1||size>8||packet<0||packet>108||depth<0||depth>108)throw new IllegalArgumentException("Invalid preparation state");
        List<Actor> actors=new ArrayList<>(size);
        for(int i=0;i<size;i++){
            UUID id=b.readUUID();int seat=b.readUnsignedByte();Vec3 feet=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());float yaw=b.readFloat();boolean seated=b.readBoolean();
            int delivered=b.readVarInt();int[] pile=b.readVarIntArray(7),faces=b.readVarIntArray(108);int takeSerial=b.readVarInt(),taken=b.readVarInt();boolean fromDeck=b.readBoolean(),hidden=b.readBoolean();int debt=b.readVarInt();int[] playable=b.readVarIntArray(108);boolean canDraw=b.readBoolean(),canPass=b.readBoolean();int score=b.readVarInt();
            int awayState=b.readUnsignedByte();
            String name=b.readUtf(256);int awaySeconds=b.readVarInt();if(awaySeconds< -1)throw new IllegalArgumentException("Invalid absence timer");
            if(awayState>AwayState.RETIRED||seat>7||delivered<0||delivered>7||faces.length+pile.length>108||taken<0||taken>108||!Double.isFinite(feet.x+feet.y+feet.z)||!Float.isFinite(yaw))throw new IllegalArgumentException("Invalid participant");
            if(debt<0||debt>10000||score<0||score>1000000)throw new IllegalArgumentException("Invalid rule counters");
            for(int index:playable)if(index<0||index>=faces.length)throw new IllegalArgumentException("Invalid playable index");
            for(int index:pile)if(index<0||index>6)throw new IllegalArgumentException("Invalid pile");
            for(int face:faces)if(face< -1||face>53)throw new IllegalArgumentException("Invalid card");
            actors.add(new Actor(id,seat,feet,yaw,seated,delivered,pile,faces,takeSerial,taken,fromDeck,hidden,debt,playable,canDraw,canPass,score,awayState,name,awaySeconds));
        }
        int count=b.readVarInt();if(count<0||count>108)throw new IllegalArgumentException("Invalid discard pile");
        List<Played> discarded=new ArrayList<>(count);
        for(int i=0;i<count;i++){
            int id=b.readVarInt();UUID player=b.readUUID();int index=b.readVarInt(),face=b.readVarInt();float yaw=b.readFloat(),x=b.readFloat(),z=b.readFloat();
            if(id!=i||index< -1||index>=108||face<0||face>53||!Float.isFinite(yaw)||!Float.isFinite(x)||!Float.isFinite(z)||Math.abs(x)>.07f||Math.abs(z)>.07f)throw new IllegalArgumentException("Invalid played card");
            discarded.add(new Played(id,player,index,face,yaw,x,z));
        }
        boolean started=b.readBoolean();UUID turn=b.readUUID();int direction=b.readByte(),color=b.readByte();UUID chooser=b.readUUID(),challenger=b.readUUID(),vulnerable=b.readUUID(),winner=b.readUUID();int round=b.readVarInt();UUID lastCaller=b.readUUID();int[] revealed=b.readVarIntArray(108);boolean challengeWon=b.readBoolean(),reveal=b.readBoolean(),matchOver=b.readBoolean();
        if(direction!=1&&direction!=-1||color< -1||color>3||round<1)throw new IllegalArgumentException("Invalid round state");for(int face:revealed)if(face<0||face>53)throw new IllegalArgumentException("Invalid reveal");
        int lastSerial=b.readVarInt();if(lastSerial<0)throw new IllegalArgumentException("Invalid LAST event");boolean lastFalse=b.readBoolean();
        UUID host=b.readUUID();boolean paused=b.readBoolean();
        UUID dealer=b.readUUID();
        Rules rules=new Rules(started,turn,direction,color,chooser,challenger,vulnerable,winner,round,lastCaller,revealed,challengeWon,reveal,matchOver,lastSerial,lastFalse,host,paused,dealer,b.readBoolean(),b.readUUID());
        boolean contest=b.readBoolean();int candidates=b.readVarInt();if(candidates<0||candidates>8)throw new IllegalArgumentException("Invalid RPS size");
        List<UUID> rpsPlayers=new ArrayList<>();for(int i=0;i<candidates;i++)rpsPlayers.add(b.readUUID());int[] choices=b.readVarIntArray(8);
        int own=b.readByte(),submitted=b.readUnsignedByte();boolean shown=b.readBoolean();int rpsRound=b.readVarInt(),survivors=b.readUnsignedByte();UUID rpsWinner=b.readUUID();
        if(choices.length!=candidates||new HashSet<>(rpsPlayers).size()!=candidates||own< -1||own>2||submitted>candidates||survivors>candidates||rpsRound<1)throw new IllegalArgumentException("Invalid RPS state");
        if(contest&&candidates==0||shown&&submitted!=candidates||!rpsWinner.equals(NONE)&&!rpsPlayers.contains(rpsWinner))throw new IllegalArgumentException("Invalid RPS result");
        for(int choice:choices)if(choice< -1||choice>2||!shown&&choice!=-1||shown&&choice<0)throw new IllegalArgumentException("Invalid RPS choice");
        Rps rps=new Rps(contest,List.copyOf(rpsPlayers),choices,own,submitted,shown,rpsRound,survivors,rpsWinner);
        boolean voting=b.readBoolean();int yes=b.readVarInt(),no=b.readVarInt(),total=b.readVarInt(),seconds=b.readVarInt(),ownVote=b.readByte();
        if(yes<0||no<0||total<0||total>9||yes+no>total||seconds<0||seconds>3600||ownVote< -1||ownVote>1||voting&&total==0)throw new IllegalArgumentException("Invalid cancellation vote");
        UUID source=b.readUUID(),viewer=b.readUUID();int revealTicks=b.readVarInt();
        if(revealTicks<0||revealTicks>REVEAL_TOTAL_TICKS||revealTicks>0&&(source.equals(viewer)||actors.stream().noneMatch(a->a.id.equals(source))||actors.stream().noneMatch(a->a.id.equals(viewer))))throw new IllegalArgumentException("Invalid hand reveal");
        return new PreparationUpdate(owner,table,active,phase,remaining,serial,action,target,packet,depth,List.copyOf(actors),List.copyOf(discarded),rules,rps,new Vote(voting,yes,no,total,seconds,ownVote),new Reveal(source,viewer,revealTicks));
    }
}
