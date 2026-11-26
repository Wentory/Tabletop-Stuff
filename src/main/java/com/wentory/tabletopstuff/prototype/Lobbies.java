package com.wentory.tabletopstuff.prototype;

import com.wentory.tabletopstuff.ModContent;
import com.wentory.tabletopstuff.TabletopStuff;
import com.wentory.tabletopstuff.network.PrototypeNetwork;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative shared lobby. Game settings are data, not a separate NULL screen. */
public final class Lobbies {
    public static final int LEAVE=0, CAPACITY=1, START=2, SEAT=3, ACCESS=4, INVITE=5, CANCEL=6, KICK=7, RULE=9,FIRST_DEALER=10,NEXT_DEALER=11, ADD_BOT=13, BOT_DIFFICULTY=14;
    public static final int RESTORE_SETTINGS=12;
    private static final class Room {
        final ServerLevel level;
        final BlockPos pos;
        final LobbyGame game = LobbyGame.NULL;
        final LinkedHashMap<UUID,String> players = new LinkedHashMap<>();
        final Map<UUID,com.wentory.tabletopstuff.nullgame.NullBot.Difficulty> bots=new LinkedHashMap<>();
        final Map<UUID,Integer> seats = new HashMap<>();
        final Map<UUID,String> invited = new LinkedHashMap<>();
        final Set<UUID> kicked = new HashSet<>();
        boolean inviteOnly;
        int capacity, ruleMask,firstDealer,nextDealer;
        String message = "";
        Room(ServerPlayer owner, BlockPos pos) {
            level=owner.serverLevel(); this.pos=pos.immutable();
            players.put(owner.getUUID(),owner.getGameProfile().getName());
            capacity=Math.max(1,Math.min(game.maxPlayers(),places().size()));
        }
        UUID owner() { return players.keySet().iterator().next(); }
        List<LobbyPlaces.Place> places() { return LobbyPlaces.find(level,pos,players.keySet()); }
        boolean supported() { var state=level.getBlockState(pos); return state.is(ModContent.LOBBY.get()) && state.canSurvive(level,pos); }
    }
    private static final Map<UUID,Room> MEMBERS=new HashMap<>();
    private static final List<Room> ROOMS=new ArrayList<>();

