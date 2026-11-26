package com.wentory.tabletopstuff.client;

import com.wentory.tabletopstuff.prototype.*;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

public final class PreparationClient {
    static final class Play {
        final PreparationUpdate.Played card;final long at=System.nanoTime();final int[] faces;final List<HandLayout.Card> before;final float lift;
        Play(PreparationUpdate.Played card,int[] faces,List<HandLayout.Card> before,float lift){this.card=card;this.faces=faces;this.before=before;this.lift=lift;}
        float progress(){return fraction(at,PlayMotion.LENGTH);}
    }
    public static final class State {
        private final Map<UUID,List<Integer>> rpsOrders=new HashMap<>();
        public PreparationUpdate update;long phaseAt,actionAt,lastAt;UUID lastPlayer=PreparationUpdate.NONE;boolean acknowledged,lastFalse;
        long revealAt;
        final Map<UUID,Long> takeAt=new HashMap<>();
        final Map<UUID,Long> awayAt=new HashMap<>();
        final Map<UUID,Long> awayAckAt=new HashMap<>();
        final Map<UUID,Play> plays=new HashMap<>();
        final Map<UUID,List<HandLayout.Card>> hands=new HashMap<>();
        final Map<UUID,int[]> priorFaces=new HashMap<>(),takenIndices=new HashMap<>();
        final Map<UUID,List<HandLayout.Card>> priorHands=new HashMap<>();
        State(PreparationUpdate update){this.update=update;phaseAt=System.nanoTime();actionAt=0;}
        List<Integer> rpsOrder(UUID player){
            return rpsOrders.computeIfAbsent(player,id->{
                var signs=new ArrayList<>(List.of(0,1,2));
                Collections.shuffle(signs);
                return List.copyOf(signs);
            });
        }
        Play play(UUID id){Play p=plays.get(id);return p!=null&&p.progress()<1?p:null;}
        public PreparationUpdate.Actor actor(UUID id){return update.actors().stream().filter(a->a.id().equals(id)).findFirst().orElse(null);}
        public float phase(float seconds){return fraction(phaseAt,seconds);}
        public float action(float seconds){return actionAt==0?1:fraction(actionAt,seconds);}
        public float awaySeconds(UUID id){return (System.nanoTime()-awayAt.getOrDefault(id,System.nanoTime()))/1_000_000_000f;}
        public float take(UUID id){if(actor(id).awayState()==AwayState.LEAVING)return actor(id).faces().length==0?1:Math.max(0,1-awaySeconds(id)/PreparationMotion.TAKE);Long at=takeAt.get(id);return at==null?1:fraction(at,PreparationMotion.TAKE);}
        public boolean firstTake(UUID id){return actor(id).awayState()==AwayState.LEAVING||actor(id).awayState()==AwayState.PICKUP||priorFaces.getOrDefault(id,new int[0]).length==0;}
        public boolean merging(UUID id){return actor(id).awayState()!=AwayState.LEAVING&&take(id)<1;}
        public int[] visibleFaces(UUID id){int away=actor(id).awayState();if(update.phase()==0||away==AwayState.AWAY||away==AwayState.RETURN_FADE||away==AwayState.RETIRED)return new int[0];return merging(id)?priorFaces.getOrDefault(id,new int[0]):actor(id).faces();}
    }
    private static final Map<UUID,State> GAMES=new HashMap<>(),ACTORS=new HashMap<>();
    public static void receive(PreparationUpdate update){
        State previous=GAMES.get(update.owner());
        if(!update.active()){
            State removed=GAMES.remove(update.owner());if(removed!=null)removed.update.actors().forEach(a->{if(ACTORS.get(a.id())==removed)ACTORS.remove(a.id());});return;
        }
        boolean first=previous==null||previous.update.rules().round()!=update.rules().round();
        if(first&&previous!=null){CardHover.clear();previous.update.actors().forEach(a->{if(ACTORS.get(a.id())==previous)ACTORS.remove(a.id());});}
        State state=first?new State(update):previous;long now=System.nanoTime();
        if(!update.rps().active()||!state.update.rps().active()||state.update.rps().round()!=update.rps().round()){
            state.rpsOrders.clear();
        }
        var reveal=update.reveal();
        if(reveal.remainingTicks()>0&&(first||state.update.reveal().remainingTicks()==0)){
            state.revealAt=now-(update.revealDurationTicks()-reveal.remainingTicks())*50_000_000L;
        }
        if(!first&&state.update.rules().lastSerial()!=update.rules().lastSerial()){
            state.lastAt=now;state.lastPlayer=update.rules().lastCaller();state.lastFalse=update.rules().lastFalse();
        }
        if(!first&&state.update.phase()!=update.phase()){state.phaseAt=now;if(update.phase()==0){state.acknowledged=false;state.plays.clear();state.takeAt.clear();state.priorFaces.clear();state.priorHands.clear();CardHover.clear();}}
        if(!first&&state.update.actionSerial()!=update.actionSerial())state.actionAt=now;
        if(!first&&update.discarded().size()>state.update.discarded().size()){
            for(int i=state.update.discarded().size();i<update.discarded().size();i++){
                var played=update.discarded().get(i);var old=state.actor(played.player());
                if(old!=null&&played.index()>=0&&played.index()<old.faces().length){
                    float lift=0;var mc=Minecraft.getInstance();
                    if(mc!=null&&Minecraft.getInstance().player!=null&&Minecraft.getInstance().player.getUUID().equals(played.player())&&CardHover.selected()==played.index())lift=CardHover.offset(played.index());
                    state.plays.put(played.player(),new Play(played,old.faces(),state.hands.getOrDefault(played.player(),List.of()),lift));
                    if(lift>0)CardHover.clear();
                }
            }
        }
        for(var actor:update.actors()){
            var old=state.actor(actor.id());
            if(first||old==null||old.awayState()!=actor.awayState()){
                state.awayAt.put(actor.id(),now);state.awayAckAt.remove(actor.id());
            }
            if(!first&&old!=null&&old.takeSerial()!=actor.takeSerial()){
                boolean returning=actor.awayState()==AwayState.PICKUP;
                state.priorFaces.put(actor.id(),returning?new int[0]:old.faces());state.priorHands.put(actor.id(),returning?List.of():state.hands.getOrDefault(actor.id(),List.of()));
                state.takenIndices.put(actor.id(),actor.fromDeck()||returning?new int[0]:Arrays.copyOf(old.pile(),actor.taken()));
                state.takeAt.put(actor.id(),now);
            }
            if(first||old==null||old.faces().length!=actor.faces().length)state.hands.put(actor.id(),HandLayout.arrange(actor.faces().length));
            PrototypeClient.preparedHidden(actor.id(),actor.hidden());
            if(actor.awayState()!=AwayState.RETIRED)ACTORS.put(actor.id(),state);
            else if(ACTORS.get(actor.id())==state)ACTORS.remove(actor.id());
        }
        state.update=update;GAMES.put(update.owner(),state);
    }
    private static float fraction(long since,float seconds){return Math.max(0,Math.min(1,(System.nanoTime()-since)/(seconds*1_000_000_000f)));}
    public static State of(Entity entity){return entity==null?null:ACTORS.get(entity.getUUID());}
    public static State local(){return of(Minecraft.getInstance().player);}
    public static State at(BlockPos pos){return GAMES.values().stream().filter(g->g.update.table().equals(pos)).findFirst().orElse(null);}
    public static Collection<State> games(){return GAMES.values();}
    public static List<HandLayout.Card> hand(Entity entity){State game=of(entity);if(game==null)return null;int away=game.actor(entity.getUUID()).awayState();if(game.update.phase()==0||away==AwayState.AWAY||away==AwayState.RETURN_FADE||away==AwayState.RETIRED)return List.of();return game.merging(entity.getUUID())?game.priorHands.getOrDefault(entity.getUUID(),List.of()):game.hands.getOrDefault(entity.getUUID(),List.of());}
    public static int[] faces(Entity entity){State game=of(entity);var actor=game==null?null:game.actor(entity.getUUID());return actor==null?null:game.visibleFaces(entity.getUUID());}
    public static boolean dealerHolding(Entity entity){State game=of(entity);return game!=null&&game.actor(entity.getUUID()).awayState()==AwayState.ACTIVE&&game.update.rules().dealer().equals(entity.getUUID())&&game.update.phase()>=1&&game.update.phase()<=3;}
    public static float fade(){
        State game=local();if(game==null)return 0;
        var local=Minecraft.getInstance().player;var actor=game.actor(local.getUUID());
        if(actor!=null){
            int status=actor.awayState();float time=game.awaySeconds(actor.id());
            float placement=actor.faces().length==0?0:PreparationMotion.TAKE;
            float fade=status==AwayState.LEAVING?Math.min(1,Math.max(0,(time-placement)/.2f)):status==AwayState.RETURN_FADE?Math.min(1,time/.2f):status==AwayState.PICKUP?Math.max(0,1-time/.2f):0;
            if((status==AwayState.LEAVING||status==AwayState.RETURN_FADE)&&fade>=1){
                long now=System.nanoTime();
                if(now-game.awayAckAt.getOrDefault(actor.id(),0L)>=250_000_000L){
                    game.awayAckAt.put(actor.id(),now);PrototypeNetwork.lobbyAction(PreparationUpdate.AWAY_BLACK_READY,0);
                }
            }
            if(status==AwayState.AWAY)return 1;
            if(status!=AwayState.ACTIVE)return fade;
        }
        if(game.update.phase()==0){
            float value=game.phase(.2f);
            if(value>=1&&!game.acknowledged){game.acknowledged=true;PrototypeNetwork.lobbyAction(PreparationGames.BLACK_READY,0);}
            return value;
        }
        return game.update.phase()==1?1-game.phase(.2f):0;
    }
    public static boolean hasHand(){var player=Minecraft.getInstance().player;State game=of(player);return game==null||game.actor(player.getUUID()).awayState()==AwayState.ACTIVE&&game.play(player.getUUID())==null&&game.visibleFaces(player.getUUID()).length>0;}
    public static void clear(){GAMES.clear();ACTORS.clear();PreparationRenderer.clear();}
}
