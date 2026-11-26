package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.ModContent;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import com.wentory.tabletopstuff.nullgame.DealPreparation;
import com.wentory.tabletopstuff.nullgame.NullRound;
import com.wentory.tabletopstuff.nullgame.NullHouseRule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.*;

/** Manual preparation and server-authoritative classic NULL match. */
public final class PreparationGames {
    public static final int BLACK_READY=104;
    private static final class Participant {
        final AwayState absence=new AwayState();
        final UUID id;final String name;final LobbyPlaces.Place place;int takeSerial,taken;long nextTake,nextLastCall;boolean fromDeck,hidden;
        Participant(UUID id,String name,LobbyPlaces.Place place){this.id=id;this.name=name;this.place=place;}
    }
    private static final class Game {
        final List<SoundCue> sounds=new ArrayList<>();
        final UUID owner;final ServerLevel level;final BlockPos table;
        final List<ServerPlayer> people;final LinkedHashMap<UUID,Participant> actors=new LinkedHashMap<>();
        final List<LobbyPlaces.Place> extras;final Map<UUID,com.wentory.tabletopstuff.nullgame.NullBot.Difficulty> bots;final Set<UUID> ready=new HashSet<>();
        final int firstDealerMode,nextDealerMode,houseRules;
        final Map<UUID,Integer> roundHandPoints=new LinkedHashMap<>();
        RpsContest contest;CancellationVote vote;boolean firstContest,restarting;long revealedAt;UUID lastRoundWinner,lastRoundLoser;
        NullRound rules;long rulesReadyAt,lastUntil,revealUntil;int round=1,recycled;boolean scored;final Map<UUID,Integer> scores=new HashMap<>();
        int lastEvent;UUID lastSpeaker=PreparationUpdate.NONE;boolean lastFalse;
        UUID host,dealer;final List<PreparationUpdate.Played> discarded=new ArrayList<>();DealPreparation cards;int phase=0,serial,action,packet;UUID target=new UUID(0,0);long phaseTick,nextAction,nextBotAction;
        boolean botShuffled;
        Game(List<ServerPlayer> people,BlockPos table,Map<UUID,LobbyPlaces.Place> places,List<LobbyPlaces.Place> extras,Map<UUID,com.wentory.tabletopstuff.nullgame.NullBot.Difficulty> bots,int firstDealerMode,int nextDealerMode,int houseRules){
            this.people=new ArrayList<>(people);this.owner=people.get(0).getUUID();this.host=this.owner;this.dealer=this.owner;this.level=people.get(0).serverLevel();this.table=table.immutable();this.extras=extras;this.bots=bots;
            this.firstDealerMode=firstDealerMode;this.nextDealerMode=nextDealerMode;
            this.houseRules=houseRules;
            people.forEach(p->actors.put(p.getUUID(),new Participant(p.getUUID(),p.getGameProfile().getName(),places.get(p.getUUID()))));phaseTick=level.getGameTime();
        }
    }
    private static final Map<UUID,Game> MEMBERS=new HashMap<>();
    private static final Map<ServerLevel,Map<BlockPos,Set<Game>>> PROTECTED=new IdentityHashMap<>();
    private record SoundCue(long tick,net.minecraft.sounds.SoundEvent sound,net.minecraft.world.phys.Vec3 at,float pitch){}
    private static void sound(Game game,java.util.function.Supplier<net.minecraft.sounds.SoundEvent> sound,int delay,net.minecraft.world.phys.Vec3 at){
        float pitch=(float)Math.pow(2,(game.level.getRandom().nextFloat()*2-1)/12.0);
        game.sounds.add(new SoundCue(game.level.getGameTime()+delay,sound.get(),at,pitch));
    }
    private static net.minecraft.world.phys.Vec3 surface(Game game){
        double height=LobbyBlock.surfaceHeight(game.level.getBlockState(game.table));
        return new net.minecraft.world.phys.Vec3(game.table.getX()+.5,game.table.getY()+height+.01,game.table.getZ()+.5);
    }
    private static net.minecraft.world.phys.Vec3 pile(Game game,Participant actor){
        int seat=actor.place.index();return surface(game).add(seat==1?.34:seat==3?-.34:0,0,seat==0?-.34:seat==2?.34:0);
    }
    private static net.minecraft.world.phys.Vec3 corner(Game game){
        var center=surface(game);var forward=center.subtract(game.actors.get(game.dealer).place.feet()).multiply(1,0,1).normalize();
        return center.add(forward.cross(new net.minecraft.world.phys.Vec3(0,1,0)).scale(.28)).subtract(forward.scale(.28));
    }
    public static boolean active(UUID player){return MEMBERS.containsKey(player);}
    public static boolean tableInUse(ServerLevel level,BlockPos pos){return MEMBERS.values().stream().anyMatch(g->g.level==level&&g.table.equals(pos));}
    public static boolean protectedBlock(ServerLevel level,BlockPos pos){var blocks=PROTECTED.get(level);return blocks!=null&&blocks.containsKey(pos);}
    private static void protect(Game game,boolean active){
        if(active){var blocks=PROTECTED.computeIfAbsent(game.level,key->new HashMap<>());for(var pos:List.of(game.table,game.table.below()))blocks.computeIfAbsent(pos,key->new HashSet<>()).add(game);}
        else{var blocks=PROTECTED.get(game.level);if(blocks==null)return;for(var pos:List.of(game.table,game.table.below())){var games=blocks.get(pos);if(games!=null){games.remove(game);if(games.isEmpty())blocks.remove(pos);}}if(blocks.isEmpty())PROTECTED.remove(game.level);}
    }
    private static boolean paused(Game game){return game.actors.values().stream().anyMatch(a->a.absence.paused());}
    public static boolean tryReturn(ServerPlayer player,BlockPos pos){
        Game game=MEMBERS.get(player.getUUID());if(game==null||!game.table.equals(pos)||player.serverLevel()!=game.level)return false;
        var actor=game.actors.get(player.getUUID());if(actor.absence.state()!=AwayState.AWAY)return false;
        if(!player.isAlive()||player.isSpectator()||player.isPassenger()||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>36)return true;
        var fresh=LobbyPlaces.find(game.level,pos,game.actors.keySet());
        if(fresh.stream().noneMatch(p->p.index()==actor.place.index()&&p.seated()==actor.place.seated()&&p.feet().distanceToSqr(actor.place.feet())<.000001)){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tabletop_stuff.lobby.no_space"),true);return true;
        }
        if(actor.absence.returning(game.level.getGameTime())){publish(game,true);PrototypeSessions.beginPreparation(player,pos);}
        return true;
    }
    public static void start(List<ServerPlayer> people,BlockPos table,Map<UUID,LobbyPlaces.Place> places,List<LobbyPlaces.Place> extras,Map<UUID,com.wentory.tabletopstuff.nullgame.NullBot.Difficulty> bots,int first,int next,int houseRules){
        Game game=new Game(people,table,places,people.size()==1?extras:List.of(),bots,first,next,houseRules);
        protect(game,true);
        people.forEach(p->MEMBERS.put(p.getUUID(),game));publish(game,true);
        people.forEach(p->PrototypeSessions.beginPreparation(p,table));
    }
    public static void action(ServerPlayer player,int action,int value,UUID target){
        Game game=MEMBERS.get(player.getUUID());if(game==null||player.serverLevel()!=game.level||!player.isAlive())return;
        if(action==BLACK_READY&&game.phase==0){game.ready.add(player.getUUID());return;}
        if(action==PreparationUpdate.AWAY_BLACK_READY){
            var actor=game.actors.get(player.getUUID());long tick=game.level.getGameTime();
            if(actor.absence.detach(tick,com.wentory.tabletopstuff.GameConfig.awayMinutes())){
                publish(game,true);PrototypeSessions.releaseForAway(player);
            }else if(actor.absence.state()==AwayState.RETURN_FADE&&tick-actor.absence.changed()>=4&&actor.absence.pickup(tick)){
                PrototypeSessions.enterAt(player,game.table,actor.place);actor.hidden=false;
                actor.taken=game.cards==null?0:game.cards.handFaces(actor.id).length;actor.fromDeck=false;actor.takeSerial++;actor.nextTake=tick+30;
                sound(game,CardSounds.HAND_PICKUP,13,pile(game,actor));
                sound(game,CardSounds.CARD_TO_HAND,27,actor.place.feet().add(0,1,0));publish(game,true);
            }
            return;
        }
        if(action==PreparationUpdate.STEP_AWAY){
            var actor=game.actors.get(player.getUUID());long tick=game.level.getGameTime();
            if(game.phase==4&&game.rules!=null&&tick>=game.rulesReadyAt&&tick>=actor.nextTake&&!player.getUUID().equals(game.rules.chooser())&&actor.absence.leave(tick,game.cards.handFaces(actor.id).length>0)){
                actor.hidden=false;actor.taken=game.cards.handFaces(actor.id).length;actor.fromDeck=false;
                if(actor.taken>0)sound(game,CardSounds.DECK_PLACE,17,pile(game,actor));publish(game,true);
            }
            return;
        }
        if(action==PreparationUpdate.START_CANCEL_VOTE||action==PreparationUpdate.CANCEL_VOTE){
            UUID acting=player.getUUID();var actor=game.actors.get(acting);
            if(game.phase==0||actor==null||actor.absence.state()!=AwayState.ACTIVE)return;
            long now=game.level.getGameTime();
            if(action==PreparationUpdate.START_CANCEL_VOTE){
                if(game.vote!=null)return;
                game.vote=new CancellationVote(game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).map(p->p.id).toList(),game.host,now,30);
                game.vote.choose(acting,true,now);
                for(UUID bot:game.bots.keySet()){
                    var participant=game.actors.get(bot);
                    if(participant!=null&&participant.absence.state()==AwayState.ACTIVE)game.vote.choose(bot,true,now);
                }
            }else{
                if(game.vote==null||value<0||value>1||!game.vote.choose(acting,value==1,now))return;
            }
            if(game.vote.passed()){cancel(game);return;}
            if(game.vote.finished(now))game.vote=null;
            publish(game,true);return;
        }
        UUID acting=player.getUUID();
        act(game,acting,action,value,target);
    }
    private static void act(Game game,UUID acting,int action,int value,UUID target){
        if(paused(game))return;
        long tick=game.level.getGameTime();
        if(action==PreparationUpdate.RPS_CHOICE){
            if(game.contest!=null&&game.phase==PreparationUpdate.RPS_PHASE&&game.contest.choose(acting,value)){
                if(game.contest.revealed())game.revealedAt=tick;
                publish(game,true);
            }
            return;
        }
        if(game.contest!=null||game.cards==null||game.phase<2)return;
        long now=game.level.getGameTime();
        if(game.vote!=null&&game.vote.finished(now)){
            if(game.vote.passed()){cancel(game);return;}
            game.vote=null;publish(game,true);
        }
        if(now<game.revealUntil)return;
        collectDiscard(game,now);
        if(action==PreparationUpdate.COLOR){if(game.rules!=null&&now>=game.rulesReadyAt&&game.rules.chooseColor(acting,value))publish(game,true);return;}
        if(action==PreparationUpdate.PASS){if(game.rules!=null&&now>=game.rulesReadyAt&&game.rules.pass(acting))publish(game,true);return;}
        if(action==PreparationUpdate.CHALLENGE){
            if(game.rules!=null&&now>=game.rulesReadyAt&&game.rules.challenge(acting)){
                var source=game.actors.get(game.rules.revealSource());
                var viewer=game.actors.get(acting);
                game.revealUntil=now+PreparationUpdate.revealDurationTicks(source.place.index(),viewer.place.index());
                game.rulesReadyAt=game.revealUntil;
                publish(game,true);
            }
            return;
        }
        if(action==PreparationUpdate.LAST){if(game.rules!=null&&!game.rules.over()){
            var participant=game.actors.get(acting);
            if(participant==null||now<participant.nextLastCall)return;
            participant.nextLastCall=now+100;
            game.lastFalse=!game.rules.callLast(acting);
            game.lastSpeaker=acting;game.lastEvent++;
            game.lastUntil=now+40;
            int note=11+game.level.getRandom().nextInt(3);
            var mouth=participant.place.feet().add(0,1.5,0);
            game.level.playSound(null,mouth.x,mouth.y,mouth.z,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),net.minecraft.sounds.SoundSource.PLAYERS,.8f,(float)Math.pow(2,(note-12)/12.0));
            publish(game,true);
        }return;}
        if(action==PreparationUpdate.NEXT_ROUND){
            if(game.rules==null||!game.rules.over()||!acting.equals(game.host)||now<game.rulesReadyAt||game.scores.getOrDefault(game.rules.winner(),0)>=500||game.rules.remainingPlayers()<2)return;
            var points=new LinkedHashMap<UUID,Integer>();for(var p:game.actors.values())if(p.absence.state()!=AwayState.RETIRED)points.put(p.id,game.roundHandPoints.getOrDefault(p.id,0));
            List<UUID> candidates=nextDealers(game,game.host,game.rules.winner(),points);
            startContest(game,candidates,false);return;
        }

        if(action==PreparationUpdate.PLAY){
            Participant participant=game.actors.get(acting);
            if(game.phase!=4||participant==null||participant.hidden||now<participant.nextTake||game.rules==null||now<game.rulesReadyAt)return;
            int face=game.rules.play(acting,value);if(face<0)return;
            var random=game.level.getRandom();
            boolean centered=random.nextInt(5)==0;
            float forward=centered?0:(random.nextFloat()-.5f)*.10f,side=centered?0:(random.nextFloat()-.5f)*.05f;
            double angle=Math.toRadians(participant.place.yaw());
            float x=(float)(-Math.sin(angle)*forward-Math.cos(angle)*side),z=(float)(Math.cos(angle)*forward-Math.sin(angle)*side);
            game.discarded.add(new PreparationUpdate.Played(game.discarded.size(),acting,value,face,-participant.place.yaw()+(random.nextFloat()-.5f)*18,x,z));
            sound(game,CardSounds.FAN_TAKE,4,participant.place.feet().add(0,1,0));
            sound(game,CardSounds.CARD_LAND,20,surface(game).add(x,0,z));
            participant.nextTake=now+20;game.rulesReadyAt=now+20;game.action=action;game.target=acting;game.serial++;publish(game,true);return;
        }

        if(action==PreparationUpdate.DRAW){
            Participant participant=game.actors.get(acting);
            if(game.phase!=4||participant==null||now<participant.nextTake||game.rules==null||now<game.rulesReadyAt||!game.rules.canDraw(acting))return;
            boolean added=game.rules.draw(acting);syncRecycled(game);
            if(!added){publish(game,true);return;}
            sound(game,CardSounds.CARD_TAKE,13,corner(game));
            sound(game,CardSounds.CARD_TO_HAND,27,participant.place.feet().add(0,1,0));
            game.rulesReadyAt=now+30;
            participant.taken=1;participant.fromDeck=true;participant.takeSerial++;participant.nextTake=now+30;publish(game,true);return;
        }
        if(action==PreparationUpdate.TAKE){
            Participant participant=game.actors.get(acting);
            if(participant==null||now<participant.nextTake||acting.equals(game.dealer)&&game.phase!=4)return;
            boolean inFlight=game.action==PreparationUpdate.DEAL&&game.target.equals(acting)&&now<game.nextAction;
            int count=game.cards.take(acting,inFlight);if(count==0)return;
            sound(game,CardSounds.HAND_PICKUP,13,pile(game,participant));
            sound(game,CardSounds.CARD_TO_HAND,27,participant.place.feet().add(0,1,0));
            participant.taken=count;participant.fromDeck=false;participant.takeSerial++;participant.nextTake=now+30;publish(game,true);return;
        }
        if(!acting.equals(game.dealer)||game.phase!=2||now<game.nextAction)return;
        if(action==PreparationUpdate.SHUFFLE){
            int packet=game.cards.shufflePacket();if(packet==0)return;game.packet=packet;game.nextAction=now+13;
            var at=game.actors.get(acting).place.feet().add(0,1,0);
            sound(game,CardSounds.SHUFFLE_TAKE,2,at);sound(game,CardSounds.SHUFFLE_INSERT,9,at);
        }else if(action==PreparationUpdate.DEAL){
            if(!game.cards.deal(target))return;game.target=target;game.nextAction=now+8;
            sound(game,CardSounds.CARD_TAKE,2,game.actors.get(acting).place.feet().add(0,1,0));
            sound(game,CardSounds.CARD_LAND,8,pile(game,game.actors.get(target)));
        }else if(action==PreparationUpdate.PLACE){
            if(!game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).allMatch(p->game.cards.delivered(p.id)==7))return;game.phase=3;game.phaseTick=now;game.nextAction=now+40;
            sound(game,CardSounds.DECK_PLACE,30,corner(game));
        }else return;
        game.action=action;game.serial++;publish(game,true);
    }
    public static boolean hideHand(ServerPlayer player,boolean hidden){
        Game game=MEMBERS.get(player.getUUID());if(game==null)return false;
        UUID id=player.getUUID();
        Participant actor=game.actors.get(id);if(actor!=null&&actor.absence.state()==AwayState.ACTIVE&&actor.hidden!=hidden){actor.hidden=hidden;publish(game,true);}return true;
    }
    private static void startContest(Game game,List<UUID> candidates,boolean first){
        game.contest=new RpsContest(candidates);game.firstContest=first;game.revealedAt=0;
        if(game.contest.winner()!=null){finishContest(game);return;}
        game.phase=PreparationUpdate.RPS_PHASE;game.phaseTick=game.level.getGameTime();publish(game,true);
    }
    private static void finishContest(Game game){
        game.dealer=game.contest.winner();game.contest=null;
        if(!game.firstContest){
            game.cards=new DealPreparation(game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).map(p->p.id).toList(),new Random(game.level.getRandom().nextLong()));
            game.rules=null;game.round++;game.scored=false;game.recycled=0;game.discarded.clear();game.action=0;
            for(var participant:game.actors.values()){participant.hidden=false;participant.nextTake=0;participant.taken=0;participant.takeSerial++;}
        }
        game.botShuffled=false;
        game.phase=1;game.phaseTick=game.level.getGameTime();
        game.level.setBlock(game.table,game.level.getBlockState(game.table).setValue(LobbyBlock.DECK_PRESENT,false),3);publish(game,true);
    }
    public static void tick(ServerPlayer owner){
        Game game=MEMBERS.get(owner.getUUID());if(game==null)return;
        // Respawning creates a new ServerPlayer object with the same participant UUID.
        game.people.replaceAll(p->{var current=game.level.getServer().getPlayerList().getPlayer(p.getUUID());return current==null?p:current;});
        var leader=game.people.stream().filter(p->MEMBERS.get(p.getUUID())==game).findFirst().orElse(null);
        if(leader!=owner)return;
        if(owner.serverLevel()!=game.level&&!game.actors.get(owner.getUUID()).absence.paused()||!game.level.getBlockState(game.table).is(ModContent.LOBBY.get())){cancel(game);return;}
        long now=game.level.getGameTime();
        if(game.vote!=null&&game.vote.finished(now)){if(game.vote.passed()){cancel(game);return;}game.vote=null;publish(game,true);}
        boolean restart=false;
        for(var actor:game.actors.values()){
            Entity entity=game.level.getEntity(actor.id);
            if(game.cards!=null&&(entity==null||!entity.isAlive())){
                if(game.bots.containsKey(actor.id)&&actor.absence.state()!=AwayState.RETIRED){
                    restart|=game.contest!=null||game.rules==null&&actor.id.equals(game.dealer);
                    actor.absence.retire(now);
                    if(game.rules==null)game.cards.returnHand(actor.id);else game.rules.retire(actor.id);
                    publish(game,true);
                }else if(!game.bots.containsKey(actor.id))markDead(game,actor,now);
            }
            if(actor.absence.state()==AwayState.LEAVING&&now-actor.absence.changed()>=actor.absence.leavingTicks()+4&&actor.absence.detach(now,com.wentory.tabletopstuff.GameConfig.awayMinutes())){
                publish(game,true);
                game.people.stream().filter(p->p.getUUID().equals(actor.id)).findFirst().ifPresent(PrototypeSessions::releaseForAway);
            }
            if(actor.absence.finishPickup(now))publish(game,true);
            if(actor.absence.expired(now)){
                restart|=game.contest!=null||game.rules==null&&actor.id.equals(game.dealer);
                actor.absence.retire(now);if(game.rules!=null)game.rules.retire(actor.id);else if(game.cards!=null)game.cards.returnHand(actor.id);MEMBERS.remove(actor.id);
                game.people.stream().filter(p->p.getUUID().equals(actor.id)).findFirst().ifPresent(PrototypeSessions::releaseForAway);
                if(actor.id.equals(game.host))game.host=game.people.stream().filter(p->MEMBERS.get(p.getUUID())==game).map(ServerPlayer::getUUID).findFirst().orElse(game.owner);
                publish(game,true);
            }
        }
        if(game.people.stream().noneMatch(p->MEMBERS.get(p.getUUID())==game)){cancel(game);return;}
        if((game.cards!=null||game.restarting||game.actors.values().stream().anyMatch(p->p.absence.state()==AwayState.RETIRED))&&(game.rules==null||game.contest!=null)&&game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).count()<2){cancel(game);return;}
        if(restart){restartPreparation(game,now);return;}
        for(var iterator=game.sounds.iterator();iterator.hasNext();){
            var cue=iterator.next();if(cue.tick()>now)continue;
            game.level.playSound(null,cue.at().x,cue.at().y,cue.at().z,cue.sound(),net.minecraft.sounds.SoundSource.BLOCKS,.8f,cue.pitch());iterator.remove();
        }
        if(paused(game)){game.phaseTick++;if(game.contest!=null&&game.contest.revealed())game.revealedAt++;if(now%20==0)publish(game,true);return;}
        if(game.contest!=null){
            tickBots(game,now);
            if(game.vote!=null&&now%20==0)publish(game,true);
            if(game.contest.revealed()&&now-game.revealedAt>=60){
                if(game.contest.winner()!=null)finishContest(game);
                else{game.contest.nextRound();game.revealedAt=0;publish(game,true);}
            }
            return;
        }
        if(game.phase==0){
            if(now-game.phaseTick>200){cancel(game);return;}
            if(!game.people.stream().filter(p->MEMBERS.get(p.getUUID())==game).allMatch(p->game.ready.contains(p.getUUID())))return;
            List<LobbyPlaces.Place> fresh=LobbyPlaces.find(game.level,game.table,new HashSet<>(game.actors.keySet()));
            for(var actor:game.actors.values()){
                if(actor.absence.state()==AwayState.RETIRED)continue;
                if(fresh.stream().noneMatch(p->p.index()==actor.place.index()&&p.seated()==actor.place.seated()&&p.feet().distanceToSqr(actor.place.feet())<.000001)){
                    owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("tabletop_stuff.lobby.no_space"),true);cancel(game);return;
                }
            }
            for(ServerPlayer person:game.people){
                if(MEMBERS.get(person.getUUID())!=game)continue;
                var place=game.actors.get(person.getUUID()).place;PrototypeSessions.enterAt(person,game.table,place);
            }
            if(!game.restarting&&!game.extras.isEmpty()){
                List<LobbyPlaces.Place> extraPlaces=fresh.stream().filter(p->game.extras.stream().anyMatch(e->e.index()==p.index())).toList();
                if(!game.bots.isEmpty()){
                    if(extraPlaces.size()!=game.bots.size()){cancel(game);return;}
                    // Keep the reservation order so each bot lands on its assigned place.
                    extraPlaces=game.extras.stream()
                        .flatMap(reserved->fresh.stream().filter(p->p.index()==reserved.index())).toList();
                }
                List<TestPlayer> tests=TestPlayers.spawn(owner,game.table,extraPlaces,List.copyOf(game.bots.keySet()));
                if(!game.bots.isEmpty()&&tests.size()!=game.bots.size()){cancel(game);return;}
                for(TestPlayer test:tests){var place=extraPlaces.stream().min(Comparator.comparingDouble(p->p.feet().distanceToSqr(test.position()))).orElseThrow();game.actors.put(test.getUUID(),new Participant(test.getUUID(),test.getDisplayName().getString(),place));}
            }
            List<UUID> eligible=game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).map(p->p.id).toList();
            game.cards=new DealPreparation(eligible,new Random(game.level.getRandom().nextLong()));
            List<UUID> candidates;
            if(game.round==1)candidates=game.firstDealerMode==DealerSelection.FIRST_RPS?eligible:List.of(eligible.contains(game.host)?game.host:eligible.get(0));
            else{
                var points=new LinkedHashMap<UUID,Integer>();eligible.forEach(id->points.put(id,game.roundHandPoints.getOrDefault(id,0)));
                candidates=nextDealers(game,eligible.contains(game.host)?game.host:eligible.get(0),game.lastRoundWinner,points);
            }
            game.restarting=false;startContest(game,candidates,true);
        }else if(game.phase==1&&now-game.phaseTick>=30){game.phase=2;game.phaseTick=now;publish(game,true);}
        else if(game.phase==3&&now-game.phaseTick>=40){game.phase=4;game.phaseTick=now;publish(game,true);}
        else if(now%20==0)publish(game,true);
        if(game.phase==4&&game.rules==null&&game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).allMatch(p->game.cards.delivered(p.id)==7&&game.cards.pileIndices(p.id).length==0)){
            List<UUID> order=game.actors.values().stream().filter(p->p.absence.state()!=AwayState.RETIRED).sorted(Comparator.comparingInt(p->p.place.index())).map(p->p.id).toList();
            game.rules=new NullRound(game.cards,order,game.dealer,game.houseRules);
            game.discarded.add(new PreparationUpdate.Played(0,game.dealer,-1,game.cards.topFace(),-game.actors.get(game.dealer).place.yaw(),0,0));
            game.rulesReadyAt=game.actors.values().stream().mapToLong(p->p.nextTake).max().orElse(now);publish(game,true);
        }
        if(game.rules!=null){
            collectDiscard(game,now);
            if(now>=game.rulesReadyAt&&game.cards.remaining()==0&&game.cards.discarded()>1){game.cards.refill();syncRecycled(game);publish(game,true);}
            if(now==game.rulesReadyAt)publish(game,true);
            if(game.rules.over()&&!game.scored){
                game.lastRoundWinner=game.rules.winner();
                game.lastRoundLoser=game.rules.loser();
                game.roundHandPoints.clear();
                for(var actor:game.actors.values())if(actor.absence.state()!=AwayState.RETIRED){int points=0;for(int face:game.cards.handFaces(actor.id))points+=com.wentory.tabletopstuff.nullgame.NullCard.fromFace(face).points();game.roundHandPoints.put(actor.id,points);}
                if(!NullHouseRule.LAST_STANDING.enabled(game.houseRules))game.scores.merge(game.rules.winner(),game.rules.roundPoints(),Integer::sum);
                game.scored=true;
                if(game.scores.getOrDefault(game.rules.winner(),0)>=500||game.rules.remainingPlayers()<2)protect(game,false);
                publish(game,true);
            }
        }
        tickBots(game,now);
    }
    private static void tickBots(Game game,long now){
        if(game.bots.isEmpty()||now<game.nextBotAction||now<game.revealUntil||paused(game))return;
        var random=game.level.getRandom();
        if(game.contest!=null){
            if(!game.contest.revealed()){
                for(UUID bot:game.contest.candidates()){
                    if(game.bots.containsKey(bot)&&game.contest.choice(bot)<0){
                        botAction(game,bot,PreparationUpdate.RPS_CHOICE,random.nextInt(3),PreparationUpdate.NONE,now);
                        return;
                    }
                }
            }
            return;
        }
        if(game.cards==null)return;
        if(game.phase==2&&game.bots.containsKey(game.dealer)&&now>=game.nextAction){
            if(!game.botShuffled){
                game.botShuffled=true;
                botAction(game,game.dealer,PreparationUpdate.SHUFFLE,0,PreparationUpdate.NONE,now);
                return;
            }
            var next=game.actors.values().stream().filter(actor->actor.absence.state()==AwayState.ACTIVE)
                .filter(actor->game.cards.delivered(actor.id)<7)
                .min(Comparator.<Participant>comparingInt(actor->game.cards.delivered(actor.id))
                    .thenComparingInt(actor->Math.floorMod(actor.place.index()-game.actors.get(game.dealer).place.index()-1,4)));
            if(next.isPresent())botAction(game,game.dealer,PreparationUpdate.DEAL,0,next.get().id,now);
            else botAction(game,game.dealer,PreparationUpdate.PLACE,0,PreparationUpdate.NONE,now);
            return;
        }
        if(game.phase!=4)return;
        for(UUID bot:game.bots.keySet()){
            var participant=game.actors.get(bot);
            if(participant==null||participant.absence.state()!=AwayState.ACTIVE||now<participant.nextTake)continue;
            if(game.cards.pileIndices(bot).length>0){
                botAction(game,bot,PreparationUpdate.TAKE,0,PreparationUpdate.NONE,now);
                return;
            }
            if(game.rules==null||game.rules.over()||now<game.rulesReadyAt)continue;
            int[] hand=game.cards.handFaces(bot);
            var difficulty=game.bots.get(bot);
            var move=com.wentory.tabletopstuff.nullgame.NullBot.decide(game.rules,bot,hand,now>=participant.nextLastCall,difficulty,random::nextInt);
            if(move==null)continue;
            int action=switch(move.action()){
                case COLOR->PreparationUpdate.COLOR;
                case LAST->PreparationUpdate.LAST;
                case CHALLENGE->PreparationUpdate.CHALLENGE;
                case PLAY->PreparationUpdate.PLAY;
                case DRAW->PreparationUpdate.DRAW;
                case PASS->PreparationUpdate.PASS;
            };
            botAction(game,bot,action,move.value(),PreparationUpdate.NONE,now);
            return;
        }
    }
    private static void botAction(Game game,UUID bot,int action,int value,UUID target,long now){
        game.nextBotAction=now+10;
        act(game,bot,action,value,target);
    }

    private static UUID orNone(UUID id){return id==null?PreparationUpdate.NONE:id;}
    private static List<UUID> nextDealers(Game game,UUID host,UUID winner,Map<UUID,Integer> points){
        if(game.nextDealerMode==DealerSelection.NEXT_LOSER&&NullHouseRule.LAST_STANDING.enabled(game.houseRules)){
            return List.of(points.containsKey(game.lastRoundLoser)?game.lastRoundLoser:host);
        }
        return DealerSelection.next(game.nextDealerMode,host,winner,points);
    }
    private static void restartPreparation(Game game,long now){
        if(game.contest!=null&&!game.firstContest)game.round++;
        game.restarting=true;game.contest=null;game.cards=null;game.rules=null;game.phase=0;game.phaseTick=now;
        game.ready.clear();game.sounds.clear();game.discarded.clear();game.action=0;game.serial++;game.recycled=0;game.rulesReadyAt=0;
        for(var p:game.actors.values()){p.hidden=false;p.taken=0;p.nextTake=0;p.fromDeck=false;p.takeSerial++;}
        game.level.setBlock(game.table,game.level.getBlockState(game.table).setValue(LobbyBlock.DECK_PRESENT,true),3);
        publish(game,true);
    }
    private static void collectDiscard(Game game,long now){
        if(game.rules==null||now<game.rulesReadyAt||!game.cards.collectDiscard())return;
        syncRecycled(game);
        game.level.playSound(null,game.table.getX()+.5,game.table.getY()+.1,game.table.getZ()+.5,
            ModContent.DISCARD_COLLECT.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8f,1f);
        publish(game,true);
    }
    private static void syncRecycled(Game game){
        if(game.recycled==game.cards.recycleSerial())return;game.recycled=game.cards.recycleSerial();
        int keep=game.cards.discarded();
        List<PreparationUpdate.Played> retained=new ArrayList<>(game.discarded.subList(game.discarded.size()-keep,game.discarded.size()));
        game.discarded.clear();
        for(var card:retained)game.discarded.add(new PreparationUpdate.Played(game.discarded.size(),card.player(),-1,card.face(),card.yaw(),card.x(),card.z()));
    }
    private static PreparationUpdate update(Game game,boolean active,UUID viewer){
        List<PreparationUpdate.Actor> actors=new ArrayList<>();long now=game.level.getGameTime();
        UUID logical=viewer;
        for(var p:game.actors.values()){
            boolean retired=p.absence.state()==AwayState.RETIRED;
            int[] faces=game.cards==null||retired?new int[0]:game.cards.handFaces(p.id);List<Integer> allowed=new ArrayList<>();
            boolean ready=game.rules!=null&&!paused(game)&&!retired&&now>=game.rulesReadyAt&&now>=p.nextTake;
            if(ready)for(int i=0;i<faces.length;i++)if(game.rules.canPlay(p.id,i))allowed.add(i);
            if(!logical.equals(p.id))Arrays.fill(faces,-1);
            actors.add(new PreparationUpdate.Actor(p.id,p.place.index(),p.place.feet(),p.place.yaw(),p.place.seated(),game.cards==null||retired?0:game.cards.delivered(p.id),game.cards==null||retired?new int[0]:game.cards.pileIndices(p.id),faces,p.takeSerial,p.taken,p.fromDeck,p.hidden,
                game.rules==null?0:game.rules.debt(p.id),allowed.stream().mapToInt(Integer::intValue).toArray(),ready&&game.rules.canDraw(p.id),ready&&game.rules.canPass(p.id),game.scores.getOrDefault(p.id,0),p.absence.state(),p.name,p.absence.remainingSeconds(now)));
        }
        var rules=game.rules;int[] revealed=rules!=null&&now<game.revealUntil&&rules.revealTo()!=null?rules.revealed():new int[0];
        var reveal=rules!=null&&rules.revealTo()!=null&&now<game.revealUntil
            ?new PreparationUpdate.Reveal(rules.revealSource(),rules.revealTo(),(int)(game.revealUntil-now))
            :PreparationUpdate.NO_REVEAL;
        var state=new PreparationUpdate.Rules(rules!=null,rules==null?PreparationUpdate.NONE:rules.turn(),rules==null?1:rules.direction(),rules==null?-1:rules.color(),rules==null?PreparationUpdate.NONE:orNone(rules.chooser()),rules==null?PreparationUpdate.NONE:orNone(rules.challenger()),rules==null?PreparationUpdate.NONE:orNone(rules.vulnerable()),rules==null||now<game.rulesReadyAt?PreparationUpdate.NONE:orNone(rules.winner()),game.round,rules!=null&&now<game.lastUntil?game.lastSpeaker:PreparationUpdate.NONE,revealed,rules!=null&&rules.challengeWon(),rules!=null&&now<game.revealUntil&&logical.equals(rules.revealTo()),rules!=null&&rules.over()&&(game.scores.getOrDefault(rules.winner(),0)>=500||rules.remainingPlayers()<2),game.lastEvent,game.lastFalse,game.host,paused(game),game.dealer,NullHouseRule.LAST_STANDING.enabled(game.houseRules),rules==null?PreparationUpdate.NONE:orNone(rules.loser()));
        var rps=PreparationUpdate.NO_RPS;
        if(game.contest!=null){
            var contest=game.contest;int[] choices=new int[contest.candidates().size()];Arrays.fill(choices,-1);
            if(contest.revealed())for(int i=0;i<choices.length;i++)choices[i]=contest.choice(contest.candidates().get(i));
            rps=new PreparationUpdate.Rps(true,contest.candidates(),choices,contest.choice(logical),contest.submitted(),contest.revealed(),contest.round(),contest.remaining(),orNone(contest.winner()));
        }
        var vote=game.vote==null?PreparationUpdate.NO_VOTE:new PreparationUpdate.Vote(true,game.vote.yes(),game.vote.no(),game.vote.total(),game.vote.remaining(now),game.vote.choice(logical));
        return new PreparationUpdate(game.owner,game.table,active,game.phase,game.cards==null?108:game.cards.remaining(),game.serial,game.action,game.target,game.packet,game.cards==null?0:game.cards.insertionDepth(),List.copyOf(actors),List.copyOf(game.discarded),state,rps,vote,reveal);
    }
    private static void publish(Game game,boolean active){
        for(ServerPlayer viewer:game.level.players())if(game.people.contains(viewer)||viewer.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(game.table))<128*128)PrototypeNetwork.sendPreparation(viewer,update(game,active,viewer.getUUID()));
    }
    public static void tracked(ServerPlayer viewer,Entity target){
        Game game=MEMBERS.get(target.getUUID());if(game!=null)PrototypeNetwork.sendPreparation(viewer,update(game,true,viewer.getUUID()));
    }
    public static boolean died(ServerPlayer player){
        Game game=MEMBERS.get(player.getUUID());if(game==null)return false;
        markDead(game,game.actors.get(player.getUUID()),game.level.getGameTime());return true;
    }
    public static boolean away(UUID id){Game game=MEMBERS.get(id);return game!=null&&game.actors.get(id).absence.paused();}
    private static void markDead(Game game,Participant actor,long now){
        if(!actor.absence.died(now,com.wentory.tabletopstuff.GameConfig.awayMinutes()))return;
        actor.hidden=false;actor.fromDeck=false;
        game.people.stream().filter(p->p.getUUID().equals(actor.id)).findFirst().ifPresent(PrototypeSessions::releaseForAway);
        publish(game,true);
    }
    public static void leave(ServerPlayer player){Game game=MEMBERS.get(player.getUUID());if(game!=null)cancel(game);}
    private static void cancel(Game game){
        protect(game,false);
        var members=game.people.stream().filter(p->MEMBERS.get(p.getUUID())==game).toList();
        members.forEach(p->MEMBERS.remove(p.getUUID()));publish(game,false);
        if(game.level.getBlockState(game.table).is(ModContent.LOBBY.get()))game.level.setBlock(game.table,game.level.getBlockState(game.table).setValue(LobbyBlock.DECK_PRESENT,true),3);
        TestPlayers.remove(game.owner);members.forEach(PrototypeSessions::leave);
    }
    public static void clear(){MEMBERS.clear();PROTECTED.clear();}
}