    public static void open(ServerPlayer player, BlockPos pos) {
        if(PreparationGames.tryReturn(player,pos))return;
        TabletopStuff.LOGGER.debug("Lobby open requested: player={}, pos={}",player.getGameProfile().getName(),pos);
        if (PrototypeSessions.tableInUse(player,pos)) { notify(player,"tabletop_stuff.lobby.in_game"); return; }
        if (PrototypeSessions.locked(player)||player.isSpectator()||!player.isAlive()||player.distanceToSqr(Vec3.atCenterOf(pos))>36) return;
        if (!player.level().getBlockState(pos).canSurvive(player.level(),pos)) { notify(player,"tabletop_stuff.lobby.unsupported"); return; }
        Room room=ROOMS.stream().filter(r->r.level==player.serverLevel()&&r.pos.equals(pos)).findFirst().orElse(null);
        if (room!=null&&!room.players.containsKey(player.getUUID())) {
            if (!LobbyPolicy.allowsEntry(room.inviteOnly,room.kicked.contains(player.getUUID()),room.invited.containsKey(player.getUUID()))) { notify(player,"tabletop_stuff.lobby.invite_required"); return; }
            if (room.players.size()>=room.capacity) { notify(player,"tabletop_stuff.lobby.full"); return; }
        }
        Room previous=MEMBERS.get(player.getUUID());
        if(previous!=null&&previous!=room) leave(player);
        if(room==null){room=new Room(player,pos);ROOMS.add(room);}
        room.players.put(player.getUUID(),player.getGameProfile().getName());
        room.kicked.remove(player.getUUID());
        MEMBERS.put(player.getUUID(),room);
        TabletopStuff.LOGGER.debug("Lobby joined: player={}, members={}, available={}",player.getGameProfile().getName(),room.players.size(),room.places().size());
        sync(room);
    }
    public static void action(ServerPlayer player,int action,int value) { action(player,action,value,new UUID(0,0)); }
    public static void action(ServerPlayer player,int action,int value,UUID target) {
        if(action>=100){PreparationGames.action(player,action,value,target);return;}
        Room room=MEMBERS.get(player.getUUID());
        if(room==null) return;
        if(action==LEAVE){leave(player);return;}
        if(!room.supported()){leave(player);return;}
        TabletopStuff.LOGGER.debug("Lobby action: player={}, action={}, value={}, target={}",player.getGameProfile().getName(),action,value,target);
        List<LobbyPlaces.Place> places=room.places();
        if(action==SEAT){
            if(places.stream().anyMatch(p->p.index()==value)&&room.seats.entrySet().stream().noneMatch(e->!e.getKey().equals(player.getUUID())&&e.getValue()==value)) {
                room.seats.put(player.getUUID(),value);sync(room);
            }
            return;
        }
        if(!room.owner().equals(player.getUUID())) return;
        switch(action){
            case CAPACITY -> { if(room.bots.isEmpty()&&value>=room.players.size()&&value>=1&&value<=Math.min(room.game.maxPlayers(),places.size())){room.capacity=value;room.message="";} }
            case ADD_BOT -> {
                if(room.capacity!=1||room.bots.size()+1>=Math.min(room.game.maxPlayers(),places.size()))return;
                room.bots.put(UUID.randomUUID(),com.wentory.tabletopstuff.nullgame.NullBot.Difficulty.NORMAL);
            }
            case BOT_DIFFICULTY -> {
                var difficulty=room.bots.get(target);
                if(difficulty==null)return;
                var levels=com.wentory.tabletopstuff.nullgame.NullBot.Difficulty.values();
                room.bots.put(target,levels[(difficulty.ordinal()+1)%levels.length]);
            }
            case START -> { start(room,places);return; }
            case ACCESS -> room.inviteOnly=value!=0;
            case INVITE -> {
                if(room.capacity==1)return;
                ServerPlayer candidate=room.level.getServer().getPlayerList().getPlayer(target);
                if(candidate!=null&&discoverable(room,candidate)&&room.invited.size()<64&&room.invited.putIfAbsent(target,candidate.getGameProfile().getName())==null) {
                    candidate.sendSystemMessage(Component.translatable("tabletop_stuff.lobby.invitation",player.getGameProfile().getName(),Component.translatable(room.game.nameKey())));
                }
            }
            case CANCEL -> room.invited.remove(target);
            case KICK -> {
                if(room.bots.remove(target)!=null){sync(room);return;}
                if(!target.equals(room.owner())&&room.players.containsKey(target)){
                    room.invited.remove(target);room.kicked.add(target);room.seats.remove(target);room.players.remove(target);MEMBERS.remove(target);
                    ServerPlayer removed=room.level.getServer().getPlayerList().getPlayer(target);
                    if(removed!=null){PrototypeNetwork.sendLobby(removed,update(room,removed,false));notify(removed,"tabletop_stuff.lobby.kicked");}
                }
            }
            case RULE -> room.ruleMask=com.wentory.tabletopstuff.nullgame.NullHouseRule.toggle(room.ruleMask,value);
            case FIRST_DEALER -> {if(value>=0&&value<=1)room.firstDealer=value;}
            case NEXT_DEALER -> {if(value>=0&&value<=2)room.nextDealer=value;}
            case RESTORE_SETTINGS -> {
                long preferences=target.getLeastSignificantBits();
                int capacity=(int)(preferences&15),rules=(int)(preferences>>5&31);
                int first=(int)(preferences>>10&1),next=(int)(preferences>>11&3);
                if(capacity>room.game.maxPlayers()||next>2)return;
                if(capacity>0&&room.bots.isEmpty())room.capacity=Math.max(room.players.size(),Math.min(capacity,Math.max(1,places.size())));
                room.inviteOnly=(preferences&16)!=0;
                room.ruleMask=0;
                for(int i=0;i<room.game.rules().size();i++){
                    if((rules&(1<<i))!=0)room.ruleMask=com.wentory.tabletopstuff.nullgame.NullHouseRule.toggle(room.ruleMask,i);
                }
                room.firstDealer=first;
                room.nextDealer=next;
            }
            default -> { return; }
        }
        sync(room);
    }
    private static void start(Room room,List<LobbyPlaces.Place> places){
        if(room.capacity==1&&room.bots.isEmpty())return;
        if(places.size()<room.capacity+room.bots.size()){room.message="tabletop_stuff.lobby.no_space";sync(room);return;}
        if(room.players.size()!=room.capacity){room.message="tabletop_stuff.lobby.waiting";sync(room);return;}
        List<ServerPlayer> people=new ArrayList<>();
        for(UUID id:room.players.keySet()){
            ServerPlayer person=room.level.getServer().getPlayerList().getPlayer(id);
            if(person==null||person.serverLevel()!=room.level||!person.isAlive()||person.isPassenger()||person.distanceToSqr(Vec3.atCenterOf(room.pos))>64){room.message="tabletop_stuff.lobby.unavailable";sync(room);return;}
            people.add(person);
        }
        List<Integer> seatOrder=new ArrayList<>(places.stream().map(LobbyPlaces.Place::index).toList());
        if(room.capacity==1)Collections.shuffle(seatOrder,new Random(room.level.getRandom().nextLong()));
        var result=LobbyPolicy.assign(new ArrayList<>(room.players.keySet()),room.seats,seatOrder);
        if(result.isEmpty()){room.message="tabletop_stuff.lobby.no_space";sync(room);return;}
        Map<UUID,Integer> assigned=result.get();Set<Integer> used=new HashSet<>(assigned.values());
        ROOMS.remove(room);
        Map<UUID,LobbyPlaces.Place> chosen=new LinkedHashMap<>();
        for(ServerPlayer person:people){
            MEMBERS.remove(person.getUUID());PrototypeNetwork.sendLobby(person,update(room,person,false));
            int index=assigned.get(person.getUUID());
            chosen.put(person.getUUID(),places.stream().filter(p->p.index()==index).findFirst().orElseThrow());
        }
        List<LobbyPlaces.Place> extras=new ArrayList<>(places.stream().filter(p->!used.contains(p.index())).toList());
        if(!room.bots.isEmpty()){
            Collections.shuffle(extras,new Random(room.level.getRandom().nextLong()));
            extras=List.copyOf(extras.subList(0,room.bots.size()));
        }
        PreparationGames.start(people,room.pos,chosen,extras,new LinkedHashMap<>(room.bots),room.firstDealer,room.nextDealer,room.ruleMask);
    }
    public static void leave(ServerPlayer player){
        Room room=MEMBERS.remove(player.getUUID());if(room==null)return;
        LobbyUpdate closed=update(room,player,false);
        room.players.remove(player.getUUID());room.seats.remove(player.getUUID());
        PrototypeNetwork.sendLobby(player,closed);
        if(room.players.isEmpty())ROOMS.remove(room);else sync(room);
    }
    public static void tick(ServerPlayer player){
        Room room=MEMBERS.get(player.getUUID());if(room==null)return;
        if(!player.isAlive()||player.serverLevel()!=room.level||!room.supported()||player.distanceToSqr(Vec3.atCenterOf(room.pos))>64){leave(player);return;}
        if(room.owner().equals(player.getUUID())&&room.level.getGameTime()%20==0)sync(room);
    }
    private static LobbyUpdate update(Room room,ServerPlayer viewer,boolean open){
        List<LobbyPlaces.Place> places=room.places();int mask=0;for(var p:places)mask|=1<<p.index();
        List<LobbyUpdate.Member> members=new ArrayList<>();
        for(var e:room.players.entrySet())members.add(new LobbyUpdate.Member(e.getKey(),e.getValue(),room.seats.getOrDefault(e.getKey(),-1)));
        int number=1;
        for(var bot:room.bots.entrySet())
            members.add(new LobbyUpdate.Member(bot.getKey(),Integer.toString(number++),-1,bot.getValue()));
        List<LobbyUpdate.Candidate> nearby=new ArrayList<>();
        if(open&&room.capacity>1&&room.owner().equals(viewer.getUUID())){
            // Pending invitations are the creator's own history, not a proximity scan:
            // they can be revoked without revealing a recipient's current location/presence.
            for(var invitation:room.invited.entrySet()) if(!room.players.containsKey(invitation.getKey()))
                nearby.add(new LobbyUpdate.Candidate(invitation.getKey(),invitation.getValue(),true));
            room.level.players().stream().filter(p->discoverable(room,p)&&!room.invited.containsKey(p.getUUID()))
                .sorted(Comparator.comparing(p->p.getGameProfile().getName(),String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(0,64-nearby.size())).forEach(p->nearby.add(new LobbyUpdate.Candidate(p.getUUID(),p.getGameProfile().getName(),false)));
        }
        return new LobbyUpdate(open,room.pos,room.owner(),room.capacity,mask,List.copyOf(members),List.copyOf(nearby),room.inviteOnly,
            room.ruleMask,room.game.icon(),room.game.rules(),places.size()<room.capacity?"tabletop_stuff.lobby.no_space":room.message,room.firstDealer,room.nextDealer);
    }
    private static void sync(Room room){
        List<LobbyPlaces.Place> places=room.places();Set<Integer> valid=new HashSet<>();places.forEach(p->valid.add(p.index()));
        room.seats.entrySet().removeIf(e->!valid.contains(e.getValue()));
        room.capacity=Math.max(room.players.size(),Math.min(room.capacity,Math.max(1,places.size())));
        if(room.message.equals("tabletop_stuff.lobby.no_space")&&places.size()>=room.capacity||room.message.equals("tabletop_stuff.lobby.waiting")&&room.players.size()==room.capacity)room.message="";
        for(UUID id:room.players.keySet()){
            ServerPlayer player=room.level.getServer().getPlayerList().getPlayer(id);
            if(player!=null)PrototypeNetwork.sendLobby(player,update(room,player,true));
        }
    }
    private static void notify(ServerPlayer player,String key){player.displayClientMessage(Component.translatable(key),true);}
    private static boolean discoverable(Room room,ServerPlayer player){
        return LobbyPolicy.discoverable(player.serverLevel()==room.level,player.isAlive(),player.isSpectator(),player.isInvisible(),
            room.players.containsKey(player.getUUID()),player.distanceToSqr(Vec3.atCenterOf(room.pos)));
    }
    public static void clear(){MEMBERS.clear();ROOMS.clear();}
}
